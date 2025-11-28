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
@Table(name = "access_certificate")
public class AccessCertificate extends BaseCertificateEntity {

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "relying_party_instance_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    @Setter(AccessLevel.PACKAGE)
    private RelyingPartyInstance relyingPartyInstance;

    public AccessCertificate(X509Certificate certificate, RelyingPartyInstance relyingPartyInstance) {
        super(certificate);
        this.relyingPartyInstance = relyingPartyInstance;
    }

    // for JPA instantiation.
    protected AccessCertificate() { }
}
