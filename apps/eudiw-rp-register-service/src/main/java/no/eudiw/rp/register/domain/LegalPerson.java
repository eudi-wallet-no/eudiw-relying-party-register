package no.eudiw.rp.register.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "legal_person_legal_names",
        joinColumns = @JoinColumn(name = "legal_person_id"))
    @Column(name = "legal_name", nullable = false)
    @OrderColumn(name = "position")
    private List<String> legalName = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "legal_person_established_by_law",
        joinColumns = @JoinColumn(name = "legal_person_id"))
    @AttributeOverrides({
        @AttributeOverride(name = "legislativeIdentifier",
            column = @Column(name = "legislative_identifier", nullable = false)),
        @AttributeOverride(name = "legalBasis", column = @Column(name = "legal_basis"))
    })
    @OrderColumn(name = "position")
    private List<Law> establishedByLaw = new ArrayList<>();

    public LegalPerson(List<String> legalName, List<Law> establishedByLaw) {
        this.legalName = legalName == null ? new ArrayList<>() : new ArrayList<>(legalName);
        this.establishedByLaw = establishedByLaw == null ? new ArrayList<>() : new ArrayList<>(establishedByLaw);
    }

    protected LegalPerson() { }
}
