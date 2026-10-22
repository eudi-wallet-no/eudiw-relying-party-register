package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import no.eudiw.rp.register.api.v2.resource.IntermediaryReferenceResource;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.ProvidedAttestationResource;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record WalletRelyingPartyServiceResponse(
    UUID id,
    UUID partyId,
    String serviceTradeName,
    String serviceIdentifier,
    @JsonProperty("supportURI") String supportUri,
    String email,
    String phone,
    List<List<@Valid MultiLangStringResource>> srvDescription,
    List<String> entitlements,
    List<String> subEntitlements,
    List<@Valid ProvidedAttestationResource> providesAttestations,
    List<@Valid IntermediaryReferenceResource> usesIntermediaries,
    @JsonProperty("isIntermediary") boolean isIntermediary,
    List<String> servedWRPServices,
    boolean active,
    long createdMs,
    long lastUpdatedMs,
    List<UUID> instanceIds,
    List<UUID> intendedUseIds
) { }
