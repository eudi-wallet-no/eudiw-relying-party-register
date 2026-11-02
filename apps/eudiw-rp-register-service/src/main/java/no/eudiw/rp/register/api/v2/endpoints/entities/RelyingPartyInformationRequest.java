package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.api.v2.resource.V2ResourceValidation;

import java.util.List;

public record RelyingPartyInformationRequest(
    String tradeName,
    @JsonProperty(required = true) @NotBlank String country,
    List<@NotBlank String> postalAddress,
    List<@NotBlank String> email,
    List<@NotBlank String> phone,
    List<@NotBlank String> infoURI
) {
    @AssertTrue(message = "invalid_info_uri")
    public boolean hasValidInformationUris() {
        return infoURI == null || infoURI.stream().allMatch(V2ResourceValidation::isAbsoluteUri);
    }
}
