package no.eudiw.rp.register.api.v2.endpoints.entities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.CredentialResource;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;

import java.util.List;

public record CreateIntendedUseRequest(
    @NotEmpty List<@NotNull @Valid MultiLangStringResource> purpose,
    @NotEmpty List<@NotNull @Valid PolicyResource> privacyPolicy,
    @NotEmpty List<@NotNull @Valid CredentialResource> credentials
) { }
