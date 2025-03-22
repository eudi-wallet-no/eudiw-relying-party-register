package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "relying_party_eaa")
public class RelyingPartyEaa {

    @Id
    // TODO: Jira EUW-23 (https://digdir.atlassian.net/browse/EUW-23)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "namespace", nullable = false)
    private String namespace;

    @Column(name = "intent", nullable = false)
    private String intent;

    @ManyToOne
    @JoinColumn(name = "relying_party_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingParty relyingParty;

    public RelyingPartyEaa(String namespace, String intent) {
        this(namespace, intent, null);
    }
    public RelyingPartyEaa(String namespace, String intent, RelyingParty relyingParty) {
        this.id = null;
        this.namespace = namespace;
        this.intent = intent;
        this.relyingParty = relyingParty;
    }

    // for JPA instantiation.
    protected RelyingPartyEaa() { }
}
