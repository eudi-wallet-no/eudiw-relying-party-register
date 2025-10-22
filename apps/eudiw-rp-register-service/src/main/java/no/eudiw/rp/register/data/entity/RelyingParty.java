package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;
import lombok.ToString;

@Getter
@Setter
@Entity
@ToString
@Table(name = "relying_party")
public class RelyingParty extends BaseEntity {

    @Column(name = "orgno", nullable = false)
    private String orgno;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "public_sector", nullable = false)
    private boolean publicSector;

    @Setter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "relyingParty",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<RelyingPartyEntitlement> relyingPartyEntitlements = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "relyingParty",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<RelyingPartyEaa> relyingPartyEaas = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "relyingParty",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<AccessCertificate> accessCertificates = new ArrayList<>();

    @Column(name = "created_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long createdMs;

    @Column(name = "last_updated_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long lastUpdatedMs;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    public Optional<RelyingPartyEntitlement> getRelyingPartyEntitlement(String entitlementType) {
        return relyingPartyEntitlements.stream()
                .filter(entitlement -> entitlementType.equals(entitlement.getEntitlement()))
                .findFirst();
    }

    private void addRelyingPartyEntitlement(RelyingPartyEntitlement relyingPartyEntitlement) {
        boolean entitlementNotAlreadyExists =
            this.relyingPartyEntitlements
                .stream()
                .map(RelyingPartyEntitlement::getEntitlement)
                .noneMatch(e -> Objects.equals(e, relyingPartyEntitlement.getEntitlement()));

        if (entitlementNotAlreadyExists) {
            relyingPartyEntitlement.setRelyingParty(this);
            this.relyingPartyEntitlements.add(relyingPartyEntitlement);
        }
    }

    public void setRelyingPartyEntitlements(List<RelyingPartyEntitlement> relyingPartyEntitlements) {
        if (relyingPartyEntitlements != null) {
            this.removeEntitlementsWithoutIssuerCertificates(relyingPartyEntitlements);
            relyingPartyEntitlements.forEach(this::addRelyingPartyEntitlement);
        }
    }

    private void removeEntitlementsWithoutIssuerCertificates(List<RelyingPartyEntitlement> input) {
        Set<String> incoming = input.stream().map(RelyingPartyEntitlement::getEntitlement).collect(Collectors.toSet());
        this.relyingPartyEntitlements.removeIf(entitlement ->
            !incoming.contains(entitlement.getEntitlement()) && entitlement.getIssuerCertificates().isEmpty()
        );
    }

    public void setRelyingPartyEaas(List<RelyingPartyEaa> relyingPartyEaas) {
        this.relyingPartyEaas.clear();
        if (relyingPartyEaas != null) {
            relyingPartyEaas.forEach(eaa -> eaa.setRelyingParty(this));
            this.relyingPartyEaas.addAll(relyingPartyEaas);
        }
    }

    public void setAccessCertificates(
        List<AccessCertificate> accessCertificates) {
        this.accessCertificates.clear();
        if (accessCertificates != null) {
            accessCertificates.forEach(cert -> cert.setRelyingParty(this));
            this.accessCertificates.addAll(accessCertificates);
        }
    }

    public RelyingParty(String name,
                        String orgno,
                        boolean publicSector,
                        List<RelyingPartyEntitlement> relyingPartyEntitlements,
                        List<RelyingPartyEaa> relyingPartyEaas,
                        List<AccessCertificate> accessCertificates) {
        this.name = name;
        this.orgno = orgno;
        this.publicSector = publicSector;
        this.setRelyingPartyEntitlements(relyingPartyEntitlements);
        this.setRelyingPartyEaas(relyingPartyEaas);
        this.setAccessCertificates(accessCertificates);
    }

    // for JPA instantiation.
    protected RelyingParty() { }

    @PrePersist
    protected void onPrePersist() {
        this.lastUpdatedMs = this.createdMs = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.lastUpdatedMs = Instant.now().toEpochMilli();
    }
}
