package no.idporten.eudiw.rp.admin.web.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntendedUseDetailsTest {

    @Test
    @DisplayName("Should show named credentials in the intended-use label when some credentials have no display name")
    void shouldShowNamedCredentialsInTheIntendedUseLabelWhenSomeCredentialsHaveNoDisplayName() {
        var intendedUse = intendedUse(List.of(
            credential("Personal ID"), credential(null), credential(" "), credential("Mobile driving licence")));

        assertEquals("Personal ID, Mobile driving licence", intendedUse.getDisplayLabel());
    }

    @Test
    @DisplayName("Should fall back to the intended-use identifier when credentials have no display name")
    void shouldFallBackToTheIntendedUseIdentifierWhenCredentialsHaveNoDisplayName() {
        var intendedUse = intendedUse(List.of(credential(null), credential(" ")));

        assertEquals("pid-verification", intendedUse.getDisplayLabel());
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
