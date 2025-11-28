package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@ToString
@Table(name = "relying_party_instance")
public class RelyingPartyInstance extends BaseEntity {

    @Column(name = "trade_name", nullable = false)
    private String tradeName;

    @Setter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "relyingPartyInstance",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<RelyingPartyEntitlement> relyingPartyEntitlements = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "relyingPartyInstance",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<RelyingPartyEaa> relyingPartyEaas = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "relyingPartyInstance",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<AccessCertificate> accessCertificates = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "legal_entity_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private LegalEntity legalEntity;

    @Column(name = "created_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long createdMs;

    @Column(name = "last_updated_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long lastUpdatedMs;

    @Column(name = "active", nullable = false)
    private boolean active = true;

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
            relyingPartyEntitlement.setRelyingPartyInstance(this);
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
            relyingPartyEaas.forEach(eaa -> eaa.setRelyingPartyInstance(this));
            this.relyingPartyEaas.addAll(relyingPartyEaas);
        }
    }

    public void setAccessCertificates(
        List<AccessCertificate> accessCertificates) {
        this.accessCertificates.clear();
        if (accessCertificates != null) {
            accessCertificates.forEach(cert -> cert.setRelyingPartyInstance(this));
            this.accessCertificates.addAll(accessCertificates);
        }
    }

    public RelyingPartyInstance(
        String tradeName,
        List<RelyingPartyEntitlement> relyingPartyEntitlements,
        List<RelyingPartyEaa> relyingPartyEaas,
        List<AccessCertificate> accessCertificates
    ) {
        this.tradeName = tradeName;
        this.setRelyingPartyEntitlements(relyingPartyEntitlements);
        this.setRelyingPartyEaas(relyingPartyEaas);
        this.setAccessCertificates(accessCertificates);
    }

    public RelyingPartyInstance(
        String tradeName,
        List<RelyingPartyEntitlement> relyingPartyEntitlements,
        List<RelyingPartyEaa> relyingPartyEaas,
        List<AccessCertificate> accessCertificates,
        LegalEntity legalEntity
    ) {
        this.tradeName = tradeName;
        this.setRelyingPartyEntitlements(relyingPartyEntitlements);
        this.setRelyingPartyEaas(relyingPartyEaas);
        this.setAccessCertificates(accessCertificates);
        this.legalEntity = legalEntity;
    }

    // for JPA instantiation.
    protected RelyingPartyInstance() { }

    @PrePersist
    protected void onPrePersist() {
        this.lastUpdatedMs = this.createdMs = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.lastUpdatedMs = Instant.now().toEpochMilli();
    }
}
