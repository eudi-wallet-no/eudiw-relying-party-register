package no.idporten.eudiw.rp.admin.web.form;

import no.idporten.eudiw.rp.admin.integrations.issuermetadata.IssuerMetadataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
