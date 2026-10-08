package no.idporten.eudiw.credential.registry.integration;

import io.micrometer.core.instrument.Counter;
import jakarta.validation.Validator;
import no.idporten.eudiw.credential.registry.exception.CredentialRegisterException;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import no.idporten.eudiw.credential.registry.integration.model.CredentialConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuerUrls;
import no.idporten.eudiw.credential.registry.response.model.CredentialValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.idporten.eudiw.credential.registry.exception.CredentialRegisterException.ERROR;

/**
 * Responsible for sending get-request to well-known openid credential issuer endpoints of all issuers registered
 * in application.yaml, and formating the data into an object.
 *
 */

@Service
public class CredentialIssuerMetadataRetriever {
    private static final Logger log = LoggerFactory.getLogger(CredentialIssuerMetadataRetriever.class);

    private final Validator validator;
    @Qualifier("restClientRpService")
    private final RestClient restClientRpService;
    @Qualifier("restClientExternalApi")
    private final RestClient restClientExternalApi;
    @Qualifier("connectExternalApiExceptionCounter")
    private final Counter connectExternalApiExceptionCounter;
    @Qualifier("connectInternalApiExceptionCounter")
    private final Counter connectInternalApiExceptionCounter;

    private static final String CREDENTIAL_ISSUER_CONFIG_ENDPOINT = "/.well-known/openid-credential-issuer";
    private static final JsonMapper METADATA_MAPPER = JsonMapper.builder().build();

    private List<CredentialIssuer> listOfIssuer;


    @Autowired
    public CredentialIssuerMetadataRetriever(Validator validator, RestClient restClientRpService, RestClient restClientExternalApi, Counter connectExternalApiExceptionCounter, Counter connectInternalApiExceptionCounter) {
        this.validator = validator;
        this.restClientRpService = restClientRpService;
        this.restClientExternalApi = restClientExternalApi;
        this.connectExternalApiExceptionCounter = connectExternalApiExceptionCounter;
        this.connectInternalApiExceptionCounter = connectInternalApiExceptionCounter;
    }

    protected boolean isHttps(URI uri) {
        if (uri.getScheme() == null || !uri.getScheme().equals("https")) {
            log.warn("Issuer {} does not use https in its registered uri", uri);
            return false;
        }
        return true;
    }

    protected boolean emptyHost(URI uri) {
        if (!StringUtils.hasText(uri.getHost())) {
            log.warn("Issuer {} does not contain characters in host", uri);
            return true;
        }
        return false;
    }

    protected URI buildWellKnown(URI uri) {
        if (uri.getPath().equals("/")) {
            return uri.resolve(CREDENTIAL_ISSUER_CONFIG_ENDPOINT);
        }
        return uri.resolve(CREDENTIAL_ISSUER_CONFIG_ENDPOINT + uri.getPath());
    }

    protected CredentialIssuer validateCredentialIssuer(CredentialIssuer issuer, URI uri) {
        var errors = validationErrors(issuer);
        if (!errors.isEmpty()) {
            log.warn("Ignoring issuer {}: {}", uri, errors);
            return null;
        }
        return issuer;
    }

    // Read errors for a saved issuer using the same validation as the crawler.
    public List<CredentialValidationError> getCredentialErrors(URI uri) {
        var issuers = retrieveCredentialIssuerUrlsFromRPService();
        if (issuers == null || !issuers.credentialIssuerUrls().contains(uri)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!isHttps(uri) || emptyHost(uri)) {
            return List.of(new CredentialValidationError("Utstedarmetadata", List.of("Utstedar-URL må vere ein HTTPS-URL.")));
        }
        CredentialIssuer issuer;
        try {
            issuer = readCredentialIssuerMetadata(uri);
        } catch (Exception e) {
            log.warn("Failed fetching issuer metadata for {}", uri, e);
            return List.of(new CredentialValidationError("Utstedarmetadata", List.of("Kunne ikkje hente eller lese utstedarmetadata.")));
        }
        var errors = validationErrors(issuer);
        if (!errors.isEmpty()) return List.of(new CredentialValidationError("Utstedarmetadata", errors));
        return List.copyOf(issuer.getCredentialConfigurationErrors().values());
    }

