package no.eudiw.rp.register.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "wallet_relying_party")
public class WalletRelyingParty extends BaseEntity {

    public static final String PROVIDER_TYPE = "WalletRelyingParty";

    @Column(name = "orgno", nullable = false)
    private String orgno;

    @Column(name = "is_psb", nullable = false)
    private boolean isPsb;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "country", nullable = false)
    private String country = "NO";

    @Column(name = "provider_type", nullable = false)
    @Setter(AccessLevel.NONE)
    private String providerType = PROVIDER_TYPE;

    @Setter(AccessLevel.NONE)
    @OneToOne(mappedBy = "walletRelyingParty", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private LegalPerson legalPerson;

    // Additional identifiers (e.g. EUID, LEI); orgno is the national unique identifier.
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "wallet_relying_party_identifier",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @OrderColumn(name = "position")
    private List<Identifier> identifier = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy", columnDefinition = "json", nullable = false)
    private Map<String, String> policy = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "wallet_relying_party_x5c",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "certificate_chain", nullable = false)
    @OrderColumn(name = "position")
    private List<String> x5c = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "wallet_relying_party_postal_address",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "postal_address", nullable = false)
    @OrderColumn(name = "position")
    private List<String> postalAddress = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "wallet_relying_party_email",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "email", nullable = false)
    @OrderColumn(name = "position")
    private List<String> email = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "wallet_relying_party_phone",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "phone", nullable = false)
    @OrderColumn(name = "position")
    private List<String> phone = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "wallet_relying_party_info_uri",
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

    /**
     * Creates a relying party whose trade name and legal name are both {@code name}.
     */
    public WalletRelyingParty(
        String name,
        String orgno,
        boolean isPsb,
        List<WalletRelyingPartyService> services
    ) {
        this.tradeName = name;
        this.orgno = orgno;
        this.isPsb = isPsb;
        this.setServices(services);
        this.setLegalPerson(new LegalPerson(List.of(name), List.of()));
    }

    /**
     * Sets both the trade name and the (single) legal name.
     */
    public void setName(String name) {
        this.tradeName = name;
        if (legalPerson == null) {
            setLegalPerson(new LegalPerson(List.of(name), List.of()));
        } else {
            legalPerson.getLegalName().clear();
            legalPerson.getLegalName().add(name);
        }
    }

    /**
     * The trade name, falling back to the first legal name when no trade name is registered.
     */
    public String getTradeNameOrLegalName() {
        if (tradeName != null) {
            return tradeName;
        }
        if (legalPerson == null || legalPerson.getLegalName().isEmpty()) {
            return null;
        }
        return legalPerson.getLegalName().getFirst();
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
