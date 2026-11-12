package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.eudiw.rp.register.api.v2.resource.IdentifierResource;
import no.eudiw.rp.register.api.v2.resource.LegalPersonResource;
import no.eudiw.rp.register.api.v2.resource.LawResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;
import no.eudiw.rp.register.api.v2.resource.SupervisoryAuthorityResource;
import no.eudiw.rp.register.config.RegistryProperties;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartyResponse(
    UUID id,
    String orgno,
    String tradeName,
    boolean isPSB,
    String country,
    String providerType,
    LegalPersonResource legalPerson,
    List<IdentifierResource> identifier,
    List<PolicyResource> policy,
    List<String> x5c,
    List<String> postalAddress,
    List<String> email,
    List<String> phone,
    List<String> infoURI,
    boolean active,
    long createdMs,
    long lastUpdatedMs,
    @JsonProperty("serviceIds") List<UUID> relyingPartyServiceIds,
    SupervisoryAuthorityResource supervisoryAuthority,
    String registryURI
) {
    public static RelyingPartyResponse from(
        WalletRelyingParty relyingParty,
        RegistryProperties registryProperties,
        SupervisoryAuthorityProperties supervisoryAuthorityProperties
    ) {
        return new RelyingPartyResponse(
            relyingParty.getId(),
            relyingParty.getOrgno(),
            relyingParty.getTradeName(),
            relyingParty.isPSB(),
            relyingParty.getCountry(),
            relyingParty.getProviderType(),
            toLegalPersonResource(relyingParty.getLegalPerson()),
            relyingParty.getIdentifier().stream()
                .map(identifier -> new IdentifierResource(identifier.type(), identifier.identifier()))
                .toList(),
            relyingParty.getPolicy().stream()
                .map(policy -> new PolicyResource(policy.type(), policy.policyURI()))
                .toList(),
            List.copyOf(relyingParty.getX5c()),
            relyingParty.getPostalAddress(),
            relyingParty.getEmail(),
            relyingParty.getPhone(),
            relyingParty.getInfoURI(),
            relyingParty.isActive(),
            relyingParty.getCreatedMs(),
            relyingParty.getLastUpdatedMs(),
            relyingParty.getServices().stream().map(WalletRelyingPartyService::getId).toList(),
            supervisoryAuthorityResource(supervisoryAuthorityProperties),
            registryProperties.registryUri()
        );
    }

    private static LegalPersonResource toLegalPersonResource(LegalPerson legalPerson) {
        if (legalPerson == null) {
            return null;
        }

        return new LegalPersonResource(
            legalPerson.getLegalName(),
            legalPerson.getEstablishedByLaw().stream()
                .map(item -> new LawResource(item.legislativeIdentifier(), item.legalBasis()))
                .toList()
        );
    }

    static SupervisoryAuthorityResource supervisoryAuthorityResource(
        SupervisoryAuthorityProperties properties
    ) {
        return new SupervisoryAuthorityResource(
            properties.name(),
            properties.country(),
            singletonIfPresent(properties.phone()),
            singletonIfPresent(properties.email()),
            singletonIfPresent(properties.formUri())
        );
    }

    private static List<String> singletonIfPresent(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }
}
