package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record CredentialIssuerUrlsResponse(
    @JsonProperty("credential_issuer_urls") Set<String> credentialIssuerUrls
) { }
