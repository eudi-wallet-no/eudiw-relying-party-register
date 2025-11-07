package no.idporten.eudiw.credential.registry.integration;

import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import no.idporten.eudiw.credential.registry.configuration.CredentialRegisterConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for sending get-request to well knwon openid credential issuer endpoints of all issuers registered
 * in application.yaml, and formating the data into an object.
 *
 */

@Service
public class CredentialIssuerMetadataRetriever {
    private final ConfigProperties configProperties;
    private final CredentialRegisterConfiguration configuration;
    private Map<String, CredentialIssuer> mapOfIssuers;
    @Autowired
    public CredentialIssuerMetadataRetriever(final ConfigProperties configProperties, final CredentialRegisterConfiguration configuration) {
        this.configProperties = configProperties;
        this.configuration = configuration;
        this.mapOfIssuers = new HashMap<>();
    }

    public CredentialIssuer fetchCredentialIssuerFromMetadataRequest(URI uri) {
        CredentialIssuer credentialIssuer = configuration.restClient().get()
                .uri(uri)
                .retrieve()
                .body(CredentialIssuer.class);
        return credentialIssuer;
    }

    protected Map<String, CredentialIssuer> getMapOfIssuers() {
        return mapOfIssuers;
    }


    public void loopThroughAllIssuersAndStartFlow()  {
        for (String uri : configProperties.credentialIssuerServers()) {
            CredentialIssuer content = fetchCredentialIssuerFromMetadataRequest(URI.create(uri));
            mapOfIssuers.put(content.credentialIssuer(), content);
        }
    }
}
