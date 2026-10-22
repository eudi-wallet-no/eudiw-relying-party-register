package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartyInstanceResponse(
    UUID id,
    @JsonProperty("serviceId") UUID relyingPartyServiceId,
    List<@Valid RelyingPartyEntitlementResource> relyingPartyEntitlements,
    List<@Valid RelyingPartyEaaResource> relyingPartyEaas,
    boolean active,
    long createdMs,
    long lastUpdatedMs,
    List<UUID> accessCertificateIds,
    List<UUID> issuerCertificateIds
) {
    public static RelyingPartyInstanceResponse from(RelyingPartyInstance relyingPartyInstance) {
        return new RelyingPartyInstanceResponse(
            relyingPartyInstance.getId(),
            relyingPartyInstance.getWalletRelyingPartyService().getId(),
            relyingPartyInstance.getRelyingPartyEntitlements().stream()
                .map(item -> new RelyingPartyEntitlementResource(
                    item.getEntitlement(), item.getCredentialIssuerUrl()))
                .toList(),
            relyingPartyInstance.getRelyingPartyEaas().stream()
                .map(item -> new RelyingPartyEaaResource(item.getNamespace(), item.getIntent()))
                .toList(),
            relyingPartyInstance.isActive(),
            relyingPartyInstance.getCreatedMs(),
            relyingPartyInstance.getLastUpdatedMs(),
            relyingPartyInstance.getAccessCertificates().stream().map(certificate -> certificate.getId()).toList(),
            relyingPartyInstance.getIssuerCertificates().stream().map(certificate -> certificate.getId()).toList()
        );
    }
}
