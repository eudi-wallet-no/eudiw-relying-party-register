package no.eudiw.rp.register.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@BatchSize(size = 50)
@Table(name = "wallet_relying_party_service_entitlement", uniqueConstraints =
    @UniqueConstraint(name = "uq_service_entitlement",
        columnNames = {"wallet_relying_party_service_id", "entitlement"}))
public class ServiceEntitlement extends BaseEntity {

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_relying_party_service_id", columnDefinition = "UUID", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private WalletRelyingPartyService walletRelyingPartyService;

    @Column(name = "entitlement", nullable = false, updatable = false)
    private String entitlement;

    @Setter
    @Column(name = "credential_issuer_url")
    private String credentialIssuerUrl;

    public ServiceEntitlement(String entitlement, String credentialIssuerUrl) {
        this.entitlement = entitlement;
        this.credentialIssuerUrl = credentialIssuerUrl;
    }

    protected ServiceEntitlement() { }
}
