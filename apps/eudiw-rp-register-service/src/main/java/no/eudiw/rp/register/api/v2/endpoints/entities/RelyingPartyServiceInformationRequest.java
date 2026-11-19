package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.ResourceValidation;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;

import java.util.List;

public record RelyingPartyServiceInformationRequest(
    @JsonProperty(required = true) @NotBlank String serviceTradeName,
    String serviceIdentifier,
    @JsonProperty(required = true) @NotEmpty List<@NotNull @Valid MultiLangStringResource> srvDescription,
    @JsonProperty("supportURI") String supportUri,
    String email,
    String phone
) {
    @AssertTrue(message = "service_contact_required")
    public boolean hasContactInformation() {
        return ResourceValidation.hasText(supportUri)
            || ResourceValidation.hasText(email)
            || ResourceValidation.hasText(phone);
    }

    @AssertTrue(message = "invalid_support_uri")
    public boolean hasValidSupportUri() {
        return !ResourceValidation.hasText(supportUri) || ResourceValidation.isAbsoluteUri(supportUri);
    }

    public void update(WalletRelyingPartyService relyingPartyService) {
        relyingPartyService.rename(serviceTradeName);
        relyingPartyService.setServiceIdentifier(blankToNull(serviceIdentifier));
        relyingPartyService.setSrvDescription(srvDescription.stream().map(item -> item.toDomain()).toList());
        relyingPartyService.setSupportUri(blankToNull(supportUri));
        relyingPartyService.setEmail(blankToNull(email));
        relyingPartyService.setPhone(blankToNull(phone));
        relyingPartyService.setUpdated();
    }

    private static String blankToNull(String value) {
        return ResourceValidation.hasText(value) ? value.strip() : null;
    }
}
