package no.eudiw.rp.register.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "legal_person")
public class LegalPerson extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "wallet_relying_party_id", nullable = false, unique = true)
    private WalletRelyingParty walletRelyingParty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "legal_name", columnDefinition = "json", nullable = false)
    private List<String> legalName = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "legal_person_established_by_law",
        joinColumns = @JoinColumn(name = "legal_person_id"))
    @OrderColumn(name = "position")
    private List<Law> establishedByLaw = new ArrayList<>();

    public LegalPerson(List<String> legalName, List<Law> establishedByLaw) {
        this.legalName = legalName == null ? new ArrayList<>() : new ArrayList<>(legalName);
        this.establishedByLaw = establishedByLaw == null ? new ArrayList<>() : new ArrayList<>(establishedByLaw);
    }

    protected LegalPerson() { }
}
