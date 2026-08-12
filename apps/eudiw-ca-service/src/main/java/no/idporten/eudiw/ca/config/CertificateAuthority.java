package no.idporten.eudiw.ca.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Represents a certificate authority with key material.  Call {@link #init(String)} before use.
 */
@Getter
@RequiredArgsConstructor
public class CertificateAuthority {

    /**
     * CA identifier.  Used to reference CA in URL and configuration.
     */
    private String id;

    /**
     * Root CA identifier.  Used to locate the CA that signed this CA.
     */
    @NotNull
    private final String root;

    private boolean isRoot() {
        return Objects.equals(id, root);
    }

    @Min(1)
    @Max(365)
    private final int lifetimeDays;
    @NotNull
    private final URI crlDistributionPoint;
    @NotNull
    private final URI certificateUri;

    /**
     * Profile for certificates signed with this certificate authority
     */
    @Valid
    private final CertificateProfile certificateProfile;

    @Getter(AccessLevel.PRIVATE)
    @NotNull
    private final KeyStoreProperties keyStore;

    private PrivateKey privateKey;
    private X509Certificate certificate;
    private PublicKey publicKey;

    /**
     * Load key material for this certificate authority
     */
    public void init(String id) throws Exception {
        this.id = id;
        KeyStore keyStore = new KeyStoreProvider(this.keyStore).getKeyStore();
        this.privateKey = (PrivateKey) Objects.requireNonNull(keyStore.getKey(this.keyStore.keyAlias(), this.keyStore.keyPassword().toCharArray()), "Failed to load private key");
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
