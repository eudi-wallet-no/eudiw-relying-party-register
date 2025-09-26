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

    public Entitlement(
        String entitlement,
        boolean active,
        String displayName
    ) {
        this.id = null;
        this.entitlement = entitlement;
        this.active = active;
        this.displayName = displayName;
    }

    // for JPA instantiation.
    protected Entitlement() { }
}
