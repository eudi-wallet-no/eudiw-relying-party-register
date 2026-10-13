package no.eudiw.rp.register.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "supervisory_authority")
public class SupervisoryAuthority extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "wallet_relying_party_id", nullable = false, unique = true)
    private WalletRelyingParty walletRelyingParty;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country", nullable = false)
    private String country;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "supervisory_authority_emails",
        joinColumns = @JoinColumn(name = "supervisory_authority_id"))
    @Column(name = "email", nullable = false)
    @OrderColumn(name = "position")
    private List<String> email = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "supervisory_authority_phones",
        joinColumns = @JoinColumn(name = "supervisory_authority_id"))
    @Column(name = "phone", nullable = false)
    @OrderColumn(name = "position")
    private List<String> phone = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "supervisory_authority_form_uris",
        joinColumns = @JoinColumn(name = "supervisory_authority_id"))
    @Column(name = "form_uri", nullable = false)
    @OrderColumn(name = "position")
    private List<String> formURI = new ArrayList<>();

    public SupervisoryAuthority(
        String name,
        String country,
        List<String> email,
        List<String> phone,
        List<String> formURI
    ) {
        this.name = name;
        this.country = country;
        this.email = email == null ? new ArrayList<>() : new ArrayList<>(email);
        this.phone = phone == null ? new ArrayList<>() : new ArrayList<>(phone);
        this.formURI = formURI == null ? new ArrayList<>() : new ArrayList<>(formURI);
    }

    protected SupervisoryAuthority() { }
}
