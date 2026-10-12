package no.eudiw.rp.register.data.service.walletrelyingpartylookupservice;

import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.service.WalletRelyingPartyLookupService;
import no.eudiw.rp.register.integrations.enhetsregisteret.EnhetsregisteretService;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("when using the legal entity service")
@ActiveProfiles("junit")
public class WalletRelyingPartyLookupServiceTests {

    @Autowired
    private WalletRelyingPartyLookupService walletRelyingPartyLookupService;

    @MockitoBean
    @SuppressWarnings("unused")
    private EnhetsregisteretService mockEnhetsregisteretService;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    @BeforeEach
    public void initializeMockBeans() {
        when(mockEnhetsregisteretService.queryOrgno(any())).thenReturn(
                new EnhetsregisteretService.EnhetsregisteretResponse(
                    TestDataGenerator.generateName(),
                    TestDataGenerator.generatePublicSector()));
    }

    @BeforeEach
    public void clearWalletRelyingPartyRepoBeforeEachTest() {
        walletRelyingPartyRepository.deleteAll();
    }

    @Test
    @DisplayName("then querying a non-existent legal entity auto-registers a new one")
    public void testQueryingUnknownWalletRelyingPartyCreatesANewOne() {
        System.out.println(walletRelyingPartyRepository.findAll());

        String orgno = TestDataGenerator.generateValidOrgno();
        assertFalse(walletRelyingPartyRepository.existsByOrgno(orgno));

        walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno(orgno);

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        assertTrue(walletRelyingPartyRepository.existsByOrgno(orgno));
    }

    @Test
    @DisplayName("then querying same orgno many times gives same legal entity and Enhetsregisteret queried only once")
    public void testQueryingSameOrgnoMultipleTimesGivesSameWalletRelyingParty() {

        String orgno = TestDataGenerator.generateValidOrgno();
        assertFalse(walletRelyingPartyRepository.existsByOrgno(orgno));

        WalletRelyingParty walletRelyingParty1 = walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno(orgno);
        WalletRelyingParty walletRelyingParty2 = walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno(orgno);

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        assertTrue(walletRelyingPartyRepository.existsByOrgno(orgno));
        assertEquals(walletRelyingParty1, walletRelyingParty2);
    }

    @Test
    @DisplayName("then querying distinct orgnos give distinct legal entities and distinct Enhetsregisteret queries")
    public void testDistinctQueryingOrgnosGivesDistinctWalletRelyingPartiesAndDistinctEnhetsregisteretQueries() {

        String orgno = TestDataGenerator.generateValidOrgno();
        String otherOrgno = TestDataGenerator.generateValidOrgno();
        assertFalse(walletRelyingPartyRepository.existsByOrgno(orgno));
        assertFalse(walletRelyingPartyRepository.existsByOrgno(otherOrgno));

        WalletRelyingParty walletRelyingParty = walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno(orgno);
        WalletRelyingParty otherWalletRelyingParty = walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno(otherOrgno);

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verify(mockEnhetsregisteretService, times(1)).queryOrgno(otherOrgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        assertTrue(walletRelyingPartyRepository.existsByOrgno(orgno));
        assertTrue(walletRelyingPartyRepository.existsByOrgno(otherOrgno));
        assertNotEquals(otherWalletRelyingParty, walletRelyingParty);
    }

    @Test
    @DisplayName("then querying with a synthetic orgno does not trigger Enhetsregisteret query")
    public void testQueryingWithSyntheticOrgnoShouldNotQueryEnhetsregisteret() {
        String syntheticOrgno = "316256139";
        assertFalse(walletRelyingPartyRepository.existsByOrgno(syntheticOrgno));

         walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno(syntheticOrgno);

        verifyNoInteractions(mockEnhetsregisteretService);
        assertTrue(walletRelyingPartyRepository.existsByOrgno(syntheticOrgno));
    }
}
