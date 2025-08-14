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

    public Entitlement(
        String entitlement,
        boolean active
    ) {
        this.id = null;
        this.entitlement = entitlement;
        this.active = active;
    }

    // for JPA instantiation.
    protected Entitlement() { }
}
