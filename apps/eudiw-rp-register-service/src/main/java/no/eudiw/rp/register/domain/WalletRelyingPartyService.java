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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "uses_intermediaries", columnDefinition = "JSON", nullable = false)
    private List<IntermediaryReference> usesIntermediaries = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "is_intermediary", nullable = false)
    private boolean isIntermediary;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "served_wrp_services", columnDefinition = "JSON", nullable = false)
    private List<String> servedWRPServices = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "wallet_relying_party_id", columnDefinition = "UUID", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private WalletRelyingParty walletRelyingParty;

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "walletRelyingPartyService",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<IntendedUse> intendedUses = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "walletRelyingPartyService",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<RelyingPartyInstance> relyingPartyInstances = new ArrayList<>();

    @Column(name = "created_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long createdMs;

    @Column(name = "last_updated_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long lastUpdatedMs;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public WalletRelyingPartyService(
        String serviceTradeName,
        WalletRelyingParty walletRelyingParty,
        List<RelyingPartyInstance> relyingPartyInstances
    ) {
        this(serviceTradeName, walletRelyingParty, relyingPartyInstances, false, List.of(), List.of());
    }

    public WalletRelyingPartyService(
        String serviceTradeName,
        WalletRelyingParty walletRelyingParty,
        List<RelyingPartyInstance> relyingPartyInstances,
        boolean isIntermediary,
        List<IntermediaryReference> usesIntermediaries,
        List<String> servedWRPServices
    ) {
        this.serviceTradeName = serviceTradeName;
        this.walletRelyingParty = walletRelyingParty;
        setRelyingPartyInstances(relyingPartyInstances);
        setIntermediaryConfiguration(isIntermediary, usesIntermediaries, servedWRPServices);
    }

    public void setIntermediaryConfiguration(
        boolean isIntermediary,
        List<IntermediaryReference> usesIntermediaries,
        List<String> servedWRPServices
    ) {
        List<IntermediaryReference> usesIntermediariesCopy =
            usesIntermediaries == null ? new ArrayList<>() : new ArrayList<>(usesIntermediaries);
        List<String> servedWRPServicesCopy =
            servedWRPServices == null ? new ArrayList<>() : new ArrayList<>(servedWRPServices);

        validateIntermediaryConfiguration(isIntermediary, usesIntermediariesCopy, servedWRPServicesCopy);

        this.isIntermediary = isIntermediary;
        this.usesIntermediaries = usesIntermediariesCopy;
        this.servedWRPServices = servedWRPServicesCopy;
    }

    private static void validateIntermediaryConfiguration(
        boolean isIntermediary,
        List<IntermediaryReference> usesIntermediaries,
        List<String> servedWRPServices
    ) {
        if (isIntermediary && !usesIntermediaries.isEmpty()) {
            throw new IllegalArgumentException("An intermediary service cannot use intermediaries");
        }
        if (!isIntermediary && !servedWRPServices.isEmpty()) {
            throw new IllegalArgumentException("Only an intermediary service can serve WRP services");
        }
    }

    public List<IntermediaryReference> getUsesIntermediaries() {
        return List.copyOf(usesIntermediaries);
    }

    public boolean isIntermediary() {
        return isIntermediary;
    }

    public List<String> getServedWRPServices() {
        return List.copyOf(servedWRPServices);
    }

    public boolean isIntermediary() {
        return isIntermediary;
    }

    public void setIntendedUses(List<IntendedUse> intendedUses) {
        List<IntendedUse> intendedUsesCopy = intendedUses == null ? List.of() : new ArrayList<>(intendedUses);
        this.intendedUses.clear();
        intendedUsesCopy.forEach(intendedUse -> intendedUse.setWalletRelyingPartyService(this));
        this.intendedUses.addAll(intendedUsesCopy);
    }

    public void setRelyingPartyInstances(List<RelyingPartyInstance> relyingPartyInstances) {
        List<RelyingPartyInstance> relyingPartyInstancesCopy = relyingPartyInstances == null ? List.of() : new ArrayList<>(relyingPartyInstances);
        this.relyingPartyInstances.clear();
        relyingPartyInstancesCopy.forEach(instance -> instance.setWalletRelyingPartyService(this));
        this.relyingPartyInstances.addAll(relyingPartyInstancesCopy);
    }

    @Transient
    public List<String> getEntitlements() {
        return relyingPartyInstances.stream()
            .filter(RelyingPartyInstance::isActive)
            .flatMap(instance -> instance.getRelyingPartyEntitlements().stream())
            .map(RelyingPartyEntitlement::getEntitlement)
            .distinct()
            .toList();
    }

    // for JPA instantiation.
    protected WalletRelyingPartyService() { }

    public void setUpdated() {
        this.lastUpdatedMs = Instant.now().toEpochMilli();
    }

    @PrePersist
    protected void onPrePersist() {
        validateIntermediaryConfiguration(isIntermediary, usesIntermediaries, servedWRPServices);
        this.lastUpdatedMs = this.createdMs = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onPreUpdate() {
        validateIntermediaryConfiguration(isIntermediary, usesIntermediaries, servedWRPServices);
        setUpdated();
    }

    @PostLoad
    protected void onPostLoad() {
        validateIntermediaryConfiguration(isIntermediary, usesIntermediaries, servedWRPServices);
    }
}