    private CredentialIssuer readCredentialIssuerMetadata(URI uri) {
        JsonNode metadata = restClientExternalApi.get().uri(buildWellKnown(uri)).retrieve().body(JsonNode.class);
        if (!(metadata instanceof ObjectNode root)
                || !root.path("credential_configurations_supported").isObject()) {
            return METADATA_MAPPER.treeToValue(metadata, CredentialIssuer.class);
        }
        JsonNode configurations = root.remove("credential_configurations_supported");
        CredentialIssuer issuer = METADATA_MAPPER.treeToValue(root, CredentialIssuer.class);
        Map<String, CredentialConfiguration> parsed = new LinkedHashMap<>();
        Map<String, CredentialValidationError> errors = new LinkedHashMap<>();
        configurations.properties().forEach(entry -> {
            List<String> messages;
            try {
                var config = METADATA_MAPPER.treeToValue(entry.getValue(), CredentialConfiguration.class);
                messages = config == null ? List.of("Tom beviskonfigurasjon.") : validationErrors(config);
                if (messages.isEmpty()) parsed.put(entry.getKey(), config);
            } catch (JacksonException e) {
                String field = e.getPath().isEmpty() ? "metadata" : e.getPath().stream()
                        .map(ref -> ref.getPropertyName() == null ? "[" + ref.getIndex() + "]" : "." + ref.getPropertyName())
                        .collect(Collectors.joining()).replaceFirst("^\\.", "");
                messages = List.of(field + ": " + e.getOriginalMessage());
            }
            if (!messages.isEmpty()) {
                String name = entry.getValue().path("credential_metadata").path("display").path(0).path("name").asText(entry.getKey());
                errors.put(entry.getKey(), new CredentialValidationError(StringUtils.hasText(name) ? name : entry.getKey(), messages));
                log.warn("Ignoring credential configuration {} from issuer {}: {}", entry.getKey(), uri, messages);
            }
        });
        issuer.setCredentialConfiguration(parsed);
        issuer.setCredentialConfigurationErrors(errors);
        return issuer;
    }

    private List<String> validationErrors(Object value) {
        if (value == null) return List.of("Tom metadatarespons.");
        return validator.validate(value).stream()
                .map(v -> v.getPropertyPath().toString().replace("credentialMetadata", "credential_metadata")
                        .replace("credentialConfiguration", "credential_configurations_supported")
                        .replace("credentialIssuer", "credential_issuer").replace("validPath", "path")
                        + ": " + v.getMessage()).sorted().toList();
    }

    protected CredentialIssuer fetchCredentialIssuerFromMetadataRequest(URI uri) {
        CredentialIssuer credentialIssuer;
        URI wellknown = buildWellKnown(uri);
        try {
            credentialIssuer = readCredentialIssuerMetadata(uri);
        } catch (ResourceAccessException e) {
            log.warn("Connection error to issuers well-known url: {}", wellknown, e);
            connectExternalApiExceptionCounter.increment();
            return null;
        } catch (Exception e) {
            log.warn("Failed fetching content from issuers well-known url: {}", wellknown, e);
            return null;
        }
        return validateCredentialIssuer(credentialIssuer, uri);
    }

    protected CredentialIssuerUrls retrieveCredentialIssuerUrlsFromRPService() {
        CredentialIssuerUrls uris;
        try {
            uris = restClientRpService.get()
                    .retrieve()
                    .body(CredentialIssuerUrls.class);
        } catch (ResourceAccessException e) {
            connectInternalApiExceptionCounter.increment();
            throw new CredentialRegisterException(ERROR, "Failed to connect to rp-service", HttpStatus.SERVICE_UNAVAILABLE, e);
        } catch (Exception e) {
            throw new CredentialRegisterException(ERROR, "Failed to fetch issuer-server uris from rp-service", HttpStatus.SERVICE_UNAVAILABLE, e);
        }
        return uris;
    }


    public void updateListOfIssuer() {
        CredentialIssuerUrls uris = retrieveCredentialIssuerUrlsFromRPService();
        if (Objects.isNull(uris)) {
            listOfIssuer = new ArrayList<>();
            return;
        }
        List<URI> listOfURI = new ArrayList<>();
        for (URI issuer : uris.credentialIssuerUrls()) {
            if (issuer != null && isHttps(issuer) && !emptyHost(issuer)) {
                listOfURI.add(URI.create(issuer.toString()));
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("Found urls in list of issuers: {}", listOfURI);
        }
        this.listOfIssuer = listOfURI.stream().map(this::fetchCredentialIssuerFromMetadataRequest).filter(Objects::nonNull).toList();
        if (listOfIssuer.isEmpty()) {
            log.info("No issuers found in list of issuers: {}", listOfIssuer);
        }
    }


    public List<CredentialIssuer> getListOfIssuer() {
        return listOfIssuer;
    }

}
