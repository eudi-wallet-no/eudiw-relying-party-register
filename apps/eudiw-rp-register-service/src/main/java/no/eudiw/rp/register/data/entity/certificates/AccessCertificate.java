package no.eudiw.rp.register.data.entity.certificates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyInstance;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.cert.X509Certificate;

@Getter
@Setter
@Entity
@Table(name = "access_certificate")
public class AccessCertificate extends BaseCertificateEntity {

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "relying_party_instance_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingPartyInstance relyingPartyInstance;

    public AccessCertificate(X509Certificate certificate, RelyingPartyInstance relyingPartyInstance) {
        super(certificate);
        this.relyingPartyInstance = relyingPartyInstance;
    }

    // for JPA instantiation.
    protected AccessCertificate() { }
}
