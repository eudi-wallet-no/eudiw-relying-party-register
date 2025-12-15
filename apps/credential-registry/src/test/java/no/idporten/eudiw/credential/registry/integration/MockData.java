package no.idporten.eudiw.credential.registry.integration;

import no.idporten.eudiw.credential.registry.integration.model.CredentialConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockData {

    private static final Logger log = LoggerFactory.getLogger(MockData.class);
    private static List<CredentialIssuer> credentialIssuers;

    public MockData() {
        setCredentialsIssuers();
    }

    public static void setCredentialsIssuers() {
        credentialIssuers = new ArrayList<>();
        Map<String, CredentialConfiguration> credConfig = new HashMap<>();
        credConfig.put("utsteder", new CredentialConfiguration("sd+jwt-vc"));
        Map<String, CredentialConfiguration> credConfig2 = new HashMap<>();
        credConfig.put("utstederTest", new CredentialConfiguration("mdoc"));
        URL credential_endpoint1;
        URL credential_endpoint2;
        try {
            credential_endpoint1 = new URL("http://eksempel.com");
            credential_endpoint2 = new URL("http://eksempel2.com");
            CredentialIssuer issuer1 = new CredentialIssuer("mock_utsteder", credential_endpoint1, credConfig);
            CredentialIssuer issuer2 = new CredentialIssuer("mock_utsteder2", credential_endpoint2, credConfig2);
            credentialIssuers.add(issuer1);
            credentialIssuers.add(issuer2);
        } catch (MalformedURLException e){
            log.info("Feil i skaping av URL i test");
        }
    }

    public static List<CredentialIssuer> getCredentialIssuers() {
        return credentialIssuers;
    }
}
