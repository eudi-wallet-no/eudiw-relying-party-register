package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEntitlementResource;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartyInstanceResponse(
    UUID id,
    UUID serviceId,
    List<@Valid RelyingPartyEntitlementResource> relyingPartyEntitlements,
    List<@Valid RelyingPartyEaaResource> relyingPartyEaas,
    boolean active,
    long createdMs,
    long lastUpdatedMs,
    List<UUID> accessCertificateIds,
    List<UUID> issuerCertificateIds
) { }
