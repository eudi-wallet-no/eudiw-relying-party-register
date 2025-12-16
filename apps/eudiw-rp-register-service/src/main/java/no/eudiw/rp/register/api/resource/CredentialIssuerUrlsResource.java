package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record CredentialIssuerUrlsResource(
    @JsonProperty("credential_issuer_urls")
    Set<String> credentialIssuerURls
) { }
