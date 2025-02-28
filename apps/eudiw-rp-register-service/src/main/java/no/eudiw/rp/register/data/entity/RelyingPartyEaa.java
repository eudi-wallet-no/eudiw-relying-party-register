package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "relying_party_eaa")
public class RelyingPartyEaa {

    @Id
    // TODO: Jira EUW-23 (https://digdir.atlassian.net/browse/EUW-23)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "namespace", nullable = false)
    private String namespace;

    @Column(name = "intent", nullable = false)
    private String intent;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "relying_party_uuid", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private RelyingParty relyingParty;

    // TODO: Jira EUW-24 (https://digdir.atlassian.net/browse/EUW-24)
    @OneToMany(mappedBy = "relyingPartyEaa",
               fetch = FetchType.EAGER,
               cascade = CascadeType.ALL)
    private List<RelyingPartyEaaAttribute> relyingPartyEaaAttributes = new ArrayList<>();

    public void addRelyingPartyEaaAttribute(RelyingPartyEaaAttribute relyingPartyEaaAttribute) {
        relyingPartyEaaAttribute.setRelyingPartyEaa(this);
        this.relyingPartyEaaAttributes.add(relyingPartyEaaAttribute);
    }
    public void setRelyingPartyEaaAttributes(List<RelyingPartyEaaAttribute> relyingPartyEaaAttributes) {
        this.relyingPartyEaaAttributes.clear();
        if (relyingPartyEaaAttributes != null) {
            relyingPartyEaaAttributes.forEach(this::addRelyingPartyEaaAttribute);
        }
    }

    public RelyingPartyEaa(String namespace, String intent) {
        this(namespace, intent, null);
    }
    public RelyingPartyEaa(String namespace, String intent, RelyingParty relyingParty) {
        this.id = null;
        this.namespace = namespace;
        this.intent = intent;
        this.relyingParty = relyingParty;
    }

    public RelyingPartyEaa(RelyingPartyEaa relyingPartyEaa) {
        this(relyingPartyEaa.namespace,
             relyingPartyEaa.intent,
             relyingPartyEaa.relyingParty);
        this.id = relyingPartyEaa.id;
    }

    // for JPA instantiation.
    protected RelyingPartyEaa() { }

    public String toString() {
        var relyingPartyStr = relyingParty.getId() != null
                                  ? relyingParty.getId()
                                  : relyingParty;
        return "relyingPartyEaaAttribute(id=" + id +
                   ", relyingParty=" + relyingPartyStr +
                   ", attributes=" + relyingPartyEaaAttributes +
                   ", intent=" + intent +
                   ")";
    }
}
