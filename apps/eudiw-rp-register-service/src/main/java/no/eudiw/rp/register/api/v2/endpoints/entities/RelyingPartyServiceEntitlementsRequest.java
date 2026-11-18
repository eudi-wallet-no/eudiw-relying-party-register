package no.eudiw.rp.register.api.v2.endpoints.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEntitlementResource;

import java.util.List;

public record RelyingPartyServiceEntitlementsRequest(
    @Schema(description = "The service roles to set.")
    @NotNull List<@NotBlank String> entitlements,
    @Schema(description = "The URLs to set. Send only entries for changed URLs. Send null to clear a URL.")
    List<@NotNull @Valid RelyingPartyEntitlementResource> credentialIssuerUrls
) { }
