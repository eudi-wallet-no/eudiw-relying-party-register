package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.eudiw.rp.register.api.v2.resource.IdentifierResource;
import no.eudiw.rp.register.api.v2.resource.LegalPersonResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;
import no.eudiw.rp.register.api.v2.resource.SupervisoryAuthorityResource;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record WalletRelyingPartyResponse(
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
