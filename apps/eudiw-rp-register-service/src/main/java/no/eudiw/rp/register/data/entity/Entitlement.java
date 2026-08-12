package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "entitlement")
public class Entitlement extends BaseEntity {

    @Column(name = "entitlement", nullable = false)
    private String entitlement;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "ca_id", nullable = false)
    private String caId;

    public Entitlement(
        String entitlement,
        boolean active,
        String displayName,
        String caId
    ) {
        this.id = null;
        this.entitlement = entitlement;
        this.active = active;
        this.displayName = displayName;
        this.caId = caId;
    }

    // for JPA instantiation.
    protected Entitlement() { }
}
