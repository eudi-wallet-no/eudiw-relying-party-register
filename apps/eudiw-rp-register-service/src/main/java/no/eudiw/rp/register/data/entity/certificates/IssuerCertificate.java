package no.eudiw.rp.register.data.entity.certificates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyEntitlement;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.cert.X509Certificate;

@Getter
@Setter
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
    private RelyingPartyEntitlement entitlement;

    public IssuerCertificate(X509Certificate certificate, RelyingPartyEntitlement relyingPartyEntitlement) {
        super(certificate);
        this.entitlement = relyingPartyEntitlement;
    }

    // for JPA instantiation.
    protected IssuerCertificate() { }
}
