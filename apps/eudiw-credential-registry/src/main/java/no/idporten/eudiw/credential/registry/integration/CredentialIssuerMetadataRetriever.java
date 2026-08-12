package no.idporten.eudiw.credential.registry.integration;

import io.micrometer.core.instrument.Counter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import no.idporten.eudiw.credential.registry.exception.CredentialRegisterException;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuerUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    protected CredentialIssuer fetchCredentialIssuerFromMetadataRequest(URI uri) {
        CredentialIssuer credentialIssuer;
        URI wellknown = buildWellKnown(uri);
        try {
            credentialIssuer = restClientExternalApi.get()
                    .uri(wellknown)
                    .retrieve()
                    .body(CredentialIssuer.class);
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

