package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.With;
import no.idporten.eudiw.rp.admin.entitlements.Entitlements;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyResource(

    @JsonProperty(value = "id", required = true)
    UUID id,

    @JsonProperty(value = "org_nr", required = true)
    String orgno,

    @JsonProperty(value = "name", required = true)
    String tradeName,

    @JsonProperty(value = "org_name", required = true)
    String orgName,

    @JsonProperty(value = "public_sector", required = true)
    boolean publicSector,

    @JsonProperty(value = "relying_party_entitlements", required = true)
    List<RelyingPartyEntitlementResource> relyingPartyEntitlements,

    @JsonProperty(value = "relying_party_eaas", required = true)
    List<RelyingPartyEaaResource> relyingPartyEaas,

    @JsonProperty(value = "access_certificates", required = true)
    List<RelyingPartyCertificateResource> accessCertificates,

    @JsonProperty(value = "issuer_certificates", required = true)
    List<RelyingPartyCertificateResource> issuerCertificates,

    @JsonProperty(value = "created_ms", required = true)
    long createdMs,

    @JsonProperty(value = "last_updated_ms", required = true)
    long lastUpdatedMs,

    @JsonProperty(value = "active", required = true)
    boolean active,

    @JsonProperty("relying_party")
    RelyingPartyDetails relyingParty,

    @JsonProperty("relying_party_service")
    RelyingPartyServiceDetails relyingPartyService,

    @JsonProperty("intended_uses")
    List<IntendedUseDetails> intendedUses
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelyingPartyDetails(
        String tradeName,
        String country,
        String providerType,
        LegalPersonDetails legalPerson,
        List<IdentifierDetails> identifier,
        List<PolicyDetails> policy,
        List<String> postalAddress,
        List<String> email,
        List<String> phone,
        @JsonProperty("infoURI") List<String> infoUri,
        List<String> x5c,
        SupervisoryAuthorityDetails supervisoryAuthority,
        @JsonProperty("registryURI") String registryUri
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SupervisoryAuthorityDetails(
        String name,
        String country,
        List<String> phone,
        List<String> email,
        @JsonProperty("formURI") List<String> formUri
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegalPersonDetails(
        List<String> legalName,
        List<LawDetails> establishedByLaw
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LawDetails(String legislativeIdentifier, String legalBasis) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IdentifierDetails(String type, String identifier) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyDetails(
        String type,
        @JsonProperty("policyURI") String policyUri
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelyingPartyServiceDetails(
        String serviceIdentifier,
        List<List<MultiLangStringDetails>> srvDescription,
        @JsonProperty("supportURI") String supportUri,
        String email,
        String phone,
        List<String> subEntitlements,
        List<ProvidedAttestationDetails> providesAttestations,
        @JsonProperty("isIntermediary") boolean isIntermediary,
        List<IntermediaryReferenceDetails> usesIntermediaries,
        List<String> servedWRPServices,
        boolean active
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MultiLangStringDetails(String lang, String content) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProvidedAttestationDetails(String format, String type) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntermediaryReferenceDetails(
        String serviceIdentifier,
        String serviceTradeName,
        @JsonProperty("registryURI") String registryUri
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntendedUseDetails(
        UUID id,
        String intendedUseIdentifier,
        String createdAt,
        String revokedAt,
        List<MultiLangStringDetails> purpose,
        List<PolicyDetails> privacyPolicy,
        List<CredentialDetails> credentials
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CredentialDetails(
        String format,
        Map<String, Object> meta,
        List<Map<String, Object>> claims
    ) { }

    @JsonIgnore
    public List<RelyingPartyEntitlementResource> getIssuerEntitlements() {
        return relyingPartyEntitlements.stream()
            .filter(Entitlements::isIssuerEntitlement)
            .toList();
    }

    public boolean hasIssuerEntitlements() {
        return this.relyingPartyEntitlements()
                   .stream()
                   .anyMatch(Entitlements::isIssuerEntitlement);
    }
}
