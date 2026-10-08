package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
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
@DisplayName("When using WalletRelyingPartyRepository")
@ActiveProfiles("junit")
public class WalletRelyingPartyRepositoryTest {

    @Autowired
    private WalletRelyingPartyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("store and get")
    void storeAndGet() {
        WalletRelyingParty rp = EntityGenerator.generateWalletRelyingParty();

        WalletRelyingParty returnFromSave = repository.saveAndFlush(rp);

        WalletRelyingParty actual = repository.findById(returnFromSave.getId()).get();

        assert actual.equals(returnFromSave);
    }

    @Test
    @DisplayName("orgno when null")
    void getOrgnoNull() {
        WalletRelyingParty actual = repository.findByOrgno("9876").orElse(new WalletRelyingParty("name", "orgno", true, new ArrayList<>()));
        assertNotNull(actual);
        assertEquals(actual.getLegalName(), "name");
    }

    @Test
    @DisplayName("orgno when exists")
    void getOrgnoExists() {
        WalletRelyingParty rp = EntityGenerator.generateWalletRelyingParty();
        repository.saveAndFlush(rp);
        WalletRelyingParty actual = repository.findByOrgno(rp.getOrgno()).orElse(new WalletRelyingParty("name", "orgno", true, new ArrayList<>()));
        assertNotNull(actual);
        assertEquals(actual.getLegalName(), rp.getLegalName());
    }
}
