package no.eudiw.rp.register.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "wallet_relying_party_service_provided_attestation")
public class ProvidedAttestation extends BaseEntity {

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "wallet_relying_party_service_id",
        columnDefinition = "UUID",
        nullable = false,
        updatable = false
    )
    @JdbcTypeCode(SqlTypes.UUID)
    private WalletRelyingPartyService walletRelyingPartyService;

    @Column(name = "format", columnDefinition = "TEXT")
    private String format;

    @Column(name = "type", columnDefinition = "TEXT")
    private String type;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trusted_authorities", columnDefinition = "JSON", nullable = false)
    private List<TrustAuthority> trustedAuthorities = new ArrayList<>();

    public ProvidedAttestation(String format, String type, List<TrustAuthority> trustedAuthorities) {
        this.format = format;
        this.type = type;
        setTrustedAuthorities(trustedAuthorities);
    }

    public ProvidedAttestation(String format, String type) {
        this(format, type, List.of());
    }

    public String format() {
        return format;
    }

    public String type() {
        return type;
    }

    public List<TrustAuthority> trustedAuthorities() {
        return List.copyOf(trustedAuthorities);
    }

    public void setTrustedAuthorities(List<TrustAuthority> trustedAuthorities) {
        this.trustedAuthorities = trustedAuthorities == null
            ? new ArrayList<>()
            : new ArrayList<>(trustedAuthorities);
    }

    protected ProvidedAttestation() { }
}
