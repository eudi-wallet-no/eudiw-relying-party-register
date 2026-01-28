package no.idporten.eudiw.credential.registry.integration;

import jakarta.validation.ValidationException;
import no.idporten.eudiw.credential.registry.integration.model.CredentialConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialDefinition;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import no.idporten.eudiw.credential.registry.response.model.*;
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

    public MockData() {

    }

    public static List<CredentialIssuer> mockCredentialIssuersListOne() {
        ArrayList<CredentialIssuer> credentialIssuers = new ArrayList<>();
        Map<String, CredentialConfiguration> credConfig = new HashMap<>();
        credConfig.put("utsteder", new CredentialConfiguration("sd+jwt-vc"));
        Map<String, CredentialConfiguration> credConfig2 = new HashMap<>();
        credConfig2.put("utstederTest", new CredentialConfiguration("mdoc"));
        Map<String, CredentialConfiguration> credConfig3 = new HashMap<>();
        CredentialConfiguration credentialConfiguration = new CredentialConfiguration("jwt_vc_json");
        CredentialDefinition definition = new CredentialDefinition();
        ArrayList<String> types = new ArrayList<>();
        types.add("VerifiableCredential");
        types.add("heiskort");
        definition.setType(types);
        credentialConfiguration.setCredentialDefinition(definition);
        credConfig3.put("testjwtvcjson", credentialConfiguration);

        URL credential_endpoint1;
        URL credential_endpoint2;
        URL credential_endpoint3;
        try {
            credential_endpoint1 = new URL("http://eksempel.com");
            credential_endpoint2 = new URL("http://eksempel2.com");
            credential_endpoint3 = new URL("http://eksempel3.com");

            CredentialIssuer issuer1 = new CredentialIssuer("mock_utsteder", credential_endpoint1, credConfig);
            CredentialIssuer issuer2 = new CredentialIssuer("mock_utsteder2", credential_endpoint2, credConfig2);
            CredentialIssuer issuer3 = new CredentialIssuer("mock_utsteder3", credential_endpoint3, credConfig3);
            credentialIssuers.add(issuer1);
            credentialIssuers.add(issuer2);
            credentialIssuers.add(issuer3);
        } catch (MalformedURLException e) {
            log.info("Feil i skaping av URL i test");
        } catch (ValidationException e) {
            log.error("Valideringsfeil ", e);
        }
        return credentialIssuers;
    }

    public static List<CredentialIssuer> mockCredentialIssuersListTwo() {
        ArrayList<CredentialIssuer> credentialIssuers = new ArrayList<>();
        Map<String, CredentialConfiguration> credConfig = new HashMap<>();
        credConfig.put("utsteder2", new CredentialConfiguration("sd+jwt-vc"));
        Map<String, CredentialConfiguration> credConfig2 = new HashMap<>();
        credConfig2.put("utstederTest2", new CredentialConfiguration("mdoc"));
        Map<String, CredentialConfiguration> credConfig3 = new HashMap<>();
        CredentialConfiguration credentialConfiguration = new CredentialConfiguration("jwt_vc_json");
        CredentialDefinition definition = new CredentialDefinition();
        ArrayList<String> types = new ArrayList<>();
        types.add("VerifiableCredential");
        types.add("heiskort2");
        definition.setType(types);
        credentialConfiguration.setCredentialDefinition(definition);
        credConfig3.put("testjwtvcjson2", credentialConfiguration);

        URL credential_endpoint1;
        URL credential_endpoint2;
        URL credential_endpoint3;
        try {
            credential_endpoint1 = new URL("http://eksempel2.com");
            credential_endpoint2 = new URL("http://eksempel22.com");
            credential_endpoint3 = new URL("http://eksempel23.com");

            CredentialIssuer issuer1 = new CredentialIssuer("mock_utsteder2", credential_endpoint1, credConfig);
            CredentialIssuer issuer2 = new CredentialIssuer("mock_utsteder22", credential_endpoint2, credConfig2);
            CredentialIssuer issuer3 = new CredentialIssuer("mock_utsteder23", credential_endpoint3, credConfig3);
            credentialIssuers.add(issuer1);
            credentialIssuers.add(issuer2);
            credentialIssuers.add(issuer3);
        } catch (MalformedURLException e) {
            log.info("Feil i skaping av URL i test");
        } catch (ValidationException e) {
            log.error("Valideringsfeil ", e);
        }
        return credentialIssuers;
    }

    public static Credentials mockCredentialsListOne() {

        CredentialsIssuer issuer1 = new CredentialsIssuer(
                "mock_utsteder",
                "utsteder",
                null,
                "sd+jwt-vc",
                new CredentialMetadata(
                        List.of(new Display("Utsteder A", "nb", "Testutsteder A")),
                        List.of(new Claims(List.of("path", "path2"), List.of(new Display("Utsteder A", "nb", "Testutsteder A"))))
                ),
                List.of(
                        new Display("Mock utsteder", "nb", "En mock av utsteder"),
                        new Display("Mock issuer", "en", "A mocked issuer")
                )
        );

        CredentialsIssuer issuer2 = new CredentialsIssuer(
                "mock_utsteder2",
                "utsteder2",
                null,
                "sd+jwt-vc",
                new CredentialMetadata(
                        List.of(new Display("Utsteder b", "nb", "Testutsteder b")),
                        List.of(new Claims(List.of("path", "path2"), List.of(new Display("Utsteder b", "nb", "Testutsteder b"))))
                ),
                List.of(
                        new Display("Mock utsteder", "nb", "En mock av utsteder"),
                        new Display("Mock issuer", "en", "A mocked issuer")
                )
        );

        CredentialsIssuer issuer3 = new CredentialsIssuer(
                "mock_utsteder",
                "utsteder",
                null,
                "sd+jwt-vc",
                new CredentialMetadata(
                        List.of(new Display("Utsteder c", "nb", "Testutsteder c")),
                        List.of(new Claims(List.of("path", "path2"), List.of(new Display("Utsteder c", "nb", "Testutsteder c"))))
                ),
                List.of(
                        new Display("Mock utsteder", "nb", "En mock av utsteder"),
                        new Display("Mock issuer", "en", "A mocked issuer")
                )
        );
        return new Credentials(List.of(issuer1, issuer2, issuer3));
    }

    public static Credentials mockCredentialsListTwo() {
        CredentialsIssuer issuer4 = new CredentialsIssuer(
                "mock_utsteder4",
                "utsteder4",
                null,
                "sd+jwt-vc",
                new CredentialMetadata(
                        List.of(new Display("Utsteder d", "nb", "Testutsteder d")),
                        List.of(new Claims(List.of("path", "path2"), List.of(new Display("Utsteder A", "nb", "Testutsteder d"))))
                ),
                List.of(
                        new Display("Mock utsteder", "nb", "En mock av utsteder"),
                        new Display("Mock issuer", "en", "A mocked issuer")
                )
        );

        CredentialsIssuer issuer5 = new CredentialsIssuer(
                "mock_utsteder5",
                "utsteder5",
                null,
                "sd+jwt-vc",
                new CredentialMetadata(
                        List.of(new Display("Utsteder b", "nb", "Testutsteder e")),
                        List.of(new Claims(List.of("path", "path2"), List.of(new Display("Utsteder e", "nb", "Testutsteder e"))))
                ),
                List.of(
                        new Display("Mock utsteder", "nb", "En mock av utsteder"),
                        new Display("Mock issuer", "en", "A mocked issuer")
                )
        );

        CredentialsIssuer issuer6 = new CredentialsIssuer(
                "mock_utsteder6",
                "utsteder6",
                null,
                "sd+jwt-vc",
                new CredentialMetadata(
                        List.of(new Display("Utsteder f", "nb", "Testutsteder f")),
                        List.of(new Claims(List.of("path", "path2"), List.of(new Display("Utsteder f", "nb", "Testutsteder f"))))
                ),
                List.of(
                        new Display("Mock utsteder", "nb", "En mock av utsteder"),
                        new Display("Mock issuer", "en", "A mocked issuer")
                )
        );
        return new Credentials(List.of(issuer4, issuer5, issuer6));
    }


}
