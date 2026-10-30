package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.eudiw.rp.register.api.v2.resource.IdentifierResource;
import no.eudiw.rp.register.api.v2.resource.IntermediaryReferenceResource;
import no.eudiw.rp.register.api.v2.resource.LegalPersonResource;
import no.eudiw.rp.register.api.v2.resource.LawResource;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;
import no.eudiw.rp.register.api.v2.resource.ProvidedAttestationResource;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v2.resource.SupervisoryAuthorityResource;
import no.eudiw.rp.register.config.RegistryProperties;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.MultiLangString;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RegistrationDetailsResponse(
    UUID id,
    UUID relyingPartyServiceId,
    UUID relyingPartyId,
    String orgno,
    String organizationName,
    String serviceTradeName,
    boolean isPSB,
    List<RelyingPartyEntitlementResponse> relyingPartyEntitlements,
    List<RelyingPartyEaaResource> relyingPartyEaas,
    List<CertificateResponse> accessCertificates,
    List<CertificateResponse> issuerCertificates,
    long createdMs,
    long lastUpdatedMs,
    boolean active,
    RelyingPartyDetails relyingParty,
    RelyingPartyServiceDetails relyingPartyService
) {
    public static RegistrationDetailsResponse from(RelyingPartyInstance relyingPartyInstance,
                                                   Map<String, Entitlement> entitlementCatalog,
                                                   RegistryProperties registryProperties,
                                                   SupervisoryAuthorityProperties supervisoryAuthorityProperties) {
        WalletRelyingPartyService relyingPartyService = relyingPartyInstance.getWalletRelyingPartyService();
        WalletRelyingParty relyingParty = relyingPartyService.getWalletRelyingParty();

        return new RegistrationDetailsResponse(
            relyingPartyInstance.getId(),
            relyingPartyService.getId(),
            relyingParty.getId(),
            relyingParty.getOrgno(),
            relyingParty.getName(),
            relyingPartyService.getServiceTradeName(),
            relyingParty.isPSB(),
            relyingPartyInstance.getRelyingPartyEntitlements()
                .stream()
                .map(relyingPartyEntitlement -> new RelyingPartyEntitlementResponse(
                    relyingPartyEntitlement.getEntitlement(),
                    displayName(relyingPartyEntitlement.getEntitlement(), entitlementCatalog),
                    relyingPartyEntitlement.getCredentialIssuerUrl()))
                .toList(),
            relyingPartyInstance.getRelyingPartyEaas()
                .stream()
                .map(relyingPartyEaa -> new RelyingPartyEaaResource(
                    relyingPartyEaa.getNamespace(),
                    relyingPartyEaa.getIntent()))
                .toList(),
            relyingPartyInstance.getAccessCertificates()
                .stream()
                .map(CertificateResponse::from)
                .toList(),
            relyingPartyInstance.getIssuerCertificates()
                .stream()
                .map(CertificateResponse::from)
                .toList(),
            relyingPartyInstance.getCreatedMs(),
            relyingPartyInstance.getLastUpdatedMs(),
            relyingPartyInstance.isActive(),
            RelyingPartyDetails.from(relyingParty, registryProperties, supervisoryAuthorityProperties),
            RelyingPartyServiceDetails.from(relyingPartyService));
    }

    public record RelyingPartyEntitlementResponse(String entitlement, String displayName, String credentialIssuerUrl) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RelyingPartyDetails(
        String tradeName,
        String country,
        String providerType,
        LegalPersonResource legalPerson,
        List<IdentifierResource> identifier,
        List<PolicyResource> policy,
        List<String> postalAddress,
        List<String> email,
        List<String> phone,
        List<String> infoURI,
        List<String> x5c,
        SupervisoryAuthorityResource supervisoryAuthority,
        String registryURI
    ) {
        private static RelyingPartyDetails from(
            WalletRelyingParty relyingParty,
            RegistryProperties registryProperties,
            SupervisoryAuthorityProperties supervisoryAuthorityProperties
        ) {
            LegalPerson legalPerson = relyingParty.getLegalPerson();
            return new RelyingPartyDetails(
                relyingParty.getTradeName(),
                relyingParty.getCountry(),
                relyingParty.getProviderType(),
                legalPerson == null ? null : new LegalPersonResource(
                    legalPerson.getLegalName(),
                    mapOrEmpty(legalPerson.getEstablishedByLaw(), law ->
                        new LawResource(law.legislativeIdentifier(), law.legalBasis()))),
                mapOrEmpty(relyingParty.getIdentifier(), identifier ->
                    new IdentifierResource(identifier.type(), identifier.identifier())),
                mapOrEmpty(relyingParty.getPolicy(), policy ->
                    new PolicyResource(policy.type(), policy.policyURI())),
                listOrEmpty(relyingParty.getPostalAddress()),
                listOrEmpty(relyingParty.getEmail()),
                listOrEmpty(relyingParty.getPhone()),
                listOrEmpty(relyingParty.getInfoURI()),
                listOrEmpty(relyingParty.getX5c()),
                RelyingPartyResponse.supervisoryAuthorityResource(supervisoryAuthorityProperties),
                registryProperties.registryUri());
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RelyingPartyServiceDetails(
        String serviceIdentifier,
        List<List<MultiLangStringResource>> srvDescription,
        @JsonProperty("supportURI") String supportUri,
        String email,
        String phone,
        List<String> subEntitlements,
        List<ProvidedAttestationResource> providesAttestations,
        @JsonProperty("isIntermediary") boolean isIntermediary,
        List<IntermediaryReferenceResource> usesIntermediaries,
        List<String> servedWRPServices,
        boolean active
    ) {
        private static RelyingPartyServiceDetails from(WalletRelyingPartyService relyingPartyService) {
            return new RelyingPartyServiceDetails(
                relyingPartyService.getServiceIdentifier(),
                toNestedMultiLangStringResource(relyingPartyService.getSrvDescription()),
                relyingPartyService.getSupportUri(),
                relyingPartyService.getEmail(),
                relyingPartyService.getPhone(),
                listOrEmpty(relyingPartyService.getSubEntitlements()),
                mapOrEmpty(relyingPartyService.getProvidesAttestations(), item ->
                    new ProvidedAttestationResource(item.format(), item.type())),
                relyingPartyService.isIntermediary(),
                mapOrEmpty(relyingPartyService.getUsesIntermediaries(), item ->
                    new IntermediaryReferenceResource(
                        item.serviceIdentifier(), item.serviceTradeName(), item.registryURI())),
                listOrEmpty(relyingPartyService.getServedWRPServices()),
                relyingPartyService.isActive());
        }
    }

    private static String displayName(String entitlementUri, Map<String, Entitlement> entitlementCatalog) {
        Entitlement entitlement = entitlementCatalog.get(entitlementUri);
        return entitlement == null ? entitlementUri : entitlement.getDisplayName();
    }

    private static List<List<MultiLangStringResource>> toNestedMultiLangStringResource(
        List<List<MultiLangString>> values
    ) {
        if (values == null) {
            return null;
        }

        return values.stream()
            .map(items -> mapOrEmpty(items, item -> new MultiLangStringResource(item.lang(), item.content())))
            .toList();
    }

    private static <T, R> List<R> mapOrEmpty(List<T> values, Function<T, R> mapper) {
        return values == null ? List.of() : values.stream().map(mapper).toList();
    }

    private static <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
