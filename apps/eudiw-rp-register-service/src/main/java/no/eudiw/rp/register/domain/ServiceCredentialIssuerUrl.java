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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "wallet_relying_party_service_credential_issuer_url", uniqueConstraints =
    @UniqueConstraint(name = "uq_service_credential_issuer_role",
        columnNames = {"wallet_relying_party_service_id", "entitlement"}))
public class ServiceCredentialIssuerUrl extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_relying_party_service_id", columnDefinition = "UUID", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private WalletRelyingPartyService walletRelyingPartyService;

    @Column(name = "entitlement", nullable = false)
    private String entitlement;

    @Column(name = "credential_issuer_url")
    private String credentialIssuerUrl;

    public ServiceCredentialIssuerUrl(String entitlement, String credentialIssuerUrl) {
        this.entitlement = entitlement;
        this.credentialIssuerUrl = credentialIssuerUrl;
    }

    protected ServiceCredentialIssuerUrl() { }
}
