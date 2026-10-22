package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.NotBlank;

public record RelyingPartyEaaResource(
    @NotBlank String namespace,
    @NotBlank String intent
) { }
