package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialMetadata {
    @JsonProperty("display")
    private List<@NotNull @Valid Display> credentialTypeDisplays = new ArrayList<>();

    @JsonProperty("claims")
    private List<@NotNull @Valid Claims> claims = new ArrayList<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Claims {
        @NotEmpty
        @JsonProperty(value = "path", required = true)
        private List<String> paths;

        @JsonProperty("display")
        private List<@NotNull @Valid Display> displays = new ArrayList<>();

        public String getClaimsDisplayName(String locale) {
            return Display.getDisplayNameForLocale(locale, this.displays, null);
        }
        public String getDcqlFormattedPaths() {
            return "[%s]".formatted(
                paths.stream()
                     .map(path -> path == null ? "null" : "\"%s\"".formatted(path))
                     .collect(Collectors.joining(", ")));
        }
    }
}
