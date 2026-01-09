package no.idporten.eudiw.rp.register.lookup.display;

import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.Display;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When using the Display class for display names and descriptions")
public class DisplayTests {

    private static List<Display> SAMPLE_DISPLAYS = List.of(
        new Display("name-en", "en", null),
        new Display("name-es", "es", "desc-es"),
        new Display("name-no", "no", "desc-no"),
        new Display("name-ru", "ru", null),
        new Display("name-da", "da", "desc-da")
    );

    @Nested
    @DisplayName("when getting display names")
    class GetDisplayNameTests {

        @Test
        @DisplayName("then display name for the desired locale is returned if one such exists")
        public void testGetDisplayNameForExistingLocale() {
            assertEquals("name-no", Display.getDisplayNameForLocale("no", SAMPLE_DISPLAYS));
        }

        @Test
        @DisplayName("then the first available display name is returned if desired locale does not exist")
        public void testGetDisplayNameForNonExistentLocale() {
            String firstAvailableDisplayName = "name-en";
            String nonexistentLocale = "se";
            assertEquals(firstAvailableDisplayName, Display.getDisplayNameForLocale(nonexistentLocale, SAMPLE_DISPLAYS));
        }
    }

    @Nested
    @DisplayName("when getting display descriptions")
    class GetDisplayDescriptionTests {

        @Test
        @DisplayName("then description for the desired locale is returned if one such exists")
        public void testGetDescriptionForExistingLocale() {
            assertEquals("desc-no", Display.getDescriptionForLocale("no", SAMPLE_DISPLAYS));
        }

        @Test
        @DisplayName("then the first available description is returned if desired locale does not exist")
        public void testGetDescriptionForNonExistentLocale() {
            String nonexistentLocale = "se";
            String firstAvailableNonNullDescription = "desc-es";
            assertEquals(firstAvailableNonNullDescription,
                         Display.getDescriptionForLocale(nonexistentLocale, SAMPLE_DISPLAYS));
        }

        @Test
        @DisplayName("then a null description is returned when no descriptions exist")
        public void testGetDescriptionDoesNotFailWhenNoDescriptionsExist() {
            List<Display> displaysWithNoDescription = List.of(
                new Display("name-en", "en", null),
                new Display("name-es", "es", null),
                new Display("name-no", "no", null)
            );
            String description = assertDoesNotThrow(
                () -> Display.getDescriptionForLocale("no", displaysWithNoDescription));
            assertNull(description);
        }

    }

}
