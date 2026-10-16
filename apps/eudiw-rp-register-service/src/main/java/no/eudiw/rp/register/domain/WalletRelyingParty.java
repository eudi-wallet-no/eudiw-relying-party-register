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
import java.util.List;

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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "identifier", columnDefinition = "json", nullable = false)
    private List<Identifier> identifier = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy", columnDefinition = "json", nullable = false)
    private List<Policy> policy = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "wallet_relying_party_x5c",
        joinColumns = @JoinColumn(name = "wallet_relying_party_id"))
    @Column(name = "certificate_chain", nullable = false)
    @OrderColumn(name = "position")
    private List<String> x5c = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "postal_address", columnDefinition = "json", nullable = false)
    private List<String> postalAddress = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "email", columnDefinition = "json", nullable = false)
    private List<String> email = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "phone", columnDefinition = "json", nullable = false)
    private List<String> phone = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "info_uri", columnDefinition = "json", nullable = false)
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
        fetch = FetchType.LAZY,
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

    public String getName() {
        if (legalPerson == null || legalPerson.getLegalName().isEmpty()) {
            return tradeName;
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
