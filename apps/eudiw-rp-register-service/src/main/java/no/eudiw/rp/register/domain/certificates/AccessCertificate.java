package no.eudiw.rp.register.domain.certificates;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.cert.X509Certificate;

@Getter
@Setter
@Entity
@DiscriminatorValue("ACCESS")
public class AccessCertificate extends RelyingPartyCertificate {

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "relying_party_instance_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingPartyInstance relyingPartyInstance;

    public AccessCertificate(String caId, X509Certificate certificate, RelyingPartyInstance relyingPartyInstance) {
        super(caId, certificate);
        this.relyingPartyInstance = relyingPartyInstance;
    }

    protected AccessCertificate() { }
}
