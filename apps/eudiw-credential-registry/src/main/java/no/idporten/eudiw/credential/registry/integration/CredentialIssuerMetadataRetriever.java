package no.idporten.eudiw.credential.registry.integration;

import jakarta.validation.*;
import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import no.idporten.eudiw.credential.registry.configuration.CredentialRegisterConfiguration;
import no.idporten.eudiw.credential.registry.exception.CredentialRegisterException;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuerUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;


import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Responsible for sending get-request to well knwon openid credential issuer endpoints of all issuers registered
 * in application.yaml, and formating the data into an object.
 *
 */

@Service
public class CredentialIssuerMetadataRetriever {
    private static final Logger log = LoggerFactory.getLogger(CredentialIssuerMetadataRetriever.class);
    private final ConfigProperties configProperties;
    private final CredentialRegisterConfiguration configuration;
    private final Validator validator;
    private final RestClient restClient;
    private static final String CREDENTIAL_ISSUER_CONFIG_ENDPOINT = "/.well-known/openid-credential-issuer";

    private List<CredentialIssuer> listOfIssuer;

    @Autowired
    public CredentialIssuerMetadataRetriever(final ConfigProperties configProperties, final CredentialRegisterConfiguration configuration, Validator validator, RestClient restClient) {
        this.configuration = configuration;
        this.restClient = restClient;
        this.validator = validator;
        this.configProperties = configProperties;
    }

    public URI buildWellKnown(URI uri) {
        if (uri == null) {
            log.error("Issuer URL is null  "+ uri);
            return null;
        }
        if (uri.getPath().equals("/")){
            return uri.resolve(CREDENTIAL_ISSUER_CONFIG_ENDPOINT);
        }
        URI wellknown = uri.resolve(CREDENTIAL_ISSUER_CONFIG_ENDPOINT + uri.getPath());
        return wellknown;
    }

    private CredentialIssuer fetchCredentialIssuerFromMetadataRequest(URI uri) {
        CredentialIssuer credentialIssuer;
        URI wellknown = buildWellKnown(uri);
        if (wellknown == null) {
            log.error("Issuer {} is null in fetchCredentialIssuerFromMetadataRequest", uri);
            return null;
        }
        if (!wellknown.getScheme().equals("https")) {
            log.error("Issuer {} does not use https in its registered uri", uri);
            return null;
        }
        if (!StringUtils.hasText(wellknown.getHost())) {
            log.error("Issuer {} does not contain characters in host", uri);
            return null;
        }
        log.info("Prepared to fetch data from complete url {}", wellknown);
        try {
            credentialIssuer = restClient.get()
                    .uri(wellknown)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CredentialIssuer.class);
        } catch (Exception e) {
            log.error("error fetching content from well known- url of issuer" + uri, e);
            return null;
        }
        Set<ConstraintViolation<CredentialIssuer>> violations = validator.validate(credentialIssuer);
        if (!violations.isEmpty()) {
            String prettyViolations = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            String errorDescription = String.format("Issuer with uri %s has these violations %s and is therefore not included", uri, prettyViolations);
            log.error("Constraint violations error " + errorDescription);
            return null;
        }
        log.info("Successfully fetched credential issuer from complete url {} ", uri);
        return credentialIssuer;
    }


    public void updateListOfIssuer(){
        CredentialIssuerUrls uris = null;
        List<URI> listUOfURI;
        listUOfURI = new ArrayList<>();
        try {
            uris = restClient.get()
                    .retrieve()
                    .body(CredentialIssuerUrls.class);
        }catch (HttpClientErrorException e) {
            listOfIssuer = null;
            throw new CredentialRegisterException(e.toString(), e.getMessage(), HttpStatus.BAD_REQUEST);
        }catch (HttpServerErrorException e) {
            listOfIssuer = null;
            throw new CredentialRegisterException(e.toString(), e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (uris == null) {
            listOfIssuer = null;
            throw new CredentialRegisterException("Error when trying to fetch list of uris from relying party service",
                     "either wrong relying party url, unsupported content on relying party service or other error",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        for (URI issuer : uris.credentialIssuerUrls()) {
            listUOfURI.add(URI.create(issuer.toString()));
        }
        this.listOfIssuer = listUOfURI.stream().map(this::fetchCredentialIssuerFromMetadataRequest).filter(Objects::nonNull).toList();
        listOfIssuer.stream().forEach(issuer -> {log.info("issuer {} registered in list of issuers", issuer.getCredentialIssuer());});
        if (listOfIssuer.isEmpty()) {
            log.info("No issuers found in list of issuers");
        }
    }


    public List<CredentialIssuer> getListOfIssuer() {
        return listOfIssuer;
    }
}

