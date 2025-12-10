package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;

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
}
