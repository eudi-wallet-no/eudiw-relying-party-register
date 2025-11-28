package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@ToString
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

    public void addRelyingPartyInstance(RelyingPartyInstance relyingPartyInstance) {
        boolean instanceNotAlreadyExists =
            relyingPartyInstance.id == null ||
                this.relyingPartyInstances
                .stream()
                .map(RelyingPartyInstance::getId)
                .noneMatch(e -> Objects.equals(e, relyingPartyInstance.getId()));

        if (instanceNotAlreadyExists) {
            relyingPartyInstance.setLegalEntity(this);
            relyingPartyInstances.add(relyingPartyInstance);
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
        this.relyingPartyInstances = relyingPartyInstances;
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
