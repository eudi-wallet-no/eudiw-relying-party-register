package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialMetadata(
    @Size(min = 1, message = "empty_display") // like @NotEmpty but allows null
    @JsonProperty("display")
    List<@NotNull @Valid Display> credentialTypeDisplays,

    @NotNull(message = "null_claims")
    @JsonProperty(value = "claims", required = true)
    List<@NotNull @Valid Claims> claims
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Claims(
        @NotEmpty
        @JsonProperty(value = "path", required = true)
        List<@NotBlank String> paths,

        @Size(min = 1, message = "empty_display") // like @NotEmpty but allows null
        @JsonProperty("display")
        List<@NotNull @Valid Display> displays
    ) {
        public String getClaimsDisplayName(String locale) {
            return this.displays != null
                       ? Display.getDisplayNameForLocale(locale, this.displays)
                       : null;
        }
        public String getDcqlFormattedPaths() {
            return "[%s]".formatted(
                paths.stream()
                     .map("\"%s\""::formatted)
                     .collect(Collectors.joining(", ")));
        }
    }
}
