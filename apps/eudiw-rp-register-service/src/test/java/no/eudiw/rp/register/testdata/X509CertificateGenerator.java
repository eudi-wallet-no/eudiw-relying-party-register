package no.eudiw.rp.register.testdata;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

public class X509CertificateGenerator extends TestDataGenerator {
    public static X509Certificate generateX509Certificate() throws Exception {
        X500Name issuerName  = new X500Name("CN=issuer-" + generateName());
        X500Name subjectName = new X500Name("CN=subject-" + generateName());

        BigInteger serialNo = BigInteger.valueOf(Math.abs(rng.nextLong()));

        Instant timeNow = Instant.now();
        long millisInOneYear = 31540000000L;
        Date notBefore = Date.from(timeNow.minusMillis(rng.nextLong(0, millisInOneYear)));
        Date notAfter  = Date.from(timeNow.plusMillis(rng.nextLong(0,  millisInOneYear)));

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        X509CertificateHolder certHolder =
            new JcaX509v3CertificateBuilder(
                issuerName, serialNo, notBefore, notAfter, subjectName, keyPair.getPublic())
                .build(signer);

        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
}
