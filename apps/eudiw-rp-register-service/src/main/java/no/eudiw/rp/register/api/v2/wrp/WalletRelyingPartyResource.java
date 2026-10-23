package no.eudiw.rp.register.api.v2.wrp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
import no.eudiw.rp.register.config.WrpApiProperties;
import no.eudiw.rp.register.domain.Credential;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WalletRelyingPartyResource(
    String country,
    List<String> email,
    List<IdentifierResource> identifier,
    List<String> infoURI,
    LegalPersonResource legalPerson,
    List<String> phone,
    List<PolicyResource> policy,
    String providerType,
    List<String> x5c,
    @JsonProperty("isPSB") boolean isPSB,
    SupervisoryAuthorityResource supervisoryAuthority,
    String tradeName,
    String registryURI,
    List<ServiceResource> services
) {

    public static WalletRelyingPartyResource from(
        WalletRelyingParty relyingParty,
        Map<UUID, List<String>> entitlementsByService,
        SupervisoryAuthorityProperties supervisoryAuthority,
        WrpApiProperties properties
    ) {
        return new WalletRelyingPartyResource(
            relyingParty.getCountry(),
            relyingParty.getEmail(),
            relyingParty.getIdentifier().stream()
                .map(identifier -> new IdentifierResource(identifier.identifier(), identifier.type()))
                .toList(),
            relyingParty.getInfoURI(),
            legalPerson(relyingParty.getLegalPerson()),
            relyingParty.getPhone(),
            relyingParty.getPolicy().stream()
                .map(policy -> new PolicyResource(policy.policyURI(), policy.type()))
                .toList(),
            relyingParty.getProviderType(),
            List.copyOf(relyingParty.getX5c()),
            relyingParty.isPSB(),
            SupervisoryAuthorityResource.from(supervisoryAuthority),
            relyingParty.getTradeName(),
            properties.registryUri(),
            relyingParty.getServices().stream()
                .filter(WalletRelyingPartyService::isActive)
                .map(service -> ServiceResource.from(
                    service, entitlementsByService.getOrDefault(service.getId(), List.of())))
                .toList()
        );
    }

    private static LegalPersonResource legalPerson(LegalPerson legalPerson) {
        return legalPerson == null ? null : new LegalPersonResource(
            legalPerson.getEstablishedByLaw().stream()
                .map(law -> new LawResource(law.legislativeIdentifier(), law.legalBasis()))
                .toList(),
            legalPerson.getLegalName()
        );
    }

    private static List<String> optionalList(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IdentifierResource(String identifier, String type) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PolicyResource(String policyURI, String type) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LawResource(String legislativeIdentifier, String legalBasis) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LegalPersonResource(List<LawResource> establishedByLaw, List<String> legalName) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SupervisoryAuthorityResource(
        String country,
        List<String> email,
        List<String> formURI,
        String name,
        List<String> phone
    ) {
        private static SupervisoryAuthorityResource from(SupervisoryAuthorityProperties properties) {
            return new SupervisoryAuthorityResource(
                properties.country(),
                optionalList(properties.email()),
                optionalList(properties.formUri()),
                properties.name(),
                optionalList(properties.phone())
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ServiceResource(
        String serviceTradeName,
        String serviceIdentifier,
        String supportURI,
        String email,
        String phone,
        List<List<MultiLangStringResource>> srvDescription,
        List<IntendedUseResource> intendedUses,
        List<String> entitlements,
        List<String> subEntitlements,
        List<ProvidedAttestationResource> providesAttestations,
        List<IntermediaryResource> usesIntermediaries,
        boolean isIntermediary,
        List<String> servedWRPServices
    ) {
        @JsonProperty("supportURI")
        public String supportURI() {
            return supportURI;
        }

        private static ServiceResource from(WalletRelyingPartyService relyingPartyService, List<String> entitlements) {
            return new ServiceResource(
                relyingPartyService.getServiceTradeName(),
                relyingPartyService.getServiceIdentifier(),
                relyingPartyService.getSupportUri(),
                relyingPartyService.getEmail(),
                relyingPartyService.getPhone(),
                mapDescriptions(relyingPartyService.getSrvDescription()),
                relyingPartyService.getIntendedUses().stream().map(IntendedUseResource::from).toList(),
                entitlements,
                relyingPartyService.getSubEntitlements(),
                relyingPartyService.getProvidesAttestations().stream()
                    .map(attestation -> new ProvidedAttestationResource(
                        attestation.format(), attestation.type()))
                    .toList(),
                relyingPartyService.getUsesIntermediaries().stream()
                    .map(intermediary -> new IntermediaryResource(
                        intermediary.serviceIdentifier(),
                        intermediary.serviceTradeName(),
                        intermediary.registryURI()))
                    .toList(),
                relyingPartyService.isIntermediary(),
                relyingPartyService.getServedWRPServices()
            );
        }

        private static List<List<MultiLangStringResource>> mapDescriptions(
            List<List<no.eudiw.rp.register.domain.MultiLangString>> descriptions
        ) {
            if (descriptions == null) {
                return null;
            }
            return descriptions.stream()
                .map(group -> group.stream()
                    .map(value -> new MultiLangStringResource(value.lang(), value.content()))
                    .toList())
                .toList();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MultiLangStringResource(String lang, String content) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IntendedUseResource(
        List<MultiLangStringResource> purpose,
        List<PolicyResource> privacyPolicy,
        String intendedUseIdentifier,
        String createdAt,
        String revokedAt,
        List<CredentialResource> credentials
    ) {
        private static IntendedUseResource from(IntendedUse intendedUse) {
            return new IntendedUseResource(
                intendedUse.getPurpose() == null ? null : intendedUse.getPurpose().stream()
                    .map(value -> new MultiLangStringResource(value.lang(), value.content()))
                    .toList(),
                    intendedUse.getPrivacyPolicy() == null ? null : intendedUse.getPrivacyPolicy().stream()
                    .map(policy -> new PolicyResource(policy.policyURI(), policy.type()))
                    .toList(),
                intendedUse.getIntendedUseIdentifier(),
                intendedUse.getCreatedAt(),
                intendedUse.getRevokedAt(),
                intendedUse.getCredentials().stream().map(CredentialResource::from).toList()
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CredentialResource(String format, Map<String, Object> meta, List<Map<String, Object>> claims) {
        private static CredentialResource from(Credential credential) {
            Map<String, Object> meta = new LinkedHashMap<>(credential.getMeta());
            if (meta.containsKey("doctype")) {
                meta.put("doctype_value", meta.remove("doctype"));
            }
            List<Map<String, Object>> claims = new ArrayList<>(credential.getClaims());
            return new CredentialResource(credential.getFormat(), meta, claims);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProvidedAttestationResource(String format, String type) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IntermediaryResource(String serviceIdentifier, String serviceTradeName, String registryURI) { }
}
