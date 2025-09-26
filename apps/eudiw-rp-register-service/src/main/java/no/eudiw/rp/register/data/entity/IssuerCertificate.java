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
@Table(name = "issuer_certificate")
public class IssuerCertificate extends BaseEntity {

    @Column(name = "certificate_pem", nullable = false)
    @Convert(converter = X509CertificateConverter.class)
    private X509Certificate certificate;

    @Column(name= "issuer", nullable = false)
    private String issuer;

    @Column(name = "ca_id", nullable = false)
    private String caId;

    @Column(name = "subject_dn", nullable = false)
    private String subjectDn;

    @Column(name = "serial_no", nullable = false)
    private String serialNo;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "entitlement_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    @Setter(AccessLevel.PACKAGE)
    private RelyingPartyEntitlement entitlement;

    @Column(name = "valid_from_ms", nullable = false)
    private long validFromMs;

    @Column(name = "valid_until_ms", nullable = false)
    private long validUntilMs;

    public IssuerCertificate(X509Certificate certificate) {
        this(certificate, null);
    }

    public IssuerCertificate(X509Certificate certificate, RelyingPartyEntitlement relyingPartyEntitlement) {
        this.id = null;
        this.certificate = certificate;
        this.issuer = certificate.getIssuerX500Principal().getName();

        //Denne er midlertidig, da api ikke er oppdatert for og støtte forskjellige sertifikat typer
        this.caId = "caId";

        this.subjectDn = certificate.getSubjectX500Principal().getName();
        this.serialNo = certificate.getSerialNumber().toString(10);
        this.validFromMs = certificate.getNotBefore().toInstant().toEpochMilli();
        this.validUntilMs = certificate.getNotAfter().toInstant().toEpochMilli();

        this.entitlement = relyingPartyEntitlement;
    }

    // for JPA instantiation.
    protected IssuerCertificate() { }
}