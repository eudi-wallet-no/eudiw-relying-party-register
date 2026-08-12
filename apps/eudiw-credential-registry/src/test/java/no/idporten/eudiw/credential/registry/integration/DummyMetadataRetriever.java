package no.idporten.eudiw.credential.registry.integration;

import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("junit")
@Primary
public class DummyMetadataRetriever extends CredentialIssuerMetadataRetriever {


    public DummyMetadataRetriever() {
        super(null, null, null,null, null); // ignored
    }

    @Override
    public List<CredentialIssuer> getListOfIssuer() {
        return MockData.mockCredentialIssuersListOne();
    }

    @Override
    public void updateListOfIssuer() {
    }
}
