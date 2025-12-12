package no.eudiw.rp.register.data.service.legalentityservice;

import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.data.repository.LegalEntityRepository;
import no.eudiw.rp.register.service.LegalEntityService;
import no.eudiw.rp.register.service.enhetsregisteretservice.EnhetsregisteretService;
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
public class LegalEntityServiceTests {

    @Autowired
    private LegalEntityService legalEntityService;

    @MockitoBean
    @SuppressWarnings("unused")
    private EnhetsregisteretService mockEnhetsregisteretService;

    @Autowired
    private LegalEntityRepository legalEntityRepository;

    @BeforeEach
    public void initializeMockBeans() {
        when(mockEnhetsregisteretService.queryOrgno(any())).thenReturn(
                new EnhetsregisteretService.EnhetsregisteretResponse(
                    TestDataGenerator.generateName(),
                    TestDataGenerator.generatePublicSector()));
    }

    @BeforeEach
    public void clearLegalEntityRepoBeforeEachTest() {
        legalEntityRepository.deleteAll();
    }

    @Test
    @DisplayName("then querying a non-existent legal entity auto-registers a new one")
    public void testQueryingUnknownLegalEntityCreatesANewOne() {
        System.out.println(legalEntityRepository.findAll());

        String orgno = TestDataGenerator.generateValidOrgno();
        assertFalse(legalEntityRepository.existsByOrgno(orgno));

        legalEntityService.getLegalEntityForOrgno(orgno);

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        assertTrue(legalEntityRepository.existsByOrgno(orgno));
    }

    @Test
    @DisplayName("then querying same orgno many times gives same legal entity and Enhetsregisteret queried only once")
    public void testQueryingSameOrgnoMultipleTimesGivesSameLegalEntity() {

        String orgno = TestDataGenerator.generateValidOrgno();
        assertFalse(legalEntityRepository.existsByOrgno(orgno));

        LegalEntity legalEntity1 = legalEntityService.getLegalEntityForOrgno(orgno);
        LegalEntity legalEntity2 = legalEntityService.getLegalEntityForOrgno(orgno);

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        assertTrue(legalEntityRepository.existsByOrgno(orgno));
        assertEquals(legalEntity1, legalEntity2);
    }

    @Test
    @DisplayName("then querying distinct orgnos give distinct legal entities and distinct Enhetsregisteret queries")
    public void testDistinctQueryingOrgnosGivesDistinctLegalEntitiesAndDistinctEnhetsregisteretQueries() {

        String orgno = TestDataGenerator.generateValidOrgno();
        String otherOrgno = TestDataGenerator.generateValidOrgno();
        assertFalse(legalEntityRepository.existsByOrgno(orgno));
        assertFalse(legalEntityRepository.existsByOrgno(otherOrgno));

        LegalEntity legalEntity = legalEntityService.getLegalEntityForOrgno(orgno);
        LegalEntity otherLegalEntity = legalEntityService.getLegalEntityForOrgno(otherOrgno);

        verify(mockEnhetsregisteretService, times(1)).queryOrgno(orgno);
        verify(mockEnhetsregisteretService, times(1)).queryOrgno(otherOrgno);
        verifyNoMoreInteractions(mockEnhetsregisteretService);

        assertTrue(legalEntityRepository.existsByOrgno(orgno));
        assertTrue(legalEntityRepository.existsByOrgno(otherOrgno));
        assertNotEquals(otherLegalEntity, legalEntity);
    }

    @Test
    @DisplayName("then querying with a synthetic orgno does not trigger Enhetsregisteret query")
    public void testQueryingWithSyntheticOrgnoShouldNotQueryEnhetsregisteret() {
        String syntheticOrgno = "316256139";
        assertFalse(legalEntityRepository.existsByOrgno(syntheticOrgno));

         legalEntityService.getLegalEntityForOrgno(syntheticOrgno);

        verifyNoInteractions(mockEnhetsregisteretService);
        assertTrue(legalEntityRepository.existsByOrgno(syntheticOrgno));
    }
}
