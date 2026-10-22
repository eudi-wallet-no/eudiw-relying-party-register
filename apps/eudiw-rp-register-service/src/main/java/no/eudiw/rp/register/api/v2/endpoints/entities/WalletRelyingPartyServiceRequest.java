package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.IntermediaryReferenceResource;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.ProvidedAttestationResource;
import no.eudiw.rp.register.api.v2.resource.V2ResourceValidation;

import java.util.List;

public record WalletRelyingPartyServiceRequest(
    @JsonProperty(required = true) @NotBlank String serviceTradeName,
    String serviceIdentifier,
    @JsonProperty("supportURI") String supportUri,
    String email,
    String phone,
    @JsonProperty(required = true) @NotEmpty List<@NotEmpty List<@NotNull @Valid MultiLangStringResource>> srvDescription,
    List<@NotBlank String> subEntitlements,
    List<@NotNull @Valid ProvidedAttestationResource> providesAttestations,
    List<@NotNull @Valid IntermediaryReferenceResource> usesIntermediaries,
    @JsonProperty(value = "isIntermediary", required = true) @NotNull Boolean isIntermediary,
    List<@NotBlank String> servedWRPServices
) {
    @AssertTrue(message = "service_contact_required")
    public boolean hasContactInformation() {
        return V2ResourceValidation.hasText(supportUri)
            || V2ResourceValidation.hasText(email)
            || V2ResourceValidation.hasText(phone);
    }

    @AssertTrue(message = "invalid_intermediary_configuration")
    public boolean hasValidIntermediaryConfiguration() {
        return Boolean.TRUE.equals(isIntermediary)
            ? usesIntermediaries == null || usesIntermediaries.isEmpty()
            : servedWRPServices == null || servedWRPServices.isEmpty();
    }

    @AssertTrue(message = "invalid_support_uri")
    public boolean hasValidSupportUri() {
        return supportUri == null || V2ResourceValidation.isAbsoluteUri(supportUri);
    }
}
