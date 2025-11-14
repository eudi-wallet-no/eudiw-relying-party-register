package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialMetadata(
    @NotNull(message = "null_display")
    @JsonProperty(value = "display", required = true)
    List<@Valid Display> credentialTypeDisplays,

    @NotNull(message = "null_claims")
    @JsonProperty(value = "claims", required = true)
    List<@Valid Claims> claims
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Claims(
        @NotEmpty
        @Valid
        @JsonProperty(value = "path", required = true)
        List<@NotBlank String> paths,

        @NotEmpty
        @Valid
        @JsonProperty(value = "display", required = true)
        List<@Valid Display> displays
    ) {
        public String getClaimsDisplay(String locale) {
            return Display.getDisplayForLocale(locale, this.displays);
        }
        public String getDcqlFormattedPaths() {
            return "[%s]".formatted(
                paths.stream()
                     .map("\"%s\""::formatted)
                     .collect(Collectors.joining(", ")));
        }
    }

    public String getCredentialTypeDisplay(String locale) {
        return Display.getDisplayForLocale(locale, this.credentialTypeDisplays);
    }
}
