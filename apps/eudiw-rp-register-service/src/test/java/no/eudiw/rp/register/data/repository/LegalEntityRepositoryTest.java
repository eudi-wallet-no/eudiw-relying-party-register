package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.entity.RelyingPartyInstance;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("When using LegalEntityRepository")
@ActiveProfiles("junit")
public class LegalEntityRepositoryTest {

    @Autowired
    private LegalEntityRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("store and get")
    void storeAndGet() {
        LegalEntity rp = EntityGenerator.generateLegalEntity();

        LegalEntity returnFromSave = repository.saveAndFlush(rp);

        LegalEntity actual = repository.findById(returnFromSave.getId()).get();

        assert actual.equals(returnFromSave);
    }

    @Test
    @DisplayName("orgno when null")
    void getOrgnoNull() {
        LegalEntity actual = repository.findByOrgno("9876").orElse(new LegalEntity("name", "orgno", true, new ArrayList<>()));
        assertNotNull(actual);
        assertEquals(actual.getName(), "name");
    }

    @Test
    @DisplayName("orgno when exists")
    void getOrgnoExists() {
        LegalEntity rp = EntityGenerator.generateLegalEntity();
        repository.saveAndFlush(rp);
        LegalEntity actual = repository.findByOrgno(rp.getOrgno()).orElse(new LegalEntity("name", "orgno", true, new ArrayList<>()));
        assertNotNull(actual);
        assertEquals(actual.getName(), rp.getName());
    }


    @Test
    @DisplayName("search on name")
    void searchOnName() {
        LegalEntity rp = EntityGenerator.generateLegalEntity(0);

        LegalEntity returnFromSave = repository.saveAndFlush(rp);

        Page<LegalEntity> searchResult = repository.searchRelyingParties(
            rp.getName(),
            new ArrayList<>(),
            0,
            false,
            false,
            Pageable.unpaged()
        );

        assertNotNull(searchResult);
        assertEquals(searchResult.getContent().getFirst(), returnFromSave);

    }

    @Test
    @DisplayName("search on orgNo")
    void searchOnOrgno() {
        LegalEntity rp = EntityGenerator.generateLegalEntity(0);
        LegalEntity returnFromSave = repository.saveAndFlush(rp);

        Page<LegalEntity> searchResult = repository.searchRelyingParties(
            rp.getOrgno(),
            new ArrayList<>(),
            0,
            false,
            false,
            Pageable.unpaged()
        );

        assertNotNull(searchResult);
        assertEquals(searchResult.getContent().getFirst(), returnFromSave);
    }

    @Test
    @DisplayName("search on tradeName")
    void searchOnTradeName() {
        LegalEntity rp = EntityGenerator.generateLegalEntity(1);

        LegalEntity returnFromSave = repository.saveAndFlush(rp);

        Page<LegalEntity> searchResult = repository.searchRelyingParties(
            rp.getRelyingPartyInstances().getFirst().getTradeName(),
            new ArrayList<>(),
            0,
            false,
            false,
            Pageable.unpaged()
        );

        assertNotNull(searchResult);
        assertEquals(searchResult.getContent().getFirst(), returnFromSave);
    }

    @Test
    @DisplayName("search on tradeName")
    void filterOnEntitlements() {
        repository.deleteAll();
        LegalEntity le1 = EntityGenerator.generateLegalEntity(1);
        RelyingPartyInstance instance1 = le1.getRelyingPartyInstances().getFirst();
        instance1.setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")));
        repository.saveAndFlush(le1);

        LegalEntity le2 = EntityGenerator.generateLegalEntity(1);
        RelyingPartyInstance instance2 = le2.getRelyingPartyInstances().getFirst();
        instance2.setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider")));
        repository.saveAndFlush(le2);

        Page<LegalEntity> searchResult = repository.searchRelyingParties(
            "",
            List.of("https://uri.etsi.org/19475/Entitlement/QEAA_Provider"),
            1,
            false,
            false,
            Pageable.unpaged()
        );

        assertEquals(1, searchResult.getContent().size());
    }



    @Test
    @DisplayName("filter away synthetic orgnumbers")
    void filterAwaySyntheticOrgNumbers() {
        LegalEntity le1 = EntityGenerator.generateLegalEntity(1);
        le1.setOrgno("234234234"); // Syntetisk orgnummer
        RelyingPartyInstance instance1 = le1.getRelyingPartyInstances().getFirst();
        instance1.setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")));
        repository.saveAndFlush(le1);

        LegalEntity le2 = EntityGenerator.generateLegalEntity(1);
        le2.setOrgno("991825827");
        RelyingPartyInstance instance2 = le2.getRelyingPartyInstances().getFirst();
        instance2.setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider")));
        repository.saveAndFlush(le2);

        Page<LegalEntity> searchResult = repository.searchRelyingParties(
                "",
                List.of("https://uri.etsi.org/19475/Entitlement/QEAA_Provider"),
                1,
                false,
                true,
                Pageable.unpaged()
        );

        assertEquals(0, searchResult.getContent().size());


        Page<LegalEntity> searchResultNew = repository.searchRelyingParties(
                "",
                List.of("https://uri.etsi.org/19475/Entitlement/Service_Provider"),
                1,
                false,
                true,
                Pageable.unpaged()
        );

        assertEquals(1, searchResultNew.getContent().size());
        assertEquals("991825827", searchResultNew.getContent().getFirst().getOrgno());


        Page<LegalEntity> searchResultLast = repository.searchRelyingParties(
                "",
                List.of("https://uri.etsi.org/19475/Entitlement/QEAA_Provider"),
                1,
                false,
                false,
                Pageable.unpaged()
        );
        assertEquals(1, searchResultLast.getContent().size());
        assertEquals("234234234",  searchResultLast.getContent().getFirst().getOrgno());

    }

}
