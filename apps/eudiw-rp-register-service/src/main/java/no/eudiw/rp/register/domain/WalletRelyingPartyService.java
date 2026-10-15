package no.eudiw.rp.register.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "wallet_relying_party_service")
public class WalletRelyingPartyService extends BaseEntity {

    @Column(name = "service_trade_name", nullable = false)
    private String serviceTradeName;

    @Column(name = "service_identifier", columnDefinition = "TEXT")
    private String serviceIdentifier;

    @Column(name = "support_uri", columnDefinition = "TEXT")
    private String supportUri;

    @Column(name = "email", columnDefinition = "TEXT")
    private String email;

    @Column(name = "phone", columnDefinition = "TEXT")
    private String phone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "srv_description", columnDefinition = "JSON")
    private List<List<MultiLangString>> srvDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sub_entitlements", columnDefinition = "JSON", nullable = false)
    private List<String> subEntitlements = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provides_attestations", columnDefinition = "JSON", nullable = false)
    private List<ProvidedAttestation> providesAttestations = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "wallet_relying_party_id", columnDefinition = "UUID", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private WalletRelyingParty walletRelyingParty;

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "walletRelyingPartyService",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<RelyingPartyInstance> relyingPartyInstances = new ArrayList<>();

    @Column(name = "created_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long createdMs;

    @Column(name = "last_updated_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long lastUpdatedMs;

    public WalletRelyingPartyService(
        String serviceTradeName,
        WalletRelyingParty walletRelyingParty,
        List<RelyingPartyInstance> relyingPartyInstances
    ) {
        this.serviceTradeName = serviceTradeName;
        this.walletRelyingParty = walletRelyingParty;
        setRelyingPartyInstances(relyingPartyInstances);
    }

    public void setRelyingPartyInstances(List<RelyingPartyInstance> relyingPartyInstances) {
        this.relyingPartyInstances.clear();
        if (relyingPartyInstances != null) {
            relyingPartyInstances.forEach(instance -> instance.setWalletRelyingPartyService(this));
            this.relyingPartyInstances.addAll(relyingPartyInstances);
        }
    }

    // Reuse the existing per-instance relation in the service-level TS5 view.
    @Transient
    public List<String> getEntitlements() {
        return relyingPartyInstances.stream()
            .flatMap(instance -> instance.getRelyingPartyEntitlements().stream())
            .map(RelyingPartyEntitlement::getEntitlement)
            .distinct()
            .toList();
    }

    // for JPA instantiation.
    protected WalletRelyingPartyService() { }

    @PrePersist
    protected void onPrePersist() {
        this.lastUpdatedMs = this.createdMs = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.lastUpdatedMs = Instant.now().toEpochMilli();
    }
}
