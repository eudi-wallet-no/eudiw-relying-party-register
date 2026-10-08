package no.eudiw.rp.register.domain.relyingparty;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import no.eudiw.rp.register.domain.BaseEntity;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "relying_party_entitlement")
public class RelyingPartyEntitlement extends BaseEntity {

    @Column(name = "entitlement", nullable = false)
    private String entitlement;

    @Column(name = "credential_issuer_url")
    private String credentialIssuerUrl;

    @ManyToOne
    @JoinColumn(name = "relying_party_instance_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingPartyInstance relyingPartyInstance;

    @OneToMany(
        mappedBy = "entitlement",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<IssuerCertificate> issuerCertificates =
        new ArrayList<>();

    public void addIssuerCertificate(IssuerCertificate issuerCertificate) {
        issuerCertificate.setEntitlement(this);
        issuerCertificates.add(issuerCertificate);
    }

    public void setIssuerCertificates(List<IssuerCertificate> issuerCertificates) {
        this.issuerCertificates.clear();
        if (issuerCertificates != null) {
            issuerCertificates.forEach(this::addIssuerCertificate);
        }
    }

    public RelyingPartyEntitlement(String entitlement) {
        this(entitlement, null);
    }
    public RelyingPartyEntitlement(String entitlement, String credentialIssuerUrl) {
        this.entitlement = entitlement;
        this.credentialIssuerUrl = credentialIssuerUrl;
    }

    // for JPA instantiation.
    protected RelyingPartyEntitlement() { }
}
