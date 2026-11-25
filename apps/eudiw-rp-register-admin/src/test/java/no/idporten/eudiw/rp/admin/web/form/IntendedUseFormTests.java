package no.idporten.eudiw.rp.admin.web.form;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntendedUseFormTests {

    @Test
    @DisplayName("Should add default Norwegian Bokmal purpose row only when purpose list is empty")
    void shouldAddDefaultNorwegianBokmalPurposeRowOnlyWhenPurposeListIsEmpty() {
        var emptyForm = new IntendedUseForm();
        emptyForm.addDefaultPurposeRow();

        var populatedForm = new IntendedUseForm();
        populatedForm.setPurpose(new ArrayList<>(List.of(new IntendedUseForm.Purpose("nn", "Alderskontroll"))));
        populatedForm.addDefaultPurposeRow();

        assertEquals(1, emptyForm.getPurpose().size());
        assertEquals("nb", emptyForm.getPurpose().getFirst().getLang());
        assertNull(emptyForm.getPurpose().getFirst().getContent());
        assertEquals("nn", populatedForm.getPurpose().getFirst().getLang());
        assertEquals("Alderskontroll", populatedForm.getPurpose().getFirst().getContent());
    }

    @Test
    @DisplayName("Should validate purpose rows for completeness, BCP 47 language tags, and case-insensitive uniqueness")
    void shouldValidatePurposeRowsForCompletenessBcp47LanguageTagsAndCaseInsensitiveUniqueness() {
        var validForm = new IntendedUseForm();
        validForm.setPurpose(List.of(
            new IntendedUseForm.Purpose("nb", "Alderskontroll"),
            new IntendedUseForm.Purpose("en-US", "Age verification")));

        var incompleteForm = new IntendedUseForm();
        incompleteForm.setPurpose(List.of(
            new IntendedUseForm.Purpose("nb", "Alderskontroll"),
            new IntendedUseForm.Purpose("", "Age verification")));

        var invalidLangForm = new IntendedUseForm();
        invalidLangForm.setPurpose(List.of(new IntendedUseForm.Purpose("norsk", "Alderskontroll")));

        var duplicateLangForm = new IntendedUseForm();
        duplicateLangForm.setPurpose(List.of(
            new IntendedUseForm.Purpose("nb", "Første føremål"),
            new IntendedUseForm.Purpose("NB", "Andre føremål")));

        assertTrue(validForm.hasCompletePurposes());
        assertTrue(validForm.hasValidPurposeLanguages());
        assertTrue(validForm.hasUniquePurposeLanguages());

        assertFalse(new IntendedUseForm().hasCompletePurposes());
        assertFalse(incompleteForm.hasCompletePurposes());
        assertFalse(invalidLangForm.hasValidPurposeLanguages());
        assertFalse(duplicateLangForm.hasUniquePurposeLanguages());
    }

    @Test
    @DisplayName("Should accept absolute privacy policy URIs and reject missing, relative, or malformed values")
    void shouldAcceptAbsolutePrivacyPolicyUrisAndRejectMissingRelativeOrMalformedValues() {
        var validForm = new IntendedUseForm();
        validForm.setPrivacyPolicyUri("  https://example.org/privacy  ");
        assertTrue(validForm.hasValidPrivacyPolicyUri());

        assertFalse(new IntendedUseForm().hasValidPrivacyPolicyUri());
        for (String invalidUri : List.of("   ", "/privacy", "https://example.org/invalid uri")) {
            var invalidForm = new IntendedUseForm();
            invalidForm.setPrivacyPolicyUri(invalidUri);
            assertFalse(invalidForm.hasValidPrivacyPolicyUri(), "Expected invalid URI: " + invalidUri);
        }
    }
}
