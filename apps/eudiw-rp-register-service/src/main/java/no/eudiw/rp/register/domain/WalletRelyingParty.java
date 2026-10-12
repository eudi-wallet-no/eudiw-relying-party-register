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
