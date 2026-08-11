package no.idporten.eudiw.rp.admin.testdata;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

public class CertificatesGenerator extends TestDataGenerator {

    // NOTE: generating new keypair for each new test certificate makes testing
    // run very slowly, so we use the same keypair for all certificates. this
    // should be fine as long as we are not doing any signing in the rp-register-service.
    private static final KeyPair keyPair;
    static {
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to instantiate keypair for use in test data generation", e);
        }
    }

    public static String generateName() {
        return ResourceGenerator.generateRandomString(5, 10);
    }

    public static X509Certificate generateX509Certificate() {
        try {
            X500Name issuerName  = new X500Name("CN=issuer-" + generateName());
            X500Name subjectName = new X500Name("CN=subject-" + generateName());

            BigInteger serialNo = BigInteger.valueOf(Math.abs(rng.nextLong()));

            Instant timeNow = Instant.now();
            long millisInOneYear = 31540000000L;
            Date notBefore = Date.from(timeNow.minusMillis(rng.nextLong(0, millisInOneYear)));
            Date notAfter  = Date.from(timeNow.plusMillis(rng.nextLong(0,  millisInOneYear)));

            ContentSigner signer = null;

                signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());

            X509CertificateHolder certHolder =
                new JcaX509v3CertificateBuilder(
                    issuerName, serialNo, notBefore, notAfter, subjectName, keyPair.getPublic())
                    .build(signer);

            return new JcaX509CertificateConverter().getCertificate(certHolder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PKCS10CertificationRequest generatePKCS10Csr() throws Exception {
        X500Name name = new X500Name("CN=" + generateName());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        return new JcaPKCS10CertificationRequestBuilder(name, keyPair.getPublic())
                   .build(signer);
    }

}
