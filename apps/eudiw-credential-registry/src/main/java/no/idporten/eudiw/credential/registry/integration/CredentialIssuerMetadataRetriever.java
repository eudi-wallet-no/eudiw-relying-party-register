package no.idporten.eudiw.credential.registry.integration;

import jakarta.annotation.PostConstruct;
import jakarta.validation.*;
import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import no.idporten.eudiw.credential.registry.configuration.CredentialRegisterConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


import java.net.URI;
import java.util.*;

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

    private List<CredentialIssuer> listOfIssuer;

    @Autowired
    public CredentialIssuerMetadataRetriever(final ConfigProperties configProperties, final CredentialRegisterConfiguration configuration, Validator validator, RestClient restClient) {
        this.configProperties = configProperties;
        this.configuration = configuration;
        this.restClient = restClient;
        this.validator = validator;
    }

    @PostConstruct
    public void initializeListOfIssuer() {
        updateListOfIssuer();
    }

    private CredentialIssuer fetchCredentialIssuerFromMetadataRequest(URI uri) {
        CredentialIssuer credentialIssuer = restClient.get()
                .uri(uri)
                .retrieve()
                .body(CredentialIssuer.class);
        Set<ConstraintViolation<CredentialIssuer>> violations = validator.validate(credentialIssuer);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return credentialIssuer;
    }

    private void updateListOfIssuer() {
        this.listOfIssuer = configProperties.credentialIssuerServers().stream().map(this::fetchCredentialIssuerFromMetadataRequest).toList();
    }

    public List<CredentialIssuer> getListOfIssuer() {
        return listOfIssuer;
    }

}
