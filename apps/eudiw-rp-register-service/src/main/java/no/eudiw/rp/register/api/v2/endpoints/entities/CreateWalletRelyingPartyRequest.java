package no.eudiw.rp.register.api.v2.endpoints.entities;

import jakarta.validation.constraints.NotBlank;
import no.idporten.validators.orgnr.Orgnr;

public record CreateWalletRelyingPartyRequest(
    @Orgnr @NotBlank String orgno
) { }
