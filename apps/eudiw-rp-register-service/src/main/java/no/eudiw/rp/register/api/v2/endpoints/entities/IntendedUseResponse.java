package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import no.eudiw.rp.register.api.v2.resource.CredentialResource;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;
import no.eudiw.rp.register.domain.IntendedUse;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record IntendedUseResponse(
    UUID id,
    @JsonProperty("serviceId") UUID relyingPartyServiceId,
    String intendedUseIdentifier,
    String createdAt,
    String revokedAt,
    List<@Valid MultiLangStringResource> purpose,
    List<@Valid PolicyResource> privacyPolicy,
    List<@Valid CredentialResource> credentials
) {
    public static IntendedUseResponse from(IntendedUse intendedUse) {
        return new IntendedUseResponse(
            intendedUse.getId(),
            intendedUse.getWalletRelyingPartyService().getId(),
            intendedUse.getIntendedUseIdentifier(),
            intendedUse.getCreatedAt(),
            intendedUse.getRevokedAt(),
            intendedUse.getPurpose().stream()
                .map(item -> new MultiLangStringResource(item.lang(), item.content()))
                .toList(),
            intendedUse.getPrivacyPolicy().stream()
                .map(policy -> new PolicyResource(policy.type(), policy.policyURI()))
                .toList(),
            intendedUse.getCredentials().stream()
                .map(credential -> new CredentialResource(
                    credential.getFormat(), credential.getMeta(), credential.getClaims()))
                .toList()
        );
    }
}
