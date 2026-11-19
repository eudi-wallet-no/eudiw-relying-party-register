package no.eudiw.rp.register.domain;

import java.util.List;

public record ProvidedAttestation(String format, String type, List<TrustAuthority> trustedAuthorities) {
    public ProvidedAttestation {
        trustedAuthorities = trustedAuthorities == null ? List.of() : List.copyOf(trustedAuthorities);
    }

    public ProvidedAttestation(String format, String type) {
        this(format, type, List.of());
    }
}
