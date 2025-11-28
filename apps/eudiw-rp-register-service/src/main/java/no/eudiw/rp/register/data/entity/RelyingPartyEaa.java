package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "relying_party_eaa")
public class RelyingPartyEaa extends BaseEntity {

    @Column(name = "namespace", nullable = false)
    private String namespace;

    @Column(name = "intent", nullable = false)
    private String intent;

    @ManyToOne
    @JoinColumn(name = "relying_party_instance_id",
        columnDefinition = "UUID",
        nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private RelyingPartyInstance relyingPartyInstance;

    public RelyingPartyEaa(String namespace, String intent) {
        this.namespace = namespace;
        this.intent = intent;
    }

    // for JPA instantiation.
    protected RelyingPartyEaa() { }
}
