package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "relying_party_eaa_attribute")
public class RelyingPartyEaaAttribute {

    @Id
    // TODO: Jira EUW-23 (https://digdir.atlassian.net/browse/EUW-23)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "relying_party_eaa_uuid", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private RelyingPartyEaa relyingPartyEaa;

    @Column(name = "attribute", nullable = false)
    private String attribute;

    @Column(name = "intent", nullable = true)
    private String intent;


    public RelyingPartyEaaAttribute(String attribute) {
        this(attribute, null, null);
    }
    public RelyingPartyEaaAttribute(String attribute, String intent) {
        this(attribute, intent, null);
    }
    public RelyingPartyEaaAttribute(String attribute, RelyingPartyEaa relyingPartyEaa) {
        this(attribute, null, relyingPartyEaa);
    }

    public RelyingPartyEaaAttribute(String attribute,
                                    String intent,
                                    RelyingPartyEaa relyingPartyEaa) {
        this.attribute = attribute;
        this.intent = intent;
        this.relyingPartyEaa = relyingPartyEaa;
    }

    public RelyingPartyEaaAttribute(RelyingPartyEaaAttribute relyingPartyEaaAttribute) {
        this(relyingPartyEaaAttribute.attribute,
             relyingPartyEaaAttribute.intent,
             relyingPartyEaaAttribute.relyingPartyEaa);
        this.id = relyingPartyEaaAttribute.id;
    }

    // for JPA instantiation.
    protected RelyingPartyEaaAttribute() { }

    public String toString() {
        return "relyingPartyEaaAttribute(id=" + id +
                   ", relyingPartyEaa=" + relyingPartyEaa.getId() +
                   ", attribute=" + attribute +
                   ", intent=" + intent +
                   ")";
    }
}
