package no.eudiw.rp.register.domain.certificates;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.cert.X509Certificate;

@Getter
@Setter
@Entity
@DiscriminatorValue("ISSUER")
public class IssuerCertificate extends RelyingPartyCertificate {

    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "relying_party_instance_id",
        columnDefinition = "UUID",
        nullable = false,
        updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingPartyInstance relyingPartyInstance;

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
        this.relyingPartyInstance = entitlement.getRelyingPartyInstance();
    }

    protected IssuerCertificate() { }
}
