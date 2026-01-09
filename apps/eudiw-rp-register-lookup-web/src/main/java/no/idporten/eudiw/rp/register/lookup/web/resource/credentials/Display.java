package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public record Display(
    @NotBlank
    @JsonProperty(value = "name", required = true)
    String name,
    @NotBlank
    @JsonProperty(value = "locale", required = true)
    String locale,
    @JsonProperty(value = "description")
    String description
) {
    @Nullable
    public static String getDescriptionForLocale(String locale, List<Display> displays) {
        List<Display> displaysWithDescription =
            displays.stream()
                    .filter(d -> d.description != null)
                    .sorted((d1, _) -> !d1.locale().equalsIgnoreCase(locale) ? 1 : -1)
                    .toList();

        if (displaysWithDescription.isEmpty()) {
            log.info("No description found (for locale \"{}\" nor fallback)", locale);
            return null;
        }

        Display display = displaysWithDescription.getFirst();
        if (!display.locale().equalsIgnoreCase(locale)) {
            log.info("No description found for locale \"{}\", using fallback locale \"{}\".",
                     locale,
                     display.locale());
        }
        return display.description();
    }

    public static String getDisplayNameForLocale(String locale, List<Display> displays) {
        Optional<Display> displayWithDesiredLocale =
            displays.stream()
                    .filter(d -> d.locale().equalsIgnoreCase(locale))
                    .findFirst();

        if (displayWithDesiredLocale.isEmpty()) {
            Display fallbackDisplay = displays.getFirst();
            log.info("No display name found for locale \"{}\", using fallback locale \"{}\".",
                     locale,
                     fallbackDisplay.locale());
            return fallbackDisplay.name();
        }
        return displayWithDesiredLocale.get().name();
    }
}
