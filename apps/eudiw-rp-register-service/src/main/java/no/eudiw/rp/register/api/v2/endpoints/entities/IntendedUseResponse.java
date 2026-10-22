package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import no.eudiw.rp.register.api.v2.resource.CredentialResource;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record IntendedUseResponse(
    UUID id,
    UUID serviceId,
    String intendedUseIdentifier,
    String createdAt,
    String revokedAt,
    List<@Valid MultiLangStringResource> purpose,
    List<@Valid PolicyResource> privacyPolicy,
    List<@Valid CredentialResource> credentials
) { }
