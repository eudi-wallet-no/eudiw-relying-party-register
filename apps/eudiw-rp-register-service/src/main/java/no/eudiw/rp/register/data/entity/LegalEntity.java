package no.eudiw.rp.register.data.entity;

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
@Table(name = "legal_entity")
public class LegalEntity extends BaseEntity {

    @Column(name = "orgno", nullable = false)
    private String orgno;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "public_sector", nullable = false)
    private boolean publicSector;

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
        mappedBy = "legalEntity",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<RelyingPartyInstance> relyingPartyInstances = new ArrayList<>();

    public void setRelyingPartyInstances(List<RelyingPartyInstance> relyingPartyInstances) {
        this.relyingPartyInstances.clear();
        if (relyingPartyInstances != null) {
            relyingPartyInstances.forEach(rpi -> rpi.setLegalEntity(this));
            this.relyingPartyInstances.addAll(relyingPartyInstances);
        }
    }

    public LegalEntity(
        String name,
        String orgno,
        boolean publicSector,
        List<RelyingPartyInstance> relyingPartyInstances
    ) {
        this.name = name;
        this.orgno = orgno;
        this.publicSector = publicSector;
        this.setRelyingPartyInstances(relyingPartyInstances);
    }

    // for JPA instantiation.
    protected LegalEntity() { }

    @PrePersist
    protected void onPrePersist() {
        this.lastUpdatedMs = this.createdMs = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.lastUpdatedMs = Instant.now().toEpochMilli();
    }
}
