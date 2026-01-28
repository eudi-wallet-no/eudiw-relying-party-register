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

    protected boolean isHttps(URI uri){
        if (uri.getScheme()==null || !uri.getScheme().equals("https")) {
            log.warn("Issuer {} does not use https in its registered uri", uri);
            return false;
        }
        return true;
    }

    protected boolean emptyHost(URI uri){
        if (!StringUtils.hasText(uri.getHost())) {
            log.warn("Issuer {} does not contain characters in host", uri);
            return true;
        }
        return false;
    }

    protected URI buildWellKnown(URI uri) {
        if (uri.getPath().equals("/")){
            return uri.resolve(CREDENTIAL_ISSUER_CONFIG_ENDPOINT);
        }
        URI wellknown = uri.resolve(CREDENTIAL_ISSUER_CONFIG_ENDPOINT + uri.getPath());
        return wellknown;
    }

    protected CredentialIssuer validateCredentialIssuer(CredentialIssuer credentialIssuer, URI uri) {
        Set<ConstraintViolation<CredentialIssuer>> violations = validator.validate(credentialIssuer);
        if (!violations.isEmpty()) {
            String prettyViolations = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            String errorDescription = String.format("Issuer with uri %s has these violations %s and is therefore not included", uri, prettyViolations);
            log.warn("Constraint violations error " + errorDescription);
            return null;
        }
        return credentialIssuer;
    }

    protected CredentialIssuer fetchCredentialIssuerFromMetadataRequest(URI uri) {
        CredentialIssuer credentialIssuer;
        URI wellknown = buildWellKnown(uri);
        try {
            credentialIssuer = restClient.get()
                    .uri(wellknown)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CredentialIssuer.class);
        } catch (Exception e) {
            log.warn("error fetching content from well known- url of issuer" + uri, e);
            return null;
        }
        return validateCredentialIssuer(credentialIssuer, uri);
    }

    protected CredentialIssuerUrls retrieveCredentialIssuerUrlsFromRPService() {
        CredentialIssuerUrls uris;
        try {
            uris = restClient.get()
                    .retrieve()
                    .body(CredentialIssuerUrls.class);
        }catch (HttpClientErrorException e) {
            throw new CredentialRegisterException(e.toString(), e.getMessage(), HttpStatus.BAD_REQUEST);
        }catch (HttpServerErrorException e) {
            throw new CredentialRegisterException(e.toString(), e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
        return uris;
    }


    public void updateListOfIssuer(){
        CredentialIssuerUrls uris = retrieveCredentialIssuerUrlsFromRPService();
        if (Objects.isNull(uris)) {
            listOfIssuer = new ArrayList<>();
            return;
        }
        List<URI> listOfURI = new ArrayList<>();
        for (URI issuer : uris.credentialIssuerUrls()) {
            if(issuer != null && isHttps(issuer) && !emptyHost(issuer)) {
                listOfURI.add(URI.create(issuer.toString()));
            }
        }
        this.listOfIssuer = listOfURI.stream().map(this::fetchCredentialIssuerFromMetadataRequest).filter(Objects::nonNull).toList();
        if (listOfIssuer.isEmpty()) {
            log.info("No issuers found in list of issuers");
        }
    }


    public List<CredentialIssuer> getListOfIssuer() {
        return listOfIssuer;
    }
}

