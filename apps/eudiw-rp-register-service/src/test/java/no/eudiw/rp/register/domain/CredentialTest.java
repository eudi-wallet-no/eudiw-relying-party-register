package no.eudiw.rp.register.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialTest {

    @Test
    void acceptsBothSupportedMetaShapesAndCopiesInputAndOutput() {
        List<String> vctValues = new ArrayList<>(List.of("urn:example:pid"));
        Map<String, Object> sdJwtMeta = new LinkedHashMap<>();
        sdJwtMeta.put("vct_values", vctValues);
        List<Map<String, Object>> claims = new ArrayList<>(claims("namespace", null, 0));
        Credential sdJwtCredential = new Credential("dc+sd-jwt", sdJwtMeta, claims);
        Credential mdocCredential = new Credential(
            "mso_mdoc",
            meta("doctype", "org.iso.18013.5.1.mDL"),
            claims("org.iso.18013.5.1", "given_name")
        );

        vctValues.add("urn:example:changed");
        claims.getFirst().put("path", List.of("changed"));
        sdJwtCredential.getMeta().put("doctype", "changed");

        assertEquals(meta("vct_values", List.of("urn:example:pid")), sdJwtCredential.getMeta());
        assertEquals(claims("namespace", null, 0), sdJwtCredential.getClaims());
        assertEquals(meta("doctype", "org.iso.18013.5.1.mDL"), mdocCredential.getMeta());
    }

    @Test
    void rejectsMetaWithoutExactlyOneSupportedVariant() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", Map.of(), claims("claim")));
        assertThrows(IllegalArgumentException.class, () ->
            new Credential(
                "format",
                Map.of("vct_values", List.of("type"), "doctype", "type"),
                claims("claim")
            ));
    }

    @Test
    void rejectsInvalidMetaVariantValues() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", meta("vct_values", List.of(1)), claims("claim")));
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", meta("vct_values", List.of()), claims("claim")));
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", meta("doctype", 1), claims("claim")));
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", Map.of("doctype", "type", "extra", "value"), claims("claim")));
    }

    @Test
    void validatesClaimsPaths() {
        assertDoesNotThrow(() -> new Credential(
            "format",
            meta("doctype", "org.iso.18013.5.1.mDL"),
            claims("namespace", null, 0)
        ));
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", meta("doctype", "type"), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", meta("doctype", "type"), claims()));
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", meta("doctype", "type"), claims(-1)));
        assertThrows(IllegalArgumentException.class, () ->
            new Credential("format", meta("doctype", "type"), claims(1.5)));
    }

    private static Map<String, Object> meta(String name, Object value) {
        return Map.of(name, value);
    }

    private static List<Map<String, Object>> claims(Object... pathParts) {
        Map<String, Object> claim = new LinkedHashMap<>();
        claim.put("path", Arrays.asList(pathParts));
        return List.of(claim);
    }
}
