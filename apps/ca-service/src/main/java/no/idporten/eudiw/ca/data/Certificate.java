package no.idporten.eudiw.ca.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.cert.X509Certificate;
import java.time.Clock;

@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(CertificateId.class)
@Table(name = "certificate")
public class Certificate {

    @Id
    @Column(name = "serial_no", nullable = false)
    private String serialNo;

    @Id
    @Column(name = "issuer_ca", nullable = false)
    private String issuerCa;

    @Column(name = "certificate", nullable = false)
    @Convert(converter = X509CertificateConverter.class)
    private X509Certificate certificate;

    @Column(name = "valid_from_ms", nullable = false)
    private long validFromMs;

    @Column(name = "valid_until_ms", nullable = false)
    private long validUntilMs;

    @Column(name = "revoked_at_ms", nullable = false)
    private long revokedAtMs;

    @Column(name = "revocation_reason", nullable = false)
    private int revocationReason = -1;

    public Certificate(X509Certificate certificate, String issuerCa) {
        this.certificate = certificate;
        this.serialNo = SerialNumberUtils.convertToString(certificate.getSerialNumber());
        this.validFromMs = certificate.getNotBefore().toInstant().toEpochMilli();
        this.validUntilMs = certificate.getNotAfter().toInstant().toEpochMilli();
        this.issuerCa = issuerCa;
    }

    public boolean isRevoked() {
        return revocationReason > -1;
    }

    public void revoke(int revocationReason) {
        this.revokedAtMs = Clock.systemUTC().millis();
        this.revocationReason = revocationReason;
    }

}
