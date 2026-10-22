package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import no.eudiw.rp.register.api.v2.resource.IntermediaryReferenceResource;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.ProvidedAttestationResource;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.MultiLangString;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartyServiceResponse(
    UUID id,
    @JsonProperty("partyId") UUID relyingPartyId,
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
    @JsonProperty("instanceIds") List<UUID> relyingPartyInstanceIds,
    List<UUID> intendedUseIds
) {
    public static RelyingPartyServiceResponse from(WalletRelyingPartyService relyingPartyService) {
        return new RelyingPartyServiceResponse(
            relyingPartyService.getId(),
            relyingPartyService.getWalletRelyingParty().getId(),
            relyingPartyService.getServiceTradeName(),
            relyingPartyService.getServiceIdentifier(),
            relyingPartyService.getSupportUri(),
            relyingPartyService.getEmail(),
            relyingPartyService.getPhone(),
            toNestedMultiLangStringResource(relyingPartyService.getSrvDescription()),
            relyingPartyService.getEntitlements(),
            relyingPartyService.getSubEntitlements(),
            relyingPartyService.getProvidesAttestations().stream()
                .map(item -> new ProvidedAttestationResource(item.format(), item.type()))
                .toList(),
            relyingPartyService.getUsesIntermediaries().stream()
                .map(item -> new IntermediaryReferenceResource(
                    item.serviceIdentifier(), item.serviceTradeName(), item.registryURI()))
                .toList(),
            relyingPartyService.isIntermediary(),
            relyingPartyService.getServedWRPServices(),
            relyingPartyService.isActive(),
            relyingPartyService.getCreatedMs(),
            relyingPartyService.getLastUpdatedMs(),
            relyingPartyService.getRelyingPartyInstances().stream().map(RelyingPartyInstance::getId).toList(),
            relyingPartyService.getIntendedUses().stream().map(IntendedUse::getId).toList()
        );
    }

    private static List<List<MultiLangStringResource>> toNestedMultiLangStringResource(
        List<List<MultiLangString>> values
    ) {
        if (values == null) {
            return null;
        }

        return values.stream().map(RelyingPartyServiceResponse::toMultiLangStringResource).toList();
    }

    private static List<MultiLangStringResource> toMultiLangStringResource(List<MultiLangString> values) {
        return values.stream()
            .map(item -> new MultiLangStringResource(item.lang(), item.content()))
            .toList();
    }
}
