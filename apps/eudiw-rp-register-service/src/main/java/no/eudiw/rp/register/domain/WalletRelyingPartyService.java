package no.eudiw.rp.register.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "wallet_relying_party_service")
public class WalletRelyingPartyService extends BaseEntity {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String STATUS_REVOKED = "revoked";

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
    private List<MultiLangString> srvDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issuer_policies", columnDefinition = "JSON", nullable = false)
    private List<Policy> issuerPolicies = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sub_entitlements", columnDefinition = "JSON", nullable = false)
    private List<String> subEntitlements = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "walletRelyingPartyService",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ProvidedAttestation> providesAttestations = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "uses_intermediaries", columnDefinition = "JSON", nullable = false)
    private List<ServiceReference> usesIntermediaries = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "is_intermediary", nullable = false)
    private boolean isIntermediary;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "served_wrp_services", columnDefinition = "JSON", nullable = false)
    private List<ServiceReference> servedWRPServices = new ArrayList<>();

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
    @BatchSize(size = 50)
    private List<IntendedUse> intendedUses = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "walletRelyingPartyService",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<RelyingPartyInstance> relyingPartyInstances = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "walletRelyingPartyService",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ServiceEntitlement> serviceEntitlements = new ArrayList<>();

    @Column(name = "created_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long createdMs;

    @Column(name = "last_updated_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long lastUpdatedMs;

    @Column(name = "active", nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean active = true;

    @Column(name = "status", nullable = false, length = 16)
    @Setter(AccessLevel.NONE)
    private String status = STATUS_ACTIVE;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private LocalDate createdAt = currentDate();

    @Column(name = "suspended_at")
    @Setter(AccessLevel.NONE)
    private LocalDate suspendedAt;

    @Column(name = "revoked_at")
    @Setter(AccessLevel.NONE)
    private LocalDate revokedAt;

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
        List<ServiceReference> usesIntermediaries,
        List<ServiceReference> servedWRPServices
    ) {
        this.serviceTradeName = serviceTradeName;
        this.walletRelyingParty = walletRelyingParty;

        if (relyingPartyInstances != null) {
            setEntitlements(relyingPartyInstances.stream()
                .flatMap(instance -> instance.getRelyingPartyEntitlements().stream())
                .map(RelyingPartyEntitlement::getEntitlement).distinct().toList());
        }

        setRelyingPartyInstances(relyingPartyInstances);
        setIntermediaryConfiguration(isIntermediary, usesIntermediaries, servedWRPServices);
    }

    public void setIntermediaryConfiguration(
        boolean isIntermediary,
        List<ServiceReference> usesIntermediaries,
        List<ServiceReference> servedWRPServices
    ) {
        List<ServiceReference> usesIntermediariesCopy =
            usesIntermediaries == null ? new ArrayList<>() : new ArrayList<>(usesIntermediaries);
        List<ServiceReference> servedWRPServicesCopy =
            servedWRPServices == null ? new ArrayList<>() : new ArrayList<>(servedWRPServices);

        validateIntermediaryConfiguration(isIntermediary, usesIntermediariesCopy, servedWRPServicesCopy);

        this.isIntermediary = isIntermediary;
        this.usesIntermediaries = usesIntermediariesCopy;
        this.servedWRPServices = servedWRPServicesCopy;
    }

    private static void validateIntermediaryConfiguration(
        boolean isIntermediary,
        List<ServiceReference> usesIntermediaries,
        List<ServiceReference> servedWRPServices
    ) {
        if (isIntermediary && !usesIntermediaries.isEmpty()) {
            throw new IllegalArgumentException("An intermediary service cannot use intermediaries");
        }
        if (!isIntermediary && !servedWRPServices.isEmpty()) {
            throw new IllegalArgumentException("Only an intermediary service can serve WRP services");
        }
    }

    public List<ServiceReference> getUsesIntermediaries() {
        return List.copyOf(usesIntermediaries);
    }

    public boolean isIntermediary() {
        return isIntermediary;
    }

    public List<ServiceReference> getServedWRPServices() {
        return List.copyOf(servedWRPServices);
    }

    public void setIntendedUses(List<IntendedUse> intendedUses) {
        List<IntendedUse> intendedUsesCopy = intendedUses == null ? List.of() : new ArrayList<>(intendedUses);
        this.intendedUses.clear();
        intendedUsesCopy.forEach(intendedUse -> intendedUse.setWalletRelyingPartyService(this));
        this.intendedUses.addAll(intendedUsesCopy);
    }

    public List<ProvidedAttestation> getProvidesAttestations() {
        return List.copyOf(providesAttestations);
    }

    public void setProvidesAttestations(List<ProvidedAttestation> providesAttestations) {
        List<ProvidedAttestation> providedAttestationsCopy =
            providesAttestations == null ? List.of() : new ArrayList<>(providesAttestations);
        this.providesAttestations.clear();
        providedAttestationsCopy.forEach(
            providedAttestation -> providedAttestation.setWalletRelyingPartyService(this));
        this.providesAttestations.addAll(providedAttestationsCopy);
    }

    public void setRelyingPartyInstances(List<RelyingPartyInstance> relyingPartyInstances) {
        List<RelyingPartyInstance> relyingPartyInstancesCopy = relyingPartyInstances == null ? List.of() : new ArrayList<>(relyingPartyInstances);
        relyingPartyInstancesCopy.forEach(instance -> instance.getRelyingPartyEntitlements()
            .forEach(selection -> requireServiceEntitlement(selection.getEntitlement())));

        this.relyingPartyInstances.clear();
        relyingPartyInstancesCopy.forEach(instance -> instance.setWalletRelyingPartyService(this));
        this.relyingPartyInstances.addAll(relyingPartyInstancesCopy);
    }

    public String getCredentialIssuerUrl(String entitlement) {
        return requireServiceEntitlement(entitlement).getCredentialIssuerUrl();
    }

    public void setCredentialIssuerUrl(String entitlement, String credentialIssuerUrl) {
        requireServiceEntitlement(entitlement).setCredentialIssuerUrl(credentialIssuerUrl);
    }

    @Transient
    public List<String> getEntitlements() {
        return serviceEntitlements.stream().map(ServiceEntitlement::getEntitlement).toList();
    }

    public ServiceEntitlement requireServiceEntitlement(String entitlement) {
        return serviceEntitlements.stream()
            .filter(role -> role.getEntitlement().equals(entitlement))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("The role is not on the service: " + entitlement));
    }

    public void setEntitlements(List<String> entitlements) {
        if (entitlements.stream().distinct().count() != entitlements.size()) {
            throw new BadRequestException("The service cannot contain the same role more than once");
        }

        boolean removesSelectedRole = relyingPartyInstances.stream()
            .flatMap(instance -> instance.getRelyingPartyEntitlements().stream())
            .anyMatch(selection -> !entitlements.contains(selection.getEntitlement()));

        if (removesSelectedRole) {
            throw new BadRequestException("A service role with instance references cannot be removed");
        }

        serviceEntitlements.removeIf(role -> !entitlements.contains(role.getEntitlement()));

        for (String entitlement : entitlements) {
            if (getEntitlements().contains(entitlement)) {
                continue;
            }

            ServiceEntitlement role = new ServiceEntitlement(entitlement, null);
            role.setWalletRelyingPartyService(this);
            serviceEntitlements.add(role);
        }
    }

    // for JPA instantiation.
    protected WalletRelyingPartyService() { }

    public void rename(String newServiceTradeName) {
        if (!Objects.equals(serviceTradeName, newServiceTradeName)) {
            relyingPartyInstances.forEach(RelyingPartyInstance::setUpdated);
        }

        this.serviceTradeName = newServiceTradeName;
    }

    public void setUpdated() {
        this.lastUpdatedMs = Instant.now().toEpochMilli();
    }

    public void suspend() {
        if (STATUS_SUSPENDED.equals(status)) {
            return;
        }
        if (STATUS_REVOKED.equals(status)) {
            throw new BadRequestException("A revoked service cannot be suspended");
        }

        this.status = STATUS_SUSPENDED;
        this.suspendedAt = currentDate();
        this.active = false;
    }

    public void reactivate() {
        if (STATUS_ACTIVE.equals(status)) {
            return;
        }
        if (STATUS_REVOKED.equals(status)) {
            throw new BadRequestException("A revoked service cannot be reactivated");
        }

        this.status = STATUS_ACTIVE;
        this.suspendedAt = null;
        this.active = true;
    }

    public void revoke() {
        if (STATUS_REVOKED.equals(status)) {
            return;
        }

        this.status = STATUS_REVOKED;
        this.revokedAt = currentDate();
        this.active = false;
    }

    public void setActive(boolean active) {
        if (active) {
            reactivate();
        } else {
            suspend();
        }
    }

    private static LocalDate currentDate() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    @PrePersist
    protected void onPrePersist() {
        validateIntermediaryConfiguration(isIntermediary, usesIntermediaries, servedWRPServices);
        Instant now = Instant.now();
        this.createdAt = LocalDate.ofInstant(now, ZoneOffset.UTC);
        this.lastUpdatedMs = this.createdMs = now.toEpochMilli();
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
