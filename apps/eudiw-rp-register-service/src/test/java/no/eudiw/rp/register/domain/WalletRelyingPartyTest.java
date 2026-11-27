package no.eudiw.rp.register.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletRelyingPartyTest {

    @Test
    void newPartyUsesTheNameAsBothTradeNameAndLegalName() {
        var party = new WalletRelyingParty("Name", "123456785", true, List.of());

        assertEquals("Name", party.getTradeName());
        assertEquals(List.of("Name"), party.getLegalPerson().getLegalName());
    }

    @Test
    @DisplayName("Sets the creation date on persist and leaves the revocation date unset")
    void initializesLifecycleDatesOnPersist() {
        var party = new WalletRelyingParty("Name", "123456785", true, List.of());

        party.onPrePersist();

        assertEquals(LocalDate.now(ZoneOffset.UTC), party.getCreatedAt());
        assertNull(party.getRevokedAt());
    }

    @Test
    @DisplayName("Should record the UTC date when a party is revoked")
    void shouldRecordTheUtcDateWhenAPartyIsRevoked() {
        var party = new WalletRelyingParty("Name", "123456785", true, List.of());
        var before = LocalDate.now(ZoneOffset.UTC);

        party.revoke();

        var after = LocalDate.now(ZoneOffset.UTC);
        assertTrue(party.getRevokedAt().equals(before) || party.getRevokedAt().equals(after));
        assertTrue(party.isActive());
    }

    @Test
    @DisplayName("Should preserve the revocation date when a party is revoked again")
    void shouldPreserveTheRevocationDateWhenAPartyIsRevokedAgain() {
        var party = new WalletRelyingParty("Name", "123456785", true, List.of());
        var originalDate = LocalDate.of(2024, 1, 1);
        ReflectionTestUtils.setField(party, "revokedAt", originalDate);

        party.revoke();

        assertEquals(originalDate, party.getRevokedAt());
    }

    @Test
    void setNameReplacesBothTradeNameAndLegalName() {
        var party = new WalletRelyingParty("Old", "123456785", true, List.of());

        party.setName("New");

        assertEquals("New", party.getTradeName());
        assertEquals(List.of("New"), party.getLegalPerson().getLegalName());
    }

    @Test
    void tradeNameOrLegalNameFallsBackToTheFirstLegalName() {
        var party = new WalletRelyingParty("Name", "123456785", true, List.of());
        assertEquals("Name", party.getName());

        party.setTradeName(null);
        assertEquals("Name", party.getName());

        party.setLegalPerson(new LegalPerson(List.of(), List.of()));
        assertNull(party.getName());
    }

    @Test
    void policyTypeProvidesTermsAndConditionsLabel() {
        assertEquals("Terms and Conditions", PolicyType.TERMS_AND_CONDITIONS.getLabel());
    }
}
