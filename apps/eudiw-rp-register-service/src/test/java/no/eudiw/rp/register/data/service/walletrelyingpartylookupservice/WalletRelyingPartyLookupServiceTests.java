package no.eudiw.rp.register.data.service.walletrelyingpartylookupservice;

import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
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

import java.util.List;

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

    @Autowired
    private SupervisoryAuthorityProperties supervisoryAuthorityProperties;

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
    @DisplayName("then creating a legal entity registers a new one")
    public void testCreatingWalletRelyingPartyRegistersANewOne() {
        String orgno = TestDataGenerator.generateValidOrgno();
        assertFalse(walletRelyingPartyRepository.existsByOrgno(orgno));

        walletRelyingPartyLookupService.createWalletRelyingParty(orgno);

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        WalletRelyingParty created = walletRelyingPartyRepository.findByOrgno(orgno).orElseThrow();
        assertAll(
            () -> assertEquals("NO", created.getCountry()),
            () -> assertEquals(WalletRelyingParty.PROVIDER_TYPE, created.getProviderType()),
            () -> assertNotNull(created.getTradeName()),
            () -> assertEquals(List.of(created.getTradeName()), created.getLegalPerson().getLegalName()),
            () -> assertEquals("Datatilsynet", supervisoryAuthorityProperties.name()),
            () -> assertEquals("NO", supervisoryAuthorityProperties.country())
        );
    }

    @Test
    @DisplayName("then querying same orgno many times gives same legal entity and Enhetsregisteret queried only once")
    public void testQueryingSameOrgnoMultipleTimesGivesSameWalletRelyingParty() {

        String orgno = TestDataGenerator.generateValidOrgno();
        assertFalse(walletRelyingPartyRepository.existsByOrgno(orgno));

        WalletRelyingParty walletRelyingParty1 = walletRelyingPartyLookupService.createWalletRelyingParty(orgno);
        WalletRelyingParty walletRelyingParty2 =
            walletRelyingPartyLookupService.findWalletRelyingPartyForOrgno(orgno).orElseThrow();

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        assertTrue(walletRelyingPartyRepository.existsByOrgno(orgno));
        assertEquals(walletRelyingParty1, walletRelyingParty2);
    }

    @Test
    @DisplayName("then creating distinct orgnos gives distinct legal entities and distinct Enhetsregisteret queries")
    public void testCreatingDistinctOrgnosGivesDistinctWalletRelyingPartiesAndDistinctEnhetsregisteretQueries() {

        String orgno = TestDataGenerator.generateValidOrgno();
        String otherOrgno = TestDataGenerator.generateValidOrgno();
        assertFalse(walletRelyingPartyRepository.existsByOrgno(orgno));
        assertFalse(walletRelyingPartyRepository.existsByOrgno(otherOrgno));

        WalletRelyingParty walletRelyingParty = walletRelyingPartyLookupService.createWalletRelyingParty(orgno);
        WalletRelyingParty otherWalletRelyingParty = walletRelyingPartyLookupService.createWalletRelyingParty(otherOrgno);

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verify(mockEnhetsregisteretService, times(1)).queryOrgno(otherOrgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        assertTrue(walletRelyingPartyRepository.existsByOrgno(orgno));
        assertTrue(walletRelyingPartyRepository.existsByOrgno(otherOrgno));
        assertNotEquals(otherWalletRelyingParty, walletRelyingParty);
    }

    @Test
    @DisplayName("then creating with a synthetic orgno does not trigger Enhetsregisteret query")
    public void testCreatingWithSyntheticOrgnoShouldNotQueryEnhetsregisteret() {
        String syntheticOrgno = "316256139";
        assertFalse(walletRelyingPartyRepository.existsByOrgno(syntheticOrgno));

        walletRelyingPartyLookupService.createWalletRelyingParty(syntheticOrgno);

        verifyNoInteractions(mockEnhetsregisteretService);
        assertTrue(walletRelyingPartyRepository.existsByOrgno(syntheticOrgno));
    }

}
