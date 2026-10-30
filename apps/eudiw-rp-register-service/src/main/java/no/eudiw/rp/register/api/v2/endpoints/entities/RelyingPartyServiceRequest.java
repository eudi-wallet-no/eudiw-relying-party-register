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
import no.eudiw.rp.register.domain.MultiLangString;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;

import java.util.ArrayList;
import java.util.List;

public record RelyingPartyServiceRequest(
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

    public static RelyingPartyServiceRequest from(RelyingPartyServiceResponse relyingPartyService) {
        return new RelyingPartyServiceRequest(
            relyingPartyService.serviceTradeName(),
            relyingPartyService.serviceIdentifier(),
            relyingPartyService.supportUri(),
            relyingPartyService.email(),
            relyingPartyService.phone(),
            relyingPartyService.srvDescription(),
            relyingPartyService.subEntitlements(),
            relyingPartyService.providesAttestations(),
            relyingPartyService.usesIntermediaries(),
            relyingPartyService.isIntermediary(),
            relyingPartyService.servedWRPServices()
        );
    }

    public WalletRelyingPartyService toDomain(
        WalletRelyingParty relyingParty,
        List<RelyingPartyInstance> relyingPartyInstances
    ) {
        WalletRelyingPartyService relyingPartyService = new WalletRelyingPartyService(
            serviceTradeName,
            relyingParty,
            relyingPartyInstances,
            isIntermediary,
            listOrEmpty(usesIntermediaries).stream().map(item -> item.toDomain()).toList(),
            listOrEmpty(servedWRPServices)
        );
        relyingPartyService.setServiceIdentifier(serviceIdentifier);
        relyingPartyService.setSupportUri(supportUri);
        relyingPartyService.setEmail(email);
        relyingPartyService.setPhone(phone);
        relyingPartyService.setSrvDescription(toDomainSrvDescription());
        relyingPartyService.setSubEntitlements(listOrEmpty(subEntitlements));
        relyingPartyService.setProvidesAttestations(
            listOrEmpty(providesAttestations).stream().map(item -> item.toDomain()).toList());
        return relyingPartyService;
    }

    public void update(WalletRelyingPartyService relyingPartyService) {
        relyingPartyService.setIntermediaryConfiguration(
            isIntermediary,
            listOrEmpty(usesIntermediaries).stream().map(item -> item.toDomain()).toList(),
            listOrEmpty(servedWRPServices)
        );
        if (!java.util.Objects.equals(relyingPartyService.getServiceTradeName(), serviceTradeName)) {
            relyingPartyService.getRelyingPartyInstances().forEach(RelyingPartyInstance::setUpdated);
        }
        relyingPartyService.setServiceTradeName(serviceTradeName);
        relyingPartyService.setServiceIdentifier(serviceIdentifier);
        relyingPartyService.setSupportUri(supportUri);
        relyingPartyService.setEmail(email);
        relyingPartyService.setPhone(phone);
        relyingPartyService.setSrvDescription(toDomainSrvDescription());
        relyingPartyService.setSubEntitlements(listOrEmpty(subEntitlements));
        relyingPartyService.setProvidesAttestations(
            listOrEmpty(providesAttestations).stream().map(item -> item.toDomain()).toList());
        relyingPartyService.setUpdated();
    }

    private List<List<MultiLangString>> toDomainSrvDescription() {
        return srvDescription == null
            ? null
            : srvDescription.stream()
                .map(values -> values.stream().map(item -> item.toDomain()).toList())
                .toList();
    }

    private static <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
