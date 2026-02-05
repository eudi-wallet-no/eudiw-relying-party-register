package no.eudiw.rp.register.data.entity.certificates;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import no.eudiw.rp.register.data.entity.BaseEntity;

import java.security.cert.X509Certificate;

@MappedSuperclass
@Getter
public abstract class BaseCertificateEntity extends BaseEntity {
    public static final int NOT_REVOKED = -1;
    public static final int REVOKED = 0;

    @Column(name = "certificate_pem", nullable = false)
    @Convert(converter = X509CertificateConverter.class)
    protected X509Certificate certificate;

    @Column(name = "subject_dn", nullable = false)
    protected String subjectDn;

    @Column(name = "serial_no", nullable = false)
    protected String serialNo;

    @Column(name = "ca_id", nullable = false)
    protected String caId;

    @Column(name = "issuer", nullable = false)
    protected String issuer;

    @Column(name = "valid_from_ms", nullable = false)
    protected long validFromMs;

    @Column(name = "valid_until_ms", nullable = false)
    protected long validUntilMs;

    @Column(name = "revocation_status", nullable = false)
    protected int revocationStatus;

    protected BaseCertificateEntity(X509Certificate certificate, String caId) {
        this.id = null;
        this.caId = caId;
        revocationStatus = NOT_REVOKED;

        this.certificate = certificate;
        this.issuer = certificate.getIssuerX500Principal().getName();
        this.subjectDn = certificate.getSubjectX500Principal().getName();
        this.serialNo = certificate.getSerialNumber().toString(10);
        this.validFromMs = certificate.getNotBefore().toInstant().toEpochMilli();
        this.validUntilMs = certificate.getNotAfter().toInstant().toEpochMilli();
    }

    // for JPA instantiation.
    protected BaseCertificateEntity() { }

    public void revoke() {
        this.revoke(REVOKED);
    }

    public void revoke(int revocationStatus) {
        this.revocationStatus = revocationStatus;
    }
}
