package no.eudiw.rp.register.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "wallet_relying_party_service_intended_use")
public class IntendedUse extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "wallet_relying_party_service_id", columnDefinition = "UUID", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private WalletRelyingPartyService walletRelyingPartyService;

    @Column(name = "intended_use_identifier")
    private String intendedUseIdentifier;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "revoked_at")
    private String revokedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "purpose", columnDefinition = "JSON")
    private List<MultiLangString> purpose = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "privacy_policy", columnDefinition = "JSON")
    private List<Policy> privacyPolicy = new ArrayList<>();

    public IntendedUse(
        String intendedUseIdentifier,
        String createdAt,
        String revokedAt,
        List<MultiLangString> purpose,
        List<Policy> privacyPolicy
    ) {
        this.intendedUseIdentifier = intendedUseIdentifier;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
        this.purpose = purpose == null ? new ArrayList<>() : new ArrayList<>(purpose);
        this.privacyPolicy = privacyPolicy == null ? new ArrayList<>() : new ArrayList<>(privacyPolicy);
    }

    protected IntendedUse() { }
}
