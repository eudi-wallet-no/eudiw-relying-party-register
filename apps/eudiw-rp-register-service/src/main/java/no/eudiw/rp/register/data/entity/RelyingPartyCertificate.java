package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.eudiw.rp.register.data.certificates.X509CertificateConverter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.cert.X509Certificate;

@Getter
@Entity
@Table(name = "relying_party_certificate")
public class RelyingPartyCertificate extends BaseEntity {

    @Column(name = "certificate_pem", nullable = false)
    @Convert(converter = X509CertificateConverter.class)
    private X509Certificate certificate;

    @Column(name = "subject_dn", nullable = false)
    private String subjectDn;

    @Column(name = "serial_no", nullable = false)
    private String serialNo;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "relying_party_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    @Setter(AccessLevel.PACKAGE)
    private RelyingParty relyingParty;

    @Column(name = "valid_from_ms", nullable = false)
    private long validFromMs;

    @Column(name = "valid_until_ms", nullable = false)
    private long validUntilMs;

    public RelyingPartyCertificate(X509Certificate certificate) {
        this(certificate, null);
    }

    public RelyingPartyCertificate(X509Certificate certificate, RelyingParty relyingParty) {
        this.id = null;

        this.certificate = certificate;
        this.subjectDn = certificate.getSubjectX500Principal().getName();
        this.serialNo = certificate.getSerialNumber().toString(10);
        this.validFromMs = certificate.getNotBefore().toInstant().toEpochMilli();
        this.validUntilMs = certificate.getNotAfter().toInstant().toEpochMilli();

        this.relyingParty = relyingParty;
    }

    // for JPA instantiation.
    protected RelyingPartyCertificate() { }
}
