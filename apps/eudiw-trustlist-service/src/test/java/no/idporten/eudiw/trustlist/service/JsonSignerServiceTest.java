package no.idporten.eudiw.trustlist.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.Base64;
import no.idporten.eudiw.trustlist.config.KeyProvider;
import no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When trustlist of signed json is tested")
class JsonSignerServiceTest {

    @Autowired
    private KeyProvider keyProvider;

    @Autowired
    private JsonSignerService jsonSignerService;

    private LoTE loTE;

    @BeforeEach
    public void setup() {
        loTE = createLoTETrustlist();
    }

    private static LoTE createLoTETrustlist() {
        LoTE loTE = new LoTE();
        ListAndSchemeInformation listAndSchemeInformation = new ListAndSchemeInformation();
        listAndSchemeInformation.setSchemeTerritory("NO");
        listAndSchemeInformation.setLoTEType(URI.create("http://aca-trustlist-type"));
        loTE.setListAndSchemeInformation(listAndSchemeInformation);
        return loTE;
    }

    @DisplayName("then signed json should be verified with keystore public key")
    @Test
    void signedJsonVerifiedWithKeystore() throws ParseException, JOSEException {
        String signedJwt = jsonSignerService.signedJson(loTE);
        assertNotNull(signedJwt);

        JWSObject jwsObject = JWSObject.parse(signedJwt);

        PublicKey publicKey = keyProvider.getCertificate().getPublicKey();
        JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
        assertTrue(jwsObject.verify(verifier));
    }

    @DisplayName("then signed json should be verified with X509 certificate chain in header")
    @Test
    void signedJsonVerifiedWithX509cHeader() throws Exception {
        String signedJwt = jsonSignerService.signedJson(loTE);
        assertNotNull(signedJwt);

        JWSObject jwsObject = JWSObject.parse(signedJwt);

        List<Base64> x509CertChain = jwsObject.getHeader().getX509CertChain();
        assertNotNull(x509CertChain);
        assertFalse(x509CertChain.isEmpty());

        Base64 certBase64 = x509CertChain.getFirst();
        assertNotNull(certBase64);

        X509Certificate certificate = getX509Certificate(certBase64);
        assertNotNull(certificate);

        RSAPublicKey rsaPublicKey = (RSAPublicKey) certificate.getPublicKey();
        assertNotNull(rsaPublicKey);

        JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey);
        assertTrue(jwsObject.verify(verifier));
    }

    private static X509Certificate getX509Certificate(Base64 certBase64) throws CertificateException {
        byte[] certBytes = certBase64.decode();
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
    }


}