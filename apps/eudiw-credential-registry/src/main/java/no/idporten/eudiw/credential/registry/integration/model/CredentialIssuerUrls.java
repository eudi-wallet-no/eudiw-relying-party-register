package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;


import java.net.URI;
import java.util.List;

public record CredentialIssuerUrls (
        @JsonProperty("credential_issuer_urls")
        List<URI> credentialIssuerUrls
){
}
