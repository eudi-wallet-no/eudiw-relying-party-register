package no.idporten.eudiw.credential.registry.integration;

import jakarta.validation.*;
import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import no.idporten.eudiw.credential.registry.configuration.CredentialRegisterConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuerUrls;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

    private List<CredentialIssuer> listOfIssuer;

    @Autowired
    public CredentialIssuerMetadataRetriever(final ConfigProperties configProperties, final CredentialRegisterConfiguration configuration, Validator validator, RestClient restClient) {
        this.configuration = configuration;
        this.restClient = restClient;
        this.validator = validator;
        this.configProperties = configProperties;
    }


    private CredentialIssuer fetchCredentialIssuerFromMetadataRequest(URI uri) {
        CredentialIssuer credentialIssuer;
        try {
            credentialIssuer = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(CredentialIssuer.class);
        } catch (Exception e) {
            log.error("Error fetching credential issuer from metadata request from issuer {} ", uri, e);
            return null;
        }
        Set<ConstraintViolation<CredentialIssuer>> violations = validator.validate(credentialIssuer);
        if (!violations.isEmpty()) {
            log.error( "Issuer with uri {} has these violations {} and is therefore not included", uri, violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", ")));
            return null;
        }
        return credentialIssuer;
    }

    public void updateListOfIssuer() throws BadRequestException {
        CredentialIssuerUrls uris;
        List<URI> listUOfURI;
        listUOfURI = new ArrayList<>();
        try {
            uris = restClient.get()
                    .retrieve()
                    .body(CredentialIssuerUrls.class);
        }catch (HttpClientErrorException e) {
            log.error("Can not get contact with relying party register, or format of contact is unexpected. Error : "
                    + e.getMessage());
            listOfIssuer = null;
            throw new BadRequestException(e.getMessage());
        }
            for (URI issuer : uris.credentialIssuerUrls()) {
                listUOfURI.add(URI.create(issuer.toString()));
            }
            this.listOfIssuer = listUOfURI.stream().map(this::fetchCredentialIssuerFromMetadataRequest).filter(Objects::nonNull).toList();
            if (listOfIssuer.isEmpty()) {
                log.info("No issuer found for Well-Known CredentialIssuer");
            }
        }

    public List<CredentialIssuer> getListOfIssuer() {
        return listOfIssuer;
    }
}

