package no.idporten.eudiw.trustlist.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.util.Base64;
import no.idporten.eudiw.trustlist.TestDataGenerator;
import no.idporten.eudiw.trustlist.config.TrustListACAProperties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.lib.keystore.KeystoreConfig;
import no.idporten.lib.keystore.KeystoreManager;
import no.idporten.lib.keystore.KeystoreManagerProperties;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("When trustlist of signed json is tested")
class JsonSignerServiceTest {

    private KeystoreManager keyProvider;

    private JsonSignerService jsonSignerService;

    @Mock
    private TrustListACAProperties acaProperties;

    private LoTE loTE;

    private final static String keystoreAlias = "unittest-trustlist";
    private final static String defaultKeystore = "signing-602";

    @BeforeEach
    public void setup() {
        keyProvider = createKeyManager();
        jsonSignerService = new JsonSignerService(acaProperties, keyProvider);

        loTE = TestDataGenerator.createLoTETrustlist();

        when(acaProperties.keystore()).thenReturn(defaultKeystore);
    }

    private static @NonNull KeystoreManager createKeyManager() {

        String password = "changeit";
        String keyData = "base64:MIIFCwIBAzCCBLUGCSqGSIb3DQEHAaCCBKYEggSiMIIEnjCCAZUGCSqGSIb3DQEHAaCCAYYEggGCMIIBfjCCAXoGCyqGSIb3DQEMCgECoIIBDzCCAQswZgYJKoZIhvcNAQUNMFkwOAYJKoZIhvcNAQUMMCsEFAVcI471KMwjcCmZlNScjdzvOZ4uAgInEAIBIDAMBggqhkiG9w0CCQUAMB0GCWCGSAFlAwQBKgQQ3/ADGP0ZiJOobN93RJGPvQSBoNLplMfYAZGCIsEchhS6dauMPi9saAxAq+FJW8pOlB39cAYQRtlm/anve1WpP5JoQ4fhO7MoGz5wFRFqdscO+8irBAZy2HoKd8XZtbp8vAhcTVAL0eH3eHiABS+Yagf21hfZ/YyR7Q2b3HcmYM0qU64T6ulHVspfPu4saxrZVKB8yBBF4ClDlQCJFiFHk9Y09ZjfAQBGOFBsIXcjUPrrhNcxWDAzBgkqhkiG9w0BCRQxJh4kAHUAbgBpAHQAdABlAHMAdAAtAHQAcgB1AHMAdABsAGkAcwB0MCEGCSqGSIb3DQEJFTEUBBJUaW1lIDE3NzM5MzAyNDYxOTMwggMBBgkqhkiG9w0BBwagggLyMIIC7gIBADCCAucGCSqGSIb3DQEHATBmBgkqhkiG9w0BBQ0wWTA4BgkqhkiG9w0BBQwwKwQUK0B0uYCG94eSlqcVFvaEnYIOXQkCAicQAgEgMAwGCCqGSIb3DQIJBQAwHQYJYIZIAWUDBAEqBBCTS7lvjQp3xa+mhu+OPzXlgIICcJRFmIMNomYBQ/WNoMvsKD6ZFCdliAnrv11MWd/twsG3Yst2IB79VUPKxAFY1ssjo4r52rzAx4M9WqOoil3evX0LI3j8u+85jQNBlBCZuC45gBYVegKSSdh92epnAvrFVeDdImq4eyYf3foa/KbU3yZy3SIjW53vAVHWGiPdjF6MvdWua++KOwhf+VOT2Vu0eHJFJP90X17kfEWbOBjZLPonGEohh+CsgG/XAttVgHylx1atwmUqScszM6qP7Z/0KgtEeyEq9WTD7z3lvGGH16x7Rtt6ikVc/Sw8/kGXreHzNelQAtslzBvCo1r3lDVtq/2IvkklxcaqmAAg0TXfq5tae9vLViE6aD3fACc1EAsdynAKUNpDhTTLDavatWQZPy1ujFYOWwqBeJ4+OjpZXxVwWHW/5YbQpwxEyi0xHp1160ZIoZoR4PkQX/fuY1Yq2iF+iK/W3JFAvMfaQr7O5t4ggcRSx4ctPy5mBwwuMmX1ySf/ZO9uyq9gOrXe+OI0Kzeb/G/tNgTrsFifLllwtJIBRf6fR2eWM08cKsLYFib1TPSskT5iavlF4Ak1boXr4mfuHFaNhbOfwVReOwbdBfTk1ZJTilLNsLuwTBhhEPvk6OzyA9yyRwAgFZr4+5uS5TPUawbZh+lPpHs3DIyLaFcx8kr9/4vfVSxn28+bZHH9LC7pXnfqSWrUUoavreN9Sxawy6l6zLXKf+B0ArRMqS9NHXCCwrTjJ55F89QXXKpCiCZ8eZVAcal3FF34+ClXEnBdy6OqgnnL9JyHuHiDd3CxcJmTgqLQqybrrjsQTIt4xoGQSDs4rAzoZ5MeSqmpCTBNMDEwDQYJYIZIAWUDBAIBBQAEIFSJ2hB1MEkVNNXBbapgnE43d1Pyakpr3Tiqmnl3+dxVBBRW7xA2CC/FnKSAU9VxGzEZba2XdQICJxA=";
        KeystoreConfig keystoreConfig = new KeystoreConfig(keyData, "PKCS12", password, keystoreAlias, password);
        return new KeystoreManager(new KeystoreManagerProperties(defaultKeystore, Map.of(defaultKeystore, keystoreConfig)));
    }


    @DisplayName("then signed json should be verified with keystore public key")
    @Test
    void signedJsonVerifiedWithKeystore() throws ParseException, JOSEException {

        String signedJwt = jsonSignerService.signedTrustlist(loTE);
        assertNotNull(signedJwt);

        JWSObject jwsObject = JWSObject.parse(signedJwt);

        PublicKey publicKey = keyProvider.getKeyProvider(defaultKeystore).publicKey();
        JWSVerifier verifier = new ECDSAVerifier((ECPublicKey) publicKey);
        assertTrue(jwsObject.verify(verifier));
    }

    @DisplayName("then signed json should be verified with X509 certificate chain in header")
    @Test
    void signedJsonVerifiedWithX509cHeader() throws Exception {
        String signedJwt = jsonSignerService.signedTrustlist(loTE);
        assertNotNull(signedJwt);

        JWSObject jwsObject = JWSObject.parse(signedJwt);

        List<Base64> x509CertChain = jwsObject.getHeader().getX509CertChain();
        assertNotNull(x509CertChain);
        assertFalse(x509CertChain.isEmpty());

        Base64 certBase64 = x509CertChain.getFirst();
        assertNotNull(certBase64);

        X509Certificate certificate = getX509Certificate(certBase64);
        assertNotNull(certificate);

        ECPublicKey ecPublicKey = (ECPublicKey) certificate.getPublicKey();
        assertNotNull(ecPublicKey);

        JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);
        assertTrue(jwsObject.verify(verifier));
    }

    private static X509Certificate getX509Certificate(Base64 certBase64) throws CertificateException {
        byte[] certBytes = certBase64.decode();
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
    }


}