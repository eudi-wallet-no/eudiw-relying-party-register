package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@ToString
@Entity
@Table(name = "relying_party")
public class RelyingParty {

    @Id
    // TODO: Jira EUW-23 (https://digdir.atlassian.net/browse/EUW-23)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    @JdbcTypeCode(SqlTypes.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(name = "orgno", unique = true, nullable = false)
    private String orgno;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "public_sector", nullable = false)
    private Boolean publicSector;

    // TODO: Jira EUW-24 (https://digdir.atlassian.net/browse/EUW-24)
    @OneToMany(mappedBy = "relyingParty",
               fetch = FetchType.EAGER,
               cascade = CascadeType.ALL)
    private List<RelyingPartyEntitlement> relyingPartyEntitlements =
        new ArrayList<>();

    // TODO: Jira EUW-24 (https://digdir.atlassian.net/browse/EUW-24)
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "relyingParty",
               fetch = FetchType.EAGER,
               cascade = CascadeType.ALL)
    private List<RelyingPartyEaa> relyingPartyEaas = new ArrayList<>();

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

    public void addRelyingPartyEntitlement(RelyingPartyEntitlement relyingPartyEntitlement) {
        relyingPartyEntitlement.setRelyingParty(this);
        this.relyingPartyEntitlements.add(relyingPartyEntitlement);
    }

    public void setRelyingPartyEntitlements(
        List<RelyingPartyEntitlement> relyingPartyEntitlements) {
        this.relyingPartyEntitlements.clear();
        if (relyingPartyEntitlements != null)
            relyingPartyEntitlements.forEach(this::addRelyingPartyEntitlement);
    }

    public void addRelyingPartyEaa(RelyingPartyEaa relyingPartyEaa) {
        relyingPartyEaa.setRelyingParty(this);
        this.relyingPartyEaas.add(relyingPartyEaa);
    }
    public void setRelyingPartyEaas(List<RelyingPartyEaa> relyingPartyEaas) {
        this.relyingPartyEaas.clear();
        if (relyingPartyEaas != null)
            relyingPartyEaas.forEach(this::addRelyingPartyEaa);
    }

    public RelyingParty(String name, String orgno, Boolean publicSector) {
        this(name, orgno, publicSector, null);
    }

    public RelyingParty(String name,
                        String orgno,
                        Boolean publicSector,
                        List<RelyingPartyEntitlement> relyingPartyEntitlements) {
        this.id = null;
        this.name = name;
        this.orgno = orgno;
        this.publicSector = publicSector;
        this.setRelyingPartyEntitlements(relyingPartyEntitlements);
        this.active = true;
        this.deleted = false;
    }

    public RelyingParty(RelyingParty relyingParty) {
        this(relyingParty.name,
             relyingParty.orgno,
             relyingParty.publicSector,
             relyingParty.relyingPartyEntitlements);
        this.id = relyingParty.id;
        this.createdMs = relyingParty.createdMs;
        this.lastUpdatedMs = relyingParty.lastUpdatedMs;
        this.active = relyingParty.active;
        this.deleted = relyingParty.deleted;
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
