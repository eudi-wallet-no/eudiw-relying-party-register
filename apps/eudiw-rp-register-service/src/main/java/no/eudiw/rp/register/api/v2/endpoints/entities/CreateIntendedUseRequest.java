package no.eudiw.rp.register.api.v2.endpoints.entities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.CredentialResource;
import no.eudiw.rp.register.api.v2.resource.MultiLangStringResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;
import no.eudiw.rp.register.domain.Credential;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.Policy;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public record CreateIntendedUseRequest(
    @NotEmpty List<@NotNull @Valid MultiLangStringResource> purpose,
    @NotEmpty List<@NotNull @Valid PolicyResource> privacyPolicy,
    @NotEmpty List<@NotNull @Valid CredentialResource> credentials
) {
    public IntendedUse toDomain() {
        return toDomain(LocalDate.now(ZoneOffset.UTC).toString());
    }

    public IntendedUse toDomain(String createdAt) {
        IntendedUse intendedUse = new IntendedUse(
            UUID.randomUUID().toString(),
            createdAt,
            null,
            purpose.stream().map(item -> item.toDomain()).toList(),
            privacyPolicy.stream().map(item -> item.toDomain()).toList()
        );
        List<Credential> domainCredentials = credentials.stream()
            .map(item -> item.toDomain())
            .toList();
        intendedUse.setCredentials(domainCredentials);
        return intendedUse;
    }
}
