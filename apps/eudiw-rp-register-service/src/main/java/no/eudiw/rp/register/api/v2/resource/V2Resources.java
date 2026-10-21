package no.eudiw.rp.register.api.v2.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.validators.orgnr.Orgnr;
import no.eudiw.rp.register.api.v1.resource.certificates.X509CertificateJsonDeserializer;
import no.eudiw.rp.register.api.v1.resource.certificates.X509CertificateJsonSerializer;
import no.eudiw.rp.register.api.v1.resource.certificates.PKCS10CertificationRequestJsonDeserializer;
import no.eudiw.rp.register.api.v1.resource.certificates.PKCS10CertificationRequestJsonSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.X509Certificate;
import java.net.URI;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class V2Resources {

    private V2Resources() { }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record WalletRelyingPartyResource(
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
        List<UUID> serviceIds,
        SupervisoryAuthorityResource supervisoryAuthority,
        String registryURI
    ) { }

    public record WalletRelyingPartyWriteResource(
        String tradeName,
        @JsonProperty(required = true) @NotNull Boolean isPSB,
        @JsonProperty(required = true) @NotBlank String country,
        @Valid LegalPersonResource legalPerson,
        @JsonProperty(required = true) @NotEmpty List<@NotNull @Valid IdentifierResource> identifier,
        @JsonProperty(required = true) @NotEmpty List<@NotNull @Valid PolicyResource> policy,
        List<@NotBlank String> x5c,
        List<@NotBlank String> postalAddress,
        List<@NotBlank String> email,
        List<@NotBlank String> phone,
        List<@NotBlank String> infoURI
    ) {
        @AssertTrue(message = "invalid_info_uri")
        public boolean hasValidInfoUris() {
            return infoURI == null || infoURI.stream().allMatch(V2Resources::isAbsoluteUri);
        }
    }

    public record LegalPersonResource(
        @JsonProperty(required = true) @NotEmpty List<@NotBlank String> legalName,
        List<@NotNull @Valid LawResource> establishedByLaw
    ) { }

    public record LawResource(
        String legislativeIdentifier,
        String legalBasis
    ) { }

    public record IdentifierResource(
        @NotBlank String type,
        @NotBlank String identifier
    ) { }

    public record PolicyResource(
        @NotBlank String type,
        @NotBlank String policyURI
    ) {
        @AssertTrue(message = "invalid_policy_uri")
        public boolean hasValidPolicyUri() {
            return isAbsoluteUri(policyURI);
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record WalletRelyingPartyServiceResource(
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

    public record WalletRelyingPartyServiceWriteResource(
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
            return hasText(supportUri) || hasText(email) || hasText(phone);
        }

        @AssertTrue(message = "invalid_intermediary_configuration")
        public boolean hasValidIntermediaryConfiguration() {
            return Boolean.TRUE.equals(isIntermediary)
                ? usesIntermediaries == null || usesIntermediaries.isEmpty()
                : servedWRPServices == null || servedWRPServices.isEmpty();
        }

        @AssertTrue(message = "invalid_support_uri")
        public boolean hasValidSupportUri() {
            return supportUri == null || isAbsoluteUri(supportUri);
        }
    }

    public record MultiLangStringResource(
        @NotBlank String lang,
        @NotBlank String content
    ) { }

    public record ProvidedAttestationResource(
        @NotBlank String format,
        @NotBlank String type
    ) { }

    public record IntermediaryReferenceResource(
        @NotBlank String serviceIdentifier,
        @NotBlank String serviceTradeName,
        @JsonProperty("registryURI") @NotBlank String registryUri
    ) {
        @AssertTrue(message = "invalid_intermediary_registry_uri")
        public boolean hasValidRegistryUri() {
            return isAbsoluteUri(registryUri);
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RelyingPartyInstanceResource(
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

    public record RelyingPartyInstanceWriteResource(
        @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEntitlementResource> relyingPartyEntitlements,
        @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEaaResource> relyingPartyEaas
    ) { }

    public record RelyingPartyEntitlementResource(
        @NotBlank String entitlement,
        String credentialIssuerUrl
    ) {
        @AssertTrue(message = "invalid_credential_issuer_url")
        public boolean hasValidCredentialIssuerUrl() {
            return credentialIssuerUrl == null || isAbsoluteUri(credentialIssuerUrl);
        }
    }

    public record RelyingPartyEaaResource(
        @NotBlank String namespace,
        @NotBlank String intent
    ) { }

    public record CreateWalletRelyingPartyResource(
        @Orgnr @NotBlank String orgno
    ) { }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record IntendedUseResource(
        UUID id,
        UUID serviceId,
        String intendedUseIdentifier,
        String createdAt,
        String revokedAt,
        List<@Valid MultiLangStringResource> purpose,
        List<@Valid PolicyResource> privacyPolicy,
        List<@Valid CredentialResource> credentials
    ) { }

    public record CreateIntendedUseResource(
        @NotEmpty List<@NotNull @Valid MultiLangStringResource> purpose,
        @NotEmpty List<@NotNull @Valid PolicyResource> privacyPolicy,
        @NotEmpty List<@NotNull @Valid CredentialResource> credentials
    ) { }

    public record CredentialResource(
        @NotBlank String format,
        @NotNull Map<String, Object> meta,
        @NotEmpty List<@NotNull Map<String, Object>> claims
    ) {
        @AssertTrue(message = "invalid_credential_meta")
        public boolean hasValidMeta() {
            if (meta == null || meta.size() != 1) {
                return false;
            }
            Object vctValues = meta.get("vct_values");
            if (vctValues instanceof List<?> values) {
                return !values.isEmpty() && values.stream()
                    .allMatch(value -> value instanceof String text && !text.isBlank());
            }
            return meta.get("doctype") instanceof String doctype && !doctype.isBlank();
        }

        @AssertTrue(message = "invalid_credential_claims")
        public boolean hasValidClaims() {
            return claims != null && !claims.isEmpty() && claims.stream().allMatch(claim -> {
                Object path = claim.get("path");
                if (!(path instanceof List<?> pathParts) || pathParts.isEmpty()) {
                    return false;
                }
                return pathParts.stream().allMatch(part ->
                    part == null
                        || (part instanceof String text && !text.isBlank())
                        || (part instanceof Integer value && value >= 0)
                        || (part instanceof Long value && value >= 0)
                        || (part instanceof BigInteger value && value.signum() >= 0)
                );
            });
        }
    }

    public record SupervisoryAuthorityResource(
        String name,
        String country,
        List<String> phone,
        List<String> email,
        @JsonProperty("formURI") List<String> formUri
    ) { }


    public record IntendedUseReplacementResource(
        IntendedUseResource revoked,
        IntendedUseResource current
    ) { }

    public record EntitlementResource(
        UUID id,
        String entitlement,
        boolean active,
        String displayName,
        String caId
    ) { }

    public record EntitlementsResource(List<EntitlementResource> entitlements) { }

    public record CertificateResource(
        @JsonSerialize(using = X509CertificateJsonSerializer.class)
        @JsonDeserialize(using = X509CertificateJsonDeserializer.class)
        X509Certificate certificate,
        String entitlement,
        UUID id,
        @JsonProperty("revocation_status") Integer revocationStatus
    ) { }

    public record CertificatesResource(List<CertificateResource> certificates) { }

    public record AccessCertificateRequest(
        @JsonSerialize(using = PKCS10CertificationRequestJsonSerializer.class)
        @JsonDeserialize(using = PKCS10CertificationRequestJsonDeserializer.class)
        PKCS10CertificationRequest csr
    ) { }

    public record IssuerCertificateRequest(
        @JsonSerialize(using = PKCS10CertificationRequestJsonSerializer.class)
        @JsonDeserialize(using = PKCS10CertificationRequestJsonDeserializer.class)
        PKCS10CertificationRequest csr,
        @JsonProperty(required = true) @NotBlank String entitlement
    ) { }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isAbsoluteUri(String value) {
        if (!hasText(value)) {
            return false;
        }
        try {
            return URI.create(value).isAbsolute();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
