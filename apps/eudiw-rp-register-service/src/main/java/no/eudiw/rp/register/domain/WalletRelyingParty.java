package no.eudiw.rp.register.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "wallet_relying_party")
public class WalletRelyingParty extends BaseEntity {

    @Column(name = "orgno", nullable = false)
    private String orgno;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "is_psb", nullable = false)
    private boolean isPsb;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "country")
    private String country;

    @Column(name = "provider_type")
    private String providerType;

    @Column(name = "registry_uri")
    private String registryURI;

    @Setter(AccessLevel.NONE)
    @OneToOne(mappedBy = "walletRelyingParty", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private LegalPerson legalPerson;

    @Setter(AccessLevel.NONE)
    @OneToOne(mappedBy = "walletRelyingParty", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private SupervisoryAuthority supervisoryAuthority;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_relying_party_identifiers",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @AttributeOverrides({
        @AttributeOverride(name = "type", column = @Column(name = "identifier_type", nullable = false)),
        @AttributeOverride(name = "identifier", column = @Column(name = "identifier_value", nullable = false))
    })
    @OrderColumn(name = "position")
    private List<Identifier> identifier = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_relying_party_policies",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @AttributeOverrides({
        @AttributeOverride(name = "type", column = @Column(name = "policy_type", nullable = false)),
        @AttributeOverride(name = "policyURI", column = @Column(name = "policy_uri", nullable = false))
    })
    @OrderColumn(name = "position")
    private List<Policy> policy = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_relying_party_x5c",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "certificate_chain", nullable = false)
    @OrderColumn(name = "position")
    private List<String> x5c = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_relying_party_postal_addresses",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "postal_address", nullable = false)
    @OrderColumn(name = "position")
    private List<String> postalAddress = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_relying_party_emails",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "email", nullable = false)
    @OrderColumn(name = "position")
    private List<String> email = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_relying_party_phones",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "phone", nullable = false)
    @OrderColumn(name = "position")
    private List<String> phone = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_relying_party_info_uris",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "info_uri", nullable = false)
    @OrderColumn(name = "position")
    private List<String> infoURI = new ArrayList<>();

    @Column(name = "created_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long createdMs;

    @Column(name = "last_updated_ms", nullable = false)
    @Setter(AccessLevel.NONE)
    private long lastUpdatedMs;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Setter(AccessLevel.NONE)
    @OneToMany(
        mappedBy = "walletRelyingParty",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<WalletRelyingPartyService> services = new ArrayList<>();

    public void setServices(List<WalletRelyingPartyService> services) {
        this.services.clear();
        if (services != null) {
            services.forEach(service -> service.setWalletRelyingParty(this));
            this.services.addAll(services);
        }
    }

    public void setLegalPerson(LegalPerson legalPerson) {
        if (this.legalPerson != null) {
            this.legalPerson.setWalletRelyingParty(null);
        }
        this.legalPerson = legalPerson;
        if (legalPerson != null) {
            legalPerson.setWalletRelyingParty(this);
        }
    }

    public void setSupervisoryAuthority(SupervisoryAuthority supervisoryAuthority) {
        if (this.supervisoryAuthority != null) {
            this.supervisoryAuthority.setWalletRelyingParty(null);
        }
        this.supervisoryAuthority = supervisoryAuthority;
        if (supervisoryAuthority != null) {
            supervisoryAuthority.setWalletRelyingParty(this);
        }
    }

    public WalletRelyingParty(
        String legalName,
        String orgno,
        boolean isPsb,
        List<WalletRelyingPartyService> services
    ) {
        this.legalName = legalName;
        this.orgno = orgno;
        this.isPsb = isPsb;
        this.setServices(services);
    }

    // for JPA instantiation.
    protected WalletRelyingParty() { }

    @PrePersist
    protected void onPrePersist() {
        this.lastUpdatedMs = this.createdMs = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.lastUpdatedMs = Instant.now().toEpochMilli();
    }
}
