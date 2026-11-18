package no.eudiw.rp.register.domain.relyingparty;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.eudiw.rp.register.domain.BaseEntity;
import no.eudiw.rp.register.domain.ServiceEntitlement;
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

    @ManyToOne
    @JoinColumn(name = "service_entitlement_id", columnDefinition = "UUID", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private ServiceEntitlement serviceEntitlement;

    @ManyToOne
    @JoinColumn(name = "relying_party_instance_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingPartyInstance relyingPartyInstance;

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "entitlement",
        fetch = FetchType.EAGER,
        cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<IssuerCertificate> issuerCertificates =
        new ArrayList<>();

    public void addIssuerCertificate(IssuerCertificate certificate) {
        certificate.setEntitlement(this);
        issuerCertificates.add(certificate);
        certificate.getRelyingPartyInstance().getIssuerCertificates().add(certificate);
    }

    public RelyingPartyEntitlement(String entitlement) {
        this(new ServiceEntitlement(entitlement, null));
    }

    public RelyingPartyEntitlement(ServiceEntitlement serviceEntitlement) {
        this.serviceEntitlement = serviceEntitlement;
    }

    public String getEntitlement() {
        return serviceEntitlement.getEntitlement();
    }

    public String getCredentialIssuerUrl() {
        return serviceEntitlement.getCredentialIssuerUrl();
    }

    @PrePersist
    @PreUpdate
    protected void validateServiceEntitlement() {
        if (relyingPartyInstance == null || serviceEntitlement == null) {
            throw new IllegalArgumentException(
                "The role selection must have an instance reference and a service role reference.");
        }

        var roleService = serviceEntitlement.getWalletRelyingPartyService();
        var instanceService = relyingPartyInstance.getWalletRelyingPartyService();

        if (roleService == null || instanceService == null
            || roleService != instanceService && (roleService.getId() == null
                || !roleService.getId().equals(instanceService.getId()))) {
            throw new IllegalArgumentException("The selected role must be on the same service as the instance.");
        }
    }

    // for JPA instantiation.
    protected RelyingPartyEntitlement() { }
}
