package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.cert.X509Certificate;

@Getter
@Entity
@Table(name = "issuer_certificate")
public class IssuerCertificate extends BaseCertificateEntity {

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "entitlement_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    @Setter(AccessLevel.PACKAGE)
    private RelyingPartyEntitlement entitlement;

    public IssuerCertificate(X509Certificate certificate, RelyingPartyEntitlement relyingPartyEntitlement) {
        super(certificate);
        this.entitlement = relyingPartyEntitlement;
    }

    // for JPA instantiation.
    protected IssuerCertificate() { }
}
