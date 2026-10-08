package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
        private static final JsonMapper PATH_MAPPER = JsonMapper.builder().build();
        @NotEmpty
        @JsonProperty(value = "path", required = true)
        private List<Object> paths;

        @JsonProperty("display")
        private List<@NotNull @Valid Display> displays = new ArrayList<>();

        public String getClaimsDisplayName(String locale) {
            return Display.getDisplayNameForLocale(locale, this.displays, null);
        }
        public String getDcqlFormattedPaths() {
            return PATH_MAPPER.writeValueAsString(paths);
        }

        @JsonIgnore
        @AssertTrue(message = "claim path must contain only strings, null, or non-negative integers")
        public boolean isValidPaths() {
            return paths == null || paths.stream().allMatch(element ->
                    element == null || element instanceof String
                            || ((element instanceof Byte || element instanceof Short
                                 || element instanceof Integer || element instanceof Long
                                 || element instanceof BigInteger)
                                && new BigInteger(element.toString()).signum() >= 0));
        }
    }
}
