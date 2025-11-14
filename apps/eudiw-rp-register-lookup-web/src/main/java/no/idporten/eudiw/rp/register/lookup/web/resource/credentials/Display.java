package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public record Display(
    @NotBlank
    @JsonProperty(value = "name", required = true)
    String name,
    @NotBlank
    @JsonProperty(value = "locale", required = true)
    String locale
) {
    public static String getDisplayForLocale(String locale, List<Display> displays) {
        Optional<String> displayName =
            displays.stream()
                    .filter(display -> display.locale().equalsIgnoreCase(locale))
                    .findFirst()
                    .map(Display::name);
        if (displayName.isEmpty()) {
            log.warn("No display-name found for locale {}, and no default locale set.", locale);
            return "";
        }
        return displayName.get();
    }
}
