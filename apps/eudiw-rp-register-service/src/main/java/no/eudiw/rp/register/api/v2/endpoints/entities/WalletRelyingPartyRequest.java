package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.IdentifierResource;
import no.eudiw.rp.register.api.v2.resource.LegalPersonResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;
import no.eudiw.rp.register.api.v2.resource.V2ResourceValidation;

import java.util.List;

public record WalletRelyingPartyRequest(
    String tradeName,
    @JsonProperty(required = true) @NotNull Boolean isPSB,
    @JsonProperty(required = true) @NotBlank String country,
    @Valid LegalPersonResource legalPerson,
    @JsonProperty(required = true) @NotEmpty List<@NotNull @Valid IdentifierResource> identifier,
    @JsonProperty(required = true) @NotEmpty List<@NotNull @Valid PolicyResource> policy,
    List<@NotBlank String> x5c,
    List<@NotBlank String> postalAddress,
    List<@NotBlank String> email,
    List<@NotBlank String> phone,
    List<@NotBlank String> infoURI
) {
    @AssertTrue(message = "invalid_info_uri")
    public boolean hasValidInfoUris() {
        return infoURI == null || infoURI.stream().allMatch(V2ResourceValidation::isAbsoluteUri);
    }
}
