package no.idporten.eudiw.credential.registry.integration;

import io.micrometer.core.instrument.Counter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import no.idporten.eudiw.credential.registry.integration.model.CredentialConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Responsible for sending get-request to well-known openid credential issuer endpoints of all issuers registered
 * in application.yaml, and formating the data into an object.
 *
 */

@Service
public class CredentialIssuerMetadataRetriever {
    private static final Logger log = LoggerFactory.getLogger(CredentialIssuerMetadataRetriever.class);

    private final Validator validator;
    @Qualifier("restClientExternalApi")
    private final RestClient restClientExternalApi;
    @Qualifier("connectExternalApiExceptionCounter")
    private final Counter connectExternalApiExceptionCounter;

    private final ObjectMapper metadataMapper;

    private final RelyingPartyServiceIntegration relyingPartyServiceIntegration;

    private static final String CREDENTIAL_ISSUER_CONFIG_ENDPOINT = "/.well-known/openid-credential-issuer";

    private List<CredentialIssuer> listOfIssuer;


    @Autowired
    public CredentialIssuerMetadataRetriever(Validator validator, RestClient restClientExternalApi, Counter connectExternalApiExceptionCounter, RelyingPartyServiceIntegration relyingPartyServiceIntegration,
                                             ObjectMapper metadataMapper) {
        this.validator = validator;
        this.restClientExternalApi = restClientExternalApi;
        this.connectExternalApiExceptionCounter = connectExternalApiExceptionCounter;
        this.relyingPartyServiceIntegration = relyingPartyServiceIntegration;
        this.metadataMapper = metadataMapper;
    }


    public void updateListOfIssuer() {
        List<URI> listOfURI = relyingPartyServiceIntegration.retrieveCredentialIssuerUrlsFromRPService();
        if (listOfURI.isEmpty()) {
            listOfIssuer = new ArrayList<>();
            return;
        }
        listOfIssuer = listOfURI.stream().map(this::fetchCredentialIssuerFromMetadataRequest).filter(Objects::nonNull).toList();
        if (listOfIssuer.isEmpty()) {
            log.info("No issuers found in list of issuers: {}", listOfIssuer);
        }
    }


    public List<CredentialIssuer> getListOfIssuer() {
        return listOfIssuer;
    }


    protected CredentialIssuer fetchCredentialIssuerFromMetadataRequest(URI uri) {
        URI wellknown = buildWellKnown(uri);
        JsonNode metadata;
        try {
            metadata = restClientExternalApi.get()
                    .uri(wellknown)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResourceAccessException e) {
            log.warn("Connection error to issuers well-known url: {}", wellknown, e);
            connectExternalApiExceptionCounter.increment();
            return null;
        } catch (Exception e) {
            log.warn("Failed fetching content from issuers well-known url: {}", wellknown, e);
            return null;
        }
        if (metadata == null) {
            log.warn("No metadata returned from issuers well-known url: {}", wellknown);
            return null;
        }
        CredentialIssuer credentialIssuer = readCredentialIssuerMetadata(metadata, uri);
        return validateCredentialIssuer(credentialIssuer, uri);
    }

    private CredentialIssuer readCredentialIssuerMetadata(JsonNode metadata, URI uri) {
        if (!(metadata instanceof ObjectNode metadataObject) || !metadataObject.path("credential_configurations_supported").isObject()) {
            return metadataMapper.treeToValue(metadata, CredentialIssuer.class);
        }
        JsonNode configurations = metadataObject.remove("credential_configurations_supported");
        CredentialIssuer credentialIssuer = metadataMapper.treeToValue(metadataObject, CredentialIssuer.class);

        Map<String, CredentialConfiguration> validConfigurations = new LinkedHashMap<>();
        configurations.properties().forEach(entry -> {
            try {
                CredentialConfiguration configuration = metadataMapper.treeToValue(entry.getValue(), CredentialConfiguration.class);

                if (configuration != null && validator.validate(configuration).isEmpty()) {
                    validConfigurations.put(entry.getKey(), configuration);
                } else {
                    log.warn("Ignoring invalid credential configuration {} from issuer {}", entry.getKey(), uri);
                }
            } catch (JacksonException e) {
                log.warn("Ignoring credential configuration {} from issuer {}: {}",
                        entry.getKey(), uri, e.getMessage());
            }
        });

        credentialIssuer.setCredentialConfiguration(validConfigurations);
        return credentialIssuer;
    }

    protected URI buildWellKnown(URI uri) {
        if (uri.getPath().equals("/")) {
            return uri.resolve(CREDENTIAL_ISSUER_CONFIG_ENDPOINT);
        }
        return uri.resolve(CREDENTIAL_ISSUER_CONFIG_ENDPOINT + uri.getPath());
    }

    protected CredentialIssuer validateCredentialIssuer(CredentialIssuer credentialIssuer, URI uri) {
        Set<ConstraintViolation<CredentialIssuer>> violations = validator.validate(credentialIssuer);
        if (!violations.isEmpty()) {
            String prettyViolations = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            String errorDescription = String.format("Issuer with uri %s has these violations %s and is therefore not included", uri, prettyViolations);
            log.warn("Constraint violations error {}", errorDescription);
            return null;
        }
        return credentialIssuer;
    }


}

