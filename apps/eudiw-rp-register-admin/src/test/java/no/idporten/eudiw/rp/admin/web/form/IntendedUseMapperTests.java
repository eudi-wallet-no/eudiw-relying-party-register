package no.idporten.eudiw.rp.admin.web.form;

import no.idporten.eudiw.rp.admin.integrations.issuermetadata.IssuerMetadataService;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntendedUseMapperTests {

    @Test
    @DisplayName("Should build credential options sorted alphabetically with Norwegian collation")
    void shouldBuildCredentialOptionsSortedAlphabeticallyWithNorwegianCollation() {
        var definitions = List.of(
            new IssuerMetadataService.Definition(
                "cred-aa", "Årskort", "urn:example:aa",
                List.of(new IssuerMetadataService.Claim("Gyldig til", List.of("valid_until"))), null),
            new IssuerMetadataService.Definition(
                "cred-z", "Zebra-bevis", "urn:example:z",
                List.of(new IssuerMetadataService.Claim("Kode", List.of("code"))), null),
            new IssuerMetadataService.Definition(
                "cred-oe", "Øvingsbevis", "urn:example:oe",
                List.of(), "Bevisformatet kan ikkje veljast enno."),
            new IssuerMetadataService.Definition(
                "cred-ae", "Æresbevis", "urn:example:ae",
                List.of(
                    new IssuerMetadataService.Claim("Namn", List.of("name")),
                    new IssuerMetadataService.Claim("Alder", List.of("age"))), null),
            new IssuerMetadataService.Definition(
                "cred-a", "Aldersbevis", "urn:example:a",
                List.of(new IssuerMetadataService.Claim("Over 18", List.of("age_over_18"))), null));

        var options = IntendedUseMapper.createCredentialOptions(definitions);

        assertEquals(
            List.of("Aldersbevis", "Zebra-bevis", "Æresbevis", "Øvingsbevis", "Årskort"),
            options.stream().map(IntendedUseMapper.CredentialOption::label).toList());
        assertEquals(List.of(0, 1, 2, 3, 4),
            options.stream().map(IntendedUseMapper.CredentialOption::index).toList());
        assertEquals("Bevisformatet kan ikkje veljast enno.", options.get(3).error());
        assertEquals(
            List.of(
                new IntendedUseMapper.ClaimOption(0, "Namn", false),
                new IntendedUseMapper.ClaimOption(1, "Alder", false)),
            options.get(2).claims());
    }

    @Test
    @DisplayName("Should map validated form and selected SD-JWT claims to create request")
    void shouldMapValidatedFormAndSelectedSdJwtClaimsToCreateRequest() {
        List<Object> complexPath = Arrays.asList("address", BigInteger.ZERO, null);
        var definitions = List.of(
            new IssuerMetadataService.Definition(
                "pid",
                "Personidentifikator",
                "urn:eudi:pid:1",
                List.of(
                    new IssuerMetadataService.Claim("Fornamn", List.of("given_name")),
                    new IssuerMetadataService.Claim("Etternamn", List.of("family_name")),
                    new IssuerMetadataService.Claim("Adresse", complexPath)),
                null),
            new IssuerMetadataService.Definition(
                "mdoc",
                "Førarkort",
                "",
                List.of(),
                "Bevisformatet kan ikkje veljast enno."));

        var form = new IntendedUseForm();
        form.setPurpose(List.of(
            new IntendedUseForm.Purpose("nb", "Verifisere identitet"),
            new IntendedUseForm.Purpose("en", "Verify identity")));
        form.setPrivacyPolicyUri("  https://example.org/privacy  ");
        form.setCredentialSelections(List.of(
            new IntendedUseForm.CredentialSelection("", List.of()),
            new IntendedUseForm.CredentialSelection("pid", List.of(
                new IntendedUseForm.ClaimSelection(0, true),
                new IntendedUseForm.ClaimSelection(1, false),
                new IntendedUseForm.ClaimSelection(2, true)))));

        var result = IntendedUseMapper.toCreateRequest(form, definitions);

        assertFalse(result.hasError());
        assertEquals(
            new V2RegisterClient.CreateIntendedUseRequest(
                List.of(
                    new V2RegisterClient.IntendedUsePurpose("nb", "Verifisere identitet"),
                    new V2RegisterClient.IntendedUsePurpose("en", "Verify identity")),
                List.of(new V2RegisterClient.IntendedUsePolicy(
                    "http://data.europa.eu/eudi/policy/privacy-statement",
                    "https://example.org/privacy")),
                List.of(new V2RegisterClient.IntendedUseCredential(
                    "dc+sd-jwt",
                    "Personidentifikator",
                    Map.of("vct_values", List.of("urn:eudi:pid:1")),
                    List.of(
                        Map.of("path", List.of("given_name")),
                        Map.of("path", complexPath))))),
            result.request());
    }

    @Test
    @DisplayName("Should reject mapping when credential selection is empty, stale, or unsupported")
    void shouldRejectMappingWhenCredentialSelectionIsEmptyStaleOrUnsupported() {
        var definitions = List.of(new IssuerMetadataService.Definition(
            "mdoc", "Førarkort", "", List.of(), "Bevisformatet kan ikkje veljast enno."));

        var emptyForm = formWithSelection("  ", List.of());
        var staleForm = formWithSelection("unknown", List.of(new IntendedUseForm.ClaimSelection(0, true)));
        var unsupportedForm = formWithSelection("mdoc", List.of(new IntendedUseForm.ClaimSelection(0, true)));

        var emptyResult = IntendedUseMapper.toCreateRequest(emptyForm, definitions);
        var staleResult = IntendedUseMapper.toCreateRequest(staleForm, definitions);
        var unsupportedResult = IntendedUseMapper.toCreateRequest(unsupportedForm, definitions);

        assertEquals("required", emptyResult.errorCode());
        assertNull(emptyResult.request());
        assertEquals("stale", staleResult.errorCode());
        assertNull(staleResult.request());
        assertEquals("unsupported", unsupportedResult.errorCode());
        assertNull(unsupportedResult.request());
    }

    @Test
    @DisplayName("Should reject mapping when selected claims are missing, out of bounds, or duplicated")
    void shouldRejectMappingWhenSelectedClaimsAreMissingOutOfBoundsOrDuplicated() {
        var definitions = List.of(new IssuerMetadataService.Definition(
            "pid", "Personidentifikator", "urn:eudi:pid:1",
            List.of(new IssuerMetadataService.Claim("Fornamn", List.of("given_name"))), null));

        var noClaimsResult = IntendedUseMapper.toCreateRequest(
            formWithSelection("pid", List.of(new IntendedUseForm.ClaimSelection(0, false))), definitions);
        var outOfBoundsResult = IntendedUseMapper.toCreateRequest(
            formWithSelection("pid", List.of(new IntendedUseForm.ClaimSelection(1, true))), definitions);
        var duplicateClaimResult = IntendedUseMapper.toCreateRequest(
            formWithSelection("pid", List.of(
                new IntendedUseForm.ClaimSelection(0, true),
                new IntendedUseForm.ClaimSelection(0, true))),
            definitions);

        assertTrue(noClaimsResult.hasError());
        assertEquals("claimsRequired", noClaimsResult.errorCode());
        assertEquals("invalidClaims", outOfBoundsResult.errorCode());
        assertEquals("invalidClaims", duplicateClaimResult.errorCode());
    }

    private static IntendedUseForm formWithSelection(
        String credentialId,
        List<IntendedUseForm.ClaimSelection> claims
    ) {
        var form = new IntendedUseForm();
        form.setPurpose(List.of(new IntendedUseForm.Purpose("nb", "Verifisere identitet")));
        form.setPrivacyPolicyUri("https://example.org/privacy");
        form.setCredentialSelections(List.of(new IntendedUseForm.CredentialSelection(credentialId, claims)));
        return form;
    }
}
