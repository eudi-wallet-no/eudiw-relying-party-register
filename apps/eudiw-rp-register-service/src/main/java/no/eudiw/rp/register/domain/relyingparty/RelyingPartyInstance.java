package no.eudiw.rp.register.domain.relyingparty;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;
import no.eudiw.rp.register.domain.BaseEntity;
import no.eudiw.rp.register.domain.LegalEntity;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
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

    @ManyToOne(cascade = CascadeType.PERSIST)
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

    public List<IssuerCertificate> getIssuerCertificates() {
        return this.getRelyingPartyEntitlements()
                   .stream()
                   .flatMap(entitlement -> entitlement.getIssuerCertificates().stream())
                   .toList();
    }

    public Optional<RelyingPartyEntitlement> getRelyingPartyEntitlement(String entitlementType) {
        return relyingPartyEntitlements.stream()
                .filter(entitlement -> entitlementType.equals(entitlement.getEntitlement()))
                .findFirst();
    }

    private void addOrUpdateRelyingPartyEntitlement(RelyingPartyEntitlement newEntitlement) {
        Optional<RelyingPartyEntitlement> existingEntitlement =
            this.getRelyingPartyEntitlement(newEntitlement.getEntitlement());
        if (existingEntitlement.isPresent()) {
            existingEntitlement.get().setCredentialIssuerUrl(newEntitlement.getCredentialIssuerUrl());
        } else {
            this.relyingPartyEntitlements.add(newEntitlement);
            newEntitlement.setRelyingPartyInstance(this);
        }
    }

    public void setRelyingPartyEntitlements(List<RelyingPartyEntitlement> relyingPartyEntitlements) {
        List<String> incomingEntitlementValues =
            relyingPartyEntitlements.stream()
                                    .map(RelyingPartyEntitlement::getEntitlement)
                                    .toList();

        Predicate<RelyingPartyEntitlement> doRemoveEntitlement =
            entitlement -> entitlement.getIssuerCertificates().isEmpty()
                               && !incomingEntitlementValues.contains(entitlement.getEntitlement());
        this.relyingPartyEntitlements.removeIf(doRemoveEntitlement);

        relyingPartyEntitlements.forEach(this::addOrUpdateRelyingPartyEntitlement);
    }

    public void setRelyingPartyEaas(List<RelyingPartyEaa> relyingPartyEaas) {
        this.relyingPartyEaas.clear();
        if (relyingPartyEaas != null) {
            relyingPartyEaas.forEach(eaa -> eaa.setRelyingPartyInstance(this));
            this.relyingPartyEaas.addAll(relyingPartyEaas);
        }
    }

    public void setAccessCertificates(List<AccessCertificate> accessCertificates) {
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
        List<AccessCertificate> accessCertificates) {
        this.tradeName = tradeName;
        this.setRelyingPartyEntitlements(relyingPartyEntitlements);
        this.setRelyingPartyEaas(relyingPartyEaas);
        this.setAccessCertificates(accessCertificates);
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
