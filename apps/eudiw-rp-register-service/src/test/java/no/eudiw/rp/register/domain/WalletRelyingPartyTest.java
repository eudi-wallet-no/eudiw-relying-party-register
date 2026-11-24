package no.eudiw.rp.register.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
