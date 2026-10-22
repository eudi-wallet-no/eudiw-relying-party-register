package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;

public record RelyingPartyEaaResource(
    @NotBlank String namespace,
    @NotBlank String intent
) {
    public RelyingPartyEaa toDomain() {
        return new RelyingPartyEaa(namespace, intent);
    }
}
