package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialsResource(
    @NotNull(message = "invalid_credentials")
    @JsonProperty(value = "credentials", required = true)
    List<@Valid CredentialResource> credentials
) { }
