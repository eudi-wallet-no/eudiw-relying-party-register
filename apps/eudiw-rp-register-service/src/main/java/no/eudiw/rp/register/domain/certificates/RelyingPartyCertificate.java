package no.eudiw.rp.register.domain.certificates;

import jakarta.persistence.*;
import lombok.Getter;
import no.eudiw.rp.register.domain.BaseEntity;

import java.security.cert.X509Certificate;

@Getter
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 16)
@Table(
    name = "relying_party_certificates",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_rp_certificate_type_serial",
        columnNames = {"type", "serial_no"}))
public abstract class RelyingPartyCertificate extends BaseEntity {
    public static final int NOT_REVOKED = -1;
    public static final int REVOKED = 0;

    @Column(name = "certificate_pem", nullable = false)
    @Convert(converter = X509CertificateConverter.class)
    private X509Certificate certificate;

    @Column(name = "subject_dn", nullable = false)
    private String subjectDn;

    @Column(name = "serial_no", nullable = false)
    private String serialNo;

    @Column(name = "ca_id", nullable = false)
    private String caId;

    @Column(name = "issuer", nullable = false)
    private String issuer;

    @Column(name = "valid_from_ms", nullable = false)
    private long validFromMs;

    @Column(name = "valid_until_ms", nullable = false)
    private long validUntilMs;

    @Column(name = "revocation_status", nullable = false)
    private int revocationStatus;

    protected RelyingPartyCertificate(String caId, X509Certificate certificate) {
        this.caId = caId;
        this.revocationStatus = NOT_REVOKED;
        this.certificate = certificate;
        this.issuer = certificate.getIssuerX500Principal().getName();
        this.subjectDn = certificate.getSubjectX500Principal().getName();
        this.serialNo = certificate.getSerialNumber().toString(10);
        this.validFromMs = certificate.getNotBefore().toInstant().toEpochMilli();
        this.validUntilMs = certificate.getNotAfter().toInstant().toEpochMilli();
    }

    protected RelyingPartyCertificate() { }

    public boolean isRevoked() {
        return revocationStatus >= 0;
    }

    public void revoke() {
        this.revoke(REVOKED);
    }

    public void revoke(int revocationStatus) {
        this.revocationStatus = revocationStatus;
    }
}
