package no.eudiw.rp.register.domain.certificates;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.cert.X509Certificate;

@Getter
@Setter
@Entity
@DiscriminatorValue("ISSUER")
public class IssuerCertificate extends RelyingPartyCertificate {

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "entitlement_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingPartyEntitlement entitlement;

    public IssuerCertificate(
        X509Certificate certificate,
        String caId,
        RelyingPartyEntitlement entitlement) {
        super(caId, certificate);
        this.entitlement = entitlement;
    }

    protected IssuerCertificate() { }
}
