package no.idporten.eudiw.credential.registry.response;

import no.idporten.eudiw.credential.registry.integration.DummyMetadataRetriever;
import no.idporten.eudiw.credential.registry.integration.MockData;
import no.idporten.eudiw.credential.registry.response.model.Credentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When scheduler is set to work")
public class CredentialRegisterServiceSchedulerTest {

    @MockitoBean
    DummyMetadataRetriever dummyMetadataRetriever;

    private CredentialRegisterService credentialRegisterService;

    @Autowired
    public CredentialRegisterServiceSchedulerTest(DummyMetadataRetriever dummyMetadataRetriever) {
        this.dummyMetadataRetriever = dummyMetadataRetriever;
        this.credentialRegisterService = new CredentialRegisterService(dummyMetadataRetriever);
    }

    @Test
    @DisplayName("When value already exists, and scheduler updates, the new values overwrite the old ones")
    void schedulerTest(){
        Mockito.when(dummyMetadataRetriever.getListOfIssuer()).thenReturn(MockData.mockCredentialIssuersListOne());
        credentialRegisterService.mapInputToResponse();
        Credentials credentials = credentialRegisterService.getCredentials();

        assertAll(
                () -> assertEquals("mock_utsteder", credentials.credentials().get(0).credentialIssuer()),
                () -> assertEquals("sd+jwt-vc", credentials.credentials().get(0).format()),
                () -> assertEquals("mdoc", credentials.credentials().get(1).format()),
                () -> assertEquals("jwt_vc_json", credentials.credentials().get(2).format())
        );

        Mockito.when(dummyMetadataRetriever.getListOfIssuer()).thenReturn(MockData.mockCredentialIssuersListTwo());
        credentialRegisterService.mapInputToResponse();
        Credentials credentials2 = credentialRegisterService.getCredentials();
        assertAll(
                () -> assertEquals("mock_utsteder2", credentials2.credentials().get(0).credentialIssuer()),
                () -> assertEquals("sd+jwt-vc", credentials2.credentials().get(0).format()),
                () -> assertEquals("mdoc", credentials2.credentials().get(1).format()),
                () -> assertEquals("jwt_vc_json", credentials2.credentials().get(2).format())
        );
    }
}
