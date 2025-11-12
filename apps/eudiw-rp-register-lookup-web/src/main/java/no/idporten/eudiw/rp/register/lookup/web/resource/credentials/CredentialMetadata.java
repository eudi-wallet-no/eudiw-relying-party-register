package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialMetadata(
    @NotNull(message = "null_display")
    @JsonProperty(value = "display", required = true)
    List<@Valid Display> displays,

    @NotNull(message = "null_claims")
    @JsonProperty(value = "claims", required = true)
    List<@Valid Claims> claims
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Display(
        @NotBlank
        @JsonProperty(value = "name", required = true)
        String name,
        @NotNull
        @JsonProperty(value = "locale", required = true)
        Locale locale
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Claims(
        @NotNull
        @Valid
        @JsonProperty(value = "path", required = true)
        List<@NotBlank String> paths,

        @NotNull
        @Valid
        @JsonProperty(value = "display", required = true)
        List<@Valid Display> displays
    ) { }
}
