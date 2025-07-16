package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import no.eudiw.rp.register.data.entitlement.Entitlement;
import no.eudiw.rp.register.data.entitlement.EntitlementAttributeConverter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "relying_party_entitlement")
public class RelyingPartyEntitlement extends BaseEntity {

    @Column(name = "entitlement", nullable = false)
    @Convert(converter = EntitlementAttributeConverter.class)
    private Entitlement entitlement;

    @ManyToOne
    @JoinColumn(name = "relying_party_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingParty relyingParty;

    public RelyingPartyEntitlement(Entitlement entitlement) {
        this(entitlement, null);
    }

    public RelyingPartyEntitlement(Entitlement entitlement,
                                   RelyingParty relyingParty) {
        this.id = null;
        this.entitlement = entitlement;
        this.relyingParty = relyingParty;
    }

    // for JPA instantiation.
    protected RelyingPartyEntitlement() { }
}
