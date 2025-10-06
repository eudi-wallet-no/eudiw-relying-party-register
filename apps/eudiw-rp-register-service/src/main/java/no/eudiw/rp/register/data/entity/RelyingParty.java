package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private Boolean publicSector;

    // TODO: Jira EUW-24 (https://digdir.atlassian.net/browse/EUW-24)
    @Setter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "relyingParty",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<RelyingPartyEntitlement> relyingPartyEntitlements =
        new ArrayList<>();

    // TODO: Jira EUW-24 (https://digdir.atlassian.net/browse/EUW-24)
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
    private List<AccessCertificate> accessCertificates =
        new ArrayList<>();

    @Column(name = "created_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long createdMs;

    @Column(name = "last_updated_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long lastUpdatedMs;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    public void setRelyingPartyEntitlements(
        List<RelyingPartyEntitlement> relyingPartyEntitlements
    ) {
        this.relyingPartyEntitlements.clear();
        if (relyingPartyEntitlements != null) {
            relyingPartyEntitlements.forEach(entitlement -> entitlement.setRelyingParty(this));
            this.relyingPartyEntitlements.addAll(relyingPartyEntitlements);
        }
    }

    public void addRelyingPartyEntitlement(
        RelyingPartyEntitlement relyingPartyEntitlement
    ) {
        if (relyingPartyEntitlements != null) {
            this.relyingPartyEntitlements.add(relyingPartyEntitlement);
        }
    }

    public Optional<RelyingPartyEntitlement> getRelyingPartyEntitlement(UUID id) {
        return relyingPartyEntitlements.stream()
            .filter(entitlement -> id.equals(entitlement.getId()))
            .findFirst();
    }

    public Optional<RelyingPartyEntitlement> getRelyingPartyEntitlement(String entitlementType) {
        return relyingPartyEntitlements.stream()
                .filter(entitlement -> entitlementType.equals(entitlement.getEntitlement()))
                .findFirst();
    }

    public void setRelyingPartyEaas(List<RelyingPartyEaa> relyingPartyEaas) {
        this.relyingPartyEaas.clear();
        if (relyingPartyEaas != null) {
            relyingPartyEaas.forEach(eaa -> eaa.setRelyingParty(this));
            this.relyingPartyEaas.addAll(relyingPartyEaas);
        }
    }

    public void addRelyingPartyCertificate(
        AccessCertificate accessCertificate) {
        accessCertificate.setRelyingParty(this);
        this.accessCertificates.add(accessCertificate);
    }

    public void setAccessCertificates(
        List<AccessCertificate> accessCertificates) {
        this.accessCertificates.clear();
        if (accessCertificates != null) {
            accessCertificates.forEach(this::addRelyingPartyCertificate);
        }
    }

    public RelyingParty(String name, String orgno, Boolean publicSector) {
        this(name, orgno, publicSector, null, null, null);
    }

    public RelyingParty(String name,
                        String orgno,
                        Boolean publicSector,
                        List<RelyingPartyEntitlement> relyingPartyEntitlements,
                        List<RelyingPartyEaa> relyingPartyEaas,
                        List<AccessCertificate> accessCertificates
                        ) {
        this.id = null;
        this.name = name;
        this.orgno = orgno;
        this.publicSector = publicSector;
        this.setRelyingPartyEntitlements(relyingPartyEntitlements);
        this.setRelyingPartyEaas(relyingPartyEaas);
        this.setAccessCertificates(accessCertificates);
        this.active = true;
        this.deleted = false;
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
