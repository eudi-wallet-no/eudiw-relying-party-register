package no.idporten.eudiw.ca.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When loading the certificate authorities configuration")
@ActiveProfiles("test")
@SpringBootTest
public class CertificateAuthoritiesTest {

    @Autowired
    private CertificateAuthorities certificateAuthorities;

    @DisplayName("then root CAs should be loaded and valid")
    @ValueSource(strings = {"root", "root2"})
    @ParameterizedTest
    void testFindRoot(String caId) throws Exception {
        CertificateAuthority root = certificateAuthorities.findRoot(caId);
        assertNotNull(root);
        root.validate(root);
    }

    @DisplayName("then intermediate CAs should be loaded and connected to a valid root CA")
    @ValueSource(strings = {"eaa_provider", "junit2"})
    @ParameterizedTest
    void testFindRootFromIntermediate(String caId) throws Exception {
        CertificateAuthority intermediate = certificateAuthorities.findIntermediate(caId);
        CertificateAuthority root = certificateAuthorities.findRoot(intermediate.getRoot());
        assertNotEquals(root.getCertificate(), intermediate.getCertificate());
        intermediate.validate(root);
    }

}
