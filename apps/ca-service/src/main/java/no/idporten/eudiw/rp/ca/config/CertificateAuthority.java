package no.idporten.eudiw.rp.ca.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * Represents a certificate authority with key material.  Call {@link #init()} before use.
 */
@Getter
@RequiredArgsConstructor
public class CertificateAuthority {

    private final boolean root;
    @Min(1)
    @Max(365)
    private final int lifetimeDays;
    @NotNull
    private final URI crlDistributionPoint;
    @NotNull
    private final URI certificateUri;

    @Getter(AccessLevel.PRIVATE)
    @NotNull
    private final KeyStoreProperties keyStore;

    private PrivateKey privateKey;
    private X509Certificate certificate;
    private PublicKey publicKey;

    /**
     * Load key material for this certificate authority
     */
    public void init() throws Exception {
        KeyStore keyStore = new KeyStoreProvider(this.keyStore).getKeyStore();
        this.privateKey = (PrivateKey) keyStore.getKey(this.keyStore.keyAlias(), this.keyStore.keyPassword().toCharArray());
        this.certificate = (X509Certificate) keyStore.getCertificate(this.keyStore.keyAlias());
        this.publicKey = this.certificate.getPublicKey();
    }

    /**
     * Validate this certificate authority against the issuer
     */
    public void validate(CertificateAuthority issuer) throws Exception {
        getCertificate().verify(issuer.getPublicKey());
        if (isRoot() && !getCertificate().equals(issuer.getCertificate())) {
            throw new IllegalArgumentException("Root certificate must be self-signed");
        } else if (!isRoot() && getCertificate().equals(issuer.getCertificate())) {
            throw new IllegalArgumentException("Intermediate certificate must not be self-signed");
        }
    }

}
