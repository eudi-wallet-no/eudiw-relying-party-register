package no.idporten.eudiw.credential.registry.integration;

import jakarta.validation.Validator;
import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import no.idporten.eudiw.credential.registry.configuration.CredentialRegisterConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Profile("junit")
@Primary
public class DummyMetadataRetriever extends CredentialIssuerMetadataRetriever {


    public DummyMetadataRetriever(ConfigProperties configProperties,
                                  CredentialRegisterConfiguration configuration,
                                  Validator validator,
                                  RestClient restClient) {
        super(null, null, null, null); // ignored

    }

    @Override
    public List<CredentialIssuer> getListOfIssuer() {
        return MockData.mockCredentialIssuersListOne();
    }

    @Override
    public void updateListOfIssuer() {
    }
}
