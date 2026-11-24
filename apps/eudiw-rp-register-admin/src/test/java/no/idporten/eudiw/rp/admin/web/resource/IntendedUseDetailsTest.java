package no.idporten.eudiw.rp.admin.web.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IntendedUseDetailsTest {

    @Test
    @DisplayName("Should use stored credential names when some credentials have no name")
    void shouldUseStoredCredentialNamesWhenSomeCredentialsHaveNoName() {
        var intendedUse = intendedUse(List.of(
            credential("Personal ID"), credential(null), credential("Mobile driving licence")));

        assertEquals("Personal ID, Mobile driving licence", intendedUse.getDisplayLabel());
        assertEquals("pid-verification", intendedUse.intendedUseIdentifier());
    }

    @Test
    @DisplayName("Should use the intended-use identifier when all credential names are missing or blank")
    void shouldUseIntendedUseIdentifierWhenAllCredentialNamesAreMissingOrBlank() {
        var intendedUse = intendedUse(List.of(credential(null), credential(""), credential(" \t")));

        assertEquals("pid-verification", intendedUse.getDisplayLabel());
    }

    @Test
    @DisplayName("Should use the intended-use identifier when credentials are absent or empty")
    void shouldUseIntendedUseIdentifierWhenCredentialsAreAbsentOrEmpty() {
        assertEquals("pid-verification", intendedUse(null).getDisplayLabel());
        assertEquals("pid-verification", intendedUse(List.of()).getDisplayLabel());
    }

    @Test
    @DisplayName("Should leave the display label absent when names and the intended-use identifier are absent")
    void shouldLeaveDisplayLabelAbsentWhenNamesAndIntendedUseIdentifierAreAbsent() {
        var intendedUse = new RelyingPartyResource.IntendedUseDetails(
            UUID.randomUUID(), null, null, null, List.of(), List.of(), List.of());

        assertNull(intendedUse.getDisplayLabel());
    }

    private static RelyingPartyResource.IntendedUseDetails intendedUse(
        List<RelyingPartyResource.CredentialDetails> credentials
    ) {
        return new RelyingPartyResource.IntendedUseDetails(
            UUID.randomUUID(), "pid-verification", null, null, List.of(), List.of(), credentials);
    }

    private static RelyingPartyResource.CredentialDetails credential(String displayName) {
        return new RelyingPartyResource.CredentialDetails(
            "dc+sd-jwt", Map.of("vct_values", List.of("urn:example:pid")), List.of(), displayName);
    }
}
