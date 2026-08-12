package no.idporten.eudiw.credential.registry.response;

import no.idporten.eudiw.credential.registry.integration.MockData;
import no.idporten.eudiw.credential.registry.response.model.Credentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When formatting and presenting credentials")

public class CredentialsTest {


    @Test
    @DisplayName("When display does not exist at top level in the well known endpoint")
    void entryWithoutDisplay() {
        Credentials credentials1 = MockData.mockCredentialsListOne();
        assertAll(
                () -> assertEquals("mock_utsteder", credentials1.credentials().get(0).credentialIssuer()),
                () -> assertEquals("sd+jwt-vc", credentials1.credentials().get(1).format()),
                () -> assertNull(credentials1.credentials().get(2).display())
        );
    }
}
