package no.idporten.eudiw.trustlist.config;

import lombok.Getter;
import org.springframework.context.ApplicationContextException;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;

@Getter
public class KeyProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final Certificate certificate;
    private final List<Certificate> certificateChain;


    public KeyProvider(KeyStore keyStore, String alias, String password) {
        try {
            privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            certificate = keyStore.getCertificate(alias);
            publicKey = certificate.getPublicKey();
            certificateChain = Arrays.asList(keyStore.getCertificateChain(alias));
        } catch (Exception e) {
            throw new ApplicationContextException("Failed to load key.", e);
        }
    }

}
