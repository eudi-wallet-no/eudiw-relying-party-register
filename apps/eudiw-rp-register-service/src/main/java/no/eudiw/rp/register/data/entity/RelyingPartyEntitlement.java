package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "relying_party_entitlement")
public class RelyingPartyEntitlement {

    @Id
    // TODO: Jira EUW-23 (https://digdir.atlassian.net/browse/EUW-23)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(name = "entitlement", nullable = false)
    private String entitlement;

    @ManyToOne
    @JoinColumn(name = "relying_party_uuid", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private RelyingParty relyingParty;

    public RelyingPartyEntitlement(String entitlement) {
        this(entitlement, null);
    }

    public RelyingPartyEntitlement(String entitlement, RelyingParty relyingParty) {
        this.id = null;
        this.relyingParty = relyingParty;
        this.entitlement = entitlement;
    }

    public RelyingPartyEntitlement(RelyingPartyEntitlement relyingPartyEntitlement) {
        this(relyingPartyEntitlement.entitlement,
             relyingPartyEntitlement.relyingParty);
        this.id = relyingPartyEntitlement.id;
    }

    // for JPA instantiation.
    protected RelyingPartyEntitlement() { }

    public String toString() {
        String relyingPartyStr =
            relyingParty != null ? relyingParty.getId().toString() : "<no RP>";
        return "RelyingPartyEntitlement(" +
                   "\n  id=" + id +
                   "\n  entitlement='" + entitlement +
                   "',\n  relyingParty=" + relyingPartyStr +
                   "\n)";
    }
}
