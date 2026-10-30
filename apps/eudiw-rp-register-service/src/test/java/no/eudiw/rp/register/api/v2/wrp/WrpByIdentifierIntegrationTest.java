package no.eudiw.rp.register.api.v2.wrp;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import jakarta.persistence.EntityManagerFactory;
import no.eudiw.rp.register.domain.Credential;
import no.eudiw.rp.register.domain.Identifier;
import no.eudiw.rp.register.domain.IntermediaryReference;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.MultiLangString;
import no.eudiw.rp.register.domain.Policy;
import no.eudiw.rp.register.domain.ProvidedAttestation;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class WrpByIdentifierIntegrationTest {

    private static final String JWKS_URL = "https://registry.example.test/wrp/jwks";
    private static KeyPair signingKeyPair;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockitoBean
    private KeystoreManager keystoreManager;

    @BeforeAll
    static void generateSigningKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        signingKeyPair = keyPairGenerator.generateKeyPair();
    }

    @BeforeEach
    void clearRepositoryAndConfigureSigner() {
        walletRelyingPartyRepository.deleteAll();

        KeyProvider keyProvider = mock(KeyProvider.class);
        when(keyProvider.privateKey()).thenReturn(signingKeyPair.getPrivate());
        when(keyProvider.publicKey()).thenReturn(signingKeyPair.getPublic());
        when(keystoreManager.getKeyProvider("wrp-signing")).thenReturn(keyProvider);
    }

    @Test
    void findsPartyByOrgnoAndAdditionalIdentifierAndReturnsSignedResponse() throws Exception {
        String orgno = TestDataGenerator.generateValidOrgno();
        String additionalIdentifier = "EXAMPLE-EUID-123";
        WalletRelyingParty walletRelyingParty = EntityGenerator.generateWalletRelyingParty(1);
        walletRelyingParty.setOrgno(orgno);
        walletRelyingParty.setIdentifier(List.of(new Identifier("urn:example:euid", additionalIdentifier)));
        walletRelyingParty.setPostalAddress(List.of("Private address"));
        WalletRelyingPartyService relyingPartyService = walletRelyingParty.getServices().getFirst();
        IntendedUse intendedUse = new IntendedUse(
            "intended-use-1",
            "2026-01-01",
            null,
            List.of(new MultiLangString("en", "Account opening")),
            List.of(new Policy("PrivacyStatement", "https://example.test/privacy"))
        );
        intendedUse.setCredentials(List.of(new Credential(
            "mso_mdoc",
            Map.of("doctype", "org.iso.18013-5.1.mDL"),
            List.of(Map.of("path", List.of("org.iso.18013.5.1", "family_name")))
        )));
        relyingPartyService.setIntendedUses(List.of(intendedUse));
        walletRelyingPartyRepository.saveAndFlush(walletRelyingParty);

        assertSignedWrpResponse(orgno, walletRelyingParty, additionalIdentifier);
        assertSignedWrpResponse(additionalIdentifier, walletRelyingParty, additionalIdentifier);
    }

    @Test
    void returnsNotFoundForUnknownIdentifier() throws Exception {
        mockMvc.perform(get("/wrp/unknown-identifier"))
            .andExpect(status().isNotFound());
    }

    @Test
    void returnsAnEmptySignedCollectionWhenNoPartiesAreRegistered() throws Exception {
        JWSObject jws = getCollectionPage(null, null);
        Map<String, Object> payload = jws.getPayload().toJSONObject();

        assertEquals("https://registry.example.test", payload.get("iss"));
        assertTrue(((Number) payload.get("iat")).longValue() > 0);
        assertTrue(((List<?>) payload.get("data")).isEmpty());
        Map<?, ?> pagination = (Map<?, ?>) payload.get("pagination");
        assertFalse((Boolean) pagination.get("has_next_page"));
        assertFalse(pagination.containsKey("next_cursor"));
    }

    @Test
    void paginatesTheActiveCollectionUsingTheDefaultLimit() throws Exception {
        List<WalletRelyingParty> parties = new ArrayList<>();
        for (int index = 0; index < 21; index++) {
            parties.add(partyNamed("Collection Party " + index));
        }
        WalletRelyingParty inactiveParty = partyNamed("Inactive Collection Party");
        inactiveParty.setActive(false);
        parties.add(inactiveParty);
        walletRelyingPartyRepository.saveAllAndFlush(parties);

        JWSObject firstPage = getCollectionPage(null, null);
        Map<String, Object> firstPayload = firstPage.getPayload().toJSONObject();
        List<?> firstData = (List<?>) firstPayload.get("data");
        Map<?, ?> firstPagination = (Map<?, ?>) firstPayload.get("pagination");

        assertEquals(20, firstData.size());
        assertTrue((Boolean) firstPagination.get("has_next_page"));
        assertTrue(firstPagination.get("next_cursor") instanceof String);
        assertFalse(firstData.stream().anyMatch(item ->
            "Inactive Collection Party".equals(((Map<?, ?>) item).get("tradeName"))));

        JWSObject secondPage = getCollectionPage(
            null, (String) firstPagination.get("next_cursor"));
        Map<String, Object> secondPayload = secondPage.getPayload().toJSONObject();
        List<?> secondData = (List<?>) secondPayload.get("data");
        Map<?, ?> secondPagination = (Map<?, ?>) secondPayload.get("pagination");

        assertEquals(1, secondData.size());
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        assertFalse(secondPagination.containsKey("next_cursor"));
        Set<String> tradeNames = new HashSet<>();
        firstData.forEach(item -> tradeNames.add((String) ((Map<?, ?>) item).get("tradeName")));
        secondData.forEach(item -> tradeNames.add((String) ((Map<?, ?>) item).get("tradeName")));
        assertEquals(21, tradeNames.size());
    }

    @Test
    void honorsAnExplicitPageLimit() throws Exception {
        walletRelyingPartyRepository.saveAllAndFlush(List.of(
            partyNamed("Page One"),
            partyNamed("Page Two"),
            partyNamed("Page Three")
        ));

        JWSObject firstPage = getCollectionPage(2, null);
        Map<?, ?> firstPagination = (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertEquals(2, ((List<?>) firstPage.getPayload().toJSONObject().get("data")).size());
        assertTrue((Boolean) firstPagination.get("has_next_page"));

        JWSObject secondPage = getCollectionPage(null, (String) firstPagination.get("next_cursor"));
        Map<?, ?> secondPagination = (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertEquals(1, ((List<?>) secondPage.getPayload().toJSONObject().get("data")).size());
        assertFalse((Boolean) secondPagination.get("has_next_page"));
    }

    @Test
    void rejectsInvalidPaginationAndUnsupportedFilters() throws Exception {
        mockMvc.perform(get("/wrp").param("limit", "0"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("limit", "101"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("cursor", "not-a-cursor"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("identifier", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("legalName", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("legalname", "Legal Alpha"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("serviceIdentifier", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("serviceidentifier", "service-one"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("tradeName", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("tradename", "A trade name"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("policy", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("intendedUseIdentifier", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("claimPath", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("entitlement", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("providedAttestation", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("usesIntermediary", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("isIntermediary", "TRUE"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("isIntermediary", "not-a-boolean"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("isIntermediary must be true or false")));
        mockMvc.perform(get("/wrp").param("isIntermediary", "true", "not-a-boolean"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("isIntermediary must be specified at most once")));
        mockMvc.perform(get("/wrp").param("isintermediary", "true"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("providedattestation", "https://schemas.example/payment"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("usesintermediary", "intermediary-service"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp").param("unsupported", "value"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void filtersCollectionByIdentifierAndLegalNameBeforeCursorPagination() throws Exception {
        WalletRelyingParty first = partyWithSearchData(
            "Filter Match A", "urn:example:shared", "Legal Alpha");
        WalletRelyingParty second = partyWithSearchData(
            "Filter Match B", "urn:example:shared", "Legal Beta");
        WalletRelyingParty third = partyWithSearchData(
            "Filter Match C", "urn:example:other", "Legal Alpha");
        WalletRelyingParty inactive = partyWithSearchData(
            "Filter Inactive", "urn:example:shared", "Legal Alpha");
        inactive.setActive(false);
        walletRelyingPartyRepository.saveAllAndFlush(List.of(first, second, third, inactive));

        assertEquals(
            List.of("Filter Match A"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "identifier", first.getOrgno()
            )))
        );
        assertEquals(
            Set.of("Filter Match A", "Filter Match B"),
            Set.copyOf(collectionTradeNames(getCollectionPage(null, null, Map.of(
                "identifier", "urn:example:shared"
            ))))
        );
        assertEquals(
            Set.of("Filter Match A", "Filter Match C"),
            Set.copyOf(collectionTradeNames(getCollectionPage(null, null, Map.of(
                "legalName", "Legal Alpha"
            ))))
        );
        assertEquals(
            List.of("Filter Match A"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "identifier", "urn:example:shared",
                "legalName", "Legal Alpha"
            )))
        );
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of(
            "identifier", "urn:example:shared",
            "legalName", "Legal Gamma"
        ))).isEmpty());
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of(
            "identifier", "urn:example:sha"
        ))).isEmpty());

        JWSObject firstPage = getCollectionPage(
            1, null, Map.of("identifier", "urn:example:shared"));
        Map<?, ?> firstPagination =
            (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) firstPagination.get("has_next_page"));

        JWSObject secondPage = getCollectionPage(
            1,
            (String) firstPagination.get("next_cursor"),
            Map.of("identifier", "urn:example:shared")
        );
        Map<?, ?> secondPagination =
            (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        Set<String> pagedNames = new HashSet<>(collectionTradeNames(firstPage));
        pagedNames.addAll(collectionTradeNames(secondPage));
        assertEquals(Set.of("Filter Match A", "Filter Match B"), pagedNames);
    }

    @Test
    void filtersCollectionByServiceIdentifierAndTradeNameBeforeCursorPagination() throws Exception {
        WalletRelyingParty wrpNameMatch =
            partyWithSearchData("Shared Trade Name", "urn:example:shared", "Legal Alpha");
        WalletRelyingPartyService matchingService = wrpNameMatch.getServices().getFirst();
        matchingService.setServiceIdentifier("service-one");
        matchingService.setServiceTradeName("Shared Trade Name");
        WalletRelyingPartyService secondMatchingService =
            new WalletRelyingPartyService("Shared Trade Name", wrpNameMatch, List.of());
        secondMatchingService.setServiceIdentifier("service-one-secondary");
        wrpNameMatch.setServices(List.of(matchingService, secondMatchingService));

        WalletRelyingParty serviceNameMatch =
            partyWithSearchData("Other WRP", "urn:example:other", "Legal Alpha");
        WalletRelyingPartyService otherService = serviceNameMatch.getServices().getFirst();
        otherService.setServiceIdentifier("service-two");
        otherService.setServiceTradeName("Shared Trade Name");

        WalletRelyingParty inactiveServiceMatch =
            partyWithSearchData("Inactive Service WRP", "urn:example:inactive", "Legal Beta");
        WalletRelyingPartyService inactiveService = inactiveServiceMatch.getServices().getFirst();
        inactiveService.setServiceIdentifier("inactive-service");
        inactiveService.setServiceTradeName("Shared Trade Name");
        inactiveService.setActive(false);

        WalletRelyingParty wrpOnlyMatch =
            partyWithSearchData("WRP-only Trade Name", "urn:example:wrp-only", "Legal Gamma");
        walletRelyingPartyRepository.saveAllAndFlush(List.of(
            wrpNameMatch, serviceNameMatch, inactiveServiceMatch, wrpOnlyMatch));

        assertEquals(
            List.of("Shared Trade Name"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "serviceIdentifier", "service-one"
            )))
        );
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of(
            "serviceIdentifier", "service-one-extra"
        ))).isEmpty());
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of(
            "serviceIdentifier", "inactive-service"
        ))).isEmpty());
        assertEquals(
            List.of("WRP-only Trade Name"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "tradeName", "WRP-only Trade Name"
            )))
        );
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of(
            "tradeName", "WRP-only"
        ))).isEmpty());
        assertEquals(
            List.of("Other WRP"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "identifier", "urn:example:other",
                "legalName", "Legal Alpha",
                "serviceIdentifier", "service-two",
                "tradeName", "Shared Trade Name"
            )))
        );

        JWSObject firstPage = getCollectionPage(
            1, null, Map.of("tradeName", "Shared Trade Name"));
        Map<?, ?> firstPagination =
            (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) firstPagination.get("has_next_page"));
        JWSObject secondPage = getCollectionPage(
            1,
            (String) firstPagination.get("next_cursor"),
            Map.of("tradeName", "Shared Trade Name")
        );
        Map<?, ?> secondPagination =
            (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        List<String> pagedNames = new ArrayList<>(collectionTradeNames(firstPage));
        pagedNames.addAll(collectionTradeNames(secondPage));
        assertEquals(Set.of("Shared Trade Name", "Other WRP"), Set.copyOf(pagedNames));
    }

    @Test
    void filtersCollectionByExactPolicyUriAtWrpAndIntendedUseLevels() throws Exception {
        String wrpPolicyUri = "https://example.test/policies/wrp";
        String intendedUsePolicyUri = "https://example.test/policies/intended-use";
        WalletRelyingParty wrpPolicyMatch =
            partyWithSearchData("WRP policy match", "urn:example:wrp-policy", "Policy Alpha");
        wrpPolicyMatch.setPolicy(List.of(new Policy("PrivacyStatement", wrpPolicyUri)));
        WalletRelyingParty secondWrpPolicyMatch =
            partyWithSearchData("Second WRP policy match", "urn:example:wrp-policy-second", "Policy Alpha");
        secondWrpPolicyMatch.setPolicy(List.of(new Policy("PrivacyStatement", wrpPolicyUri)));

        WalletRelyingParty intendedUsePolicyMatch =
            partyWithSearchData("Intended use policy match", "urn:example:intended-policy", "Policy Beta");
        WalletRelyingPartyService service = intendedUsePolicyMatch.getServices().getFirst();
        service.setIntendedUses(List.of(new IntendedUse(
            "policy-intended-use",
            "2026-01-01",
            null,
            List.of(new MultiLangString("en", "Purpose")),
            List.of(new Policy("PrivacyStatement", intendedUsePolicyUri))
        )));

        WalletRelyingParty partialPolicyMatch =
            partyWithSearchData("Partial policy mismatch", "urn:example:partial-policy", "Policy Beta");
        partialPolicyMatch.setPolicy(List.of(new Policy("PrivacyStatement", wrpPolicyUri + "/extended")));
        walletRelyingPartyRepository.saveAllAndFlush(
            List.of(wrpPolicyMatch, secondWrpPolicyMatch, intendedUsePolicyMatch, partialPolicyMatch));

        JWSObject firstPolicyPage = getCollectionPage(1, null, Map.of("policy", wrpPolicyUri));
        Map<?, ?> pagination =
            (Map<?, ?>) firstPolicyPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) pagination.get("has_next_page"));
        JWSObject secondPolicyPage = getCollectionPage(
            1, (String) pagination.get("next_cursor"), Map.of("policy", wrpPolicyUri));
        Map<?, ?> secondPagination =
            (Map<?, ?>) secondPolicyPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        List<String> policyMatches = new ArrayList<>(collectionTradeNames(firstPolicyPage));
        policyMatches.addAll(collectionTradeNames(secondPolicyPage));
        assertEquals(
            Set.of("WRP policy match", "Second WRP policy match"),
            Set.copyOf(policyMatches)
        );
        assertEquals(
            List.of("Intended use policy match"),
            collectionTradeNames(getCollectionPage(null, null, Map.of("policy", intendedUsePolicyUri)))
        );
        assertTrue(collectionTradeNames(getCollectionPage(
            null, null, Map.of("policy", "https://example.test/policies"))).isEmpty());
        assertEquals(
            List.of("Intended use policy match"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "policy", intendedUsePolicyUri,
                "legalName", "Policy Beta"
            )))
        );
    }

    @Test
    void filtersCollectionByIntendedUseIdentifierBeforeCursorPagination() throws Exception {
        String intendedUseIdentifier = "intended-use-match";
        WalletRelyingParty firstMatch =
            partyWithSearchData("Intended use match A", "urn:example:intended-use-a", "Intended Use Alpha");
        firstMatch.getServices().getFirst().setIntendedUses(List.of(new IntendedUse(
            intendedUseIdentifier,
            "2026-01-01",
            "2026-06-01",
            List.of(new MultiLangString("en", "Purpose A")),
            List.of(new Policy("PrivacyStatement", "https://example.test/privacy-a"))
        )));
        firstMatch.setServices(List.of(
            firstMatch.getServices().getFirst(),
            new WalletRelyingPartyService("Other active service", firstMatch, List.of())
        ));

        WalletRelyingParty secondMatch =
            partyWithSearchData("Intended use match B", "urn:example:intended-use-b", "Intended Use Beta");
        secondMatch.getServices().getFirst().setIntendedUses(List.of(new IntendedUse(
            intendedUseIdentifier,
            "2026-01-01",
            null,
            List.of(new MultiLangString("en", "Purpose B")),
            List.of(new Policy("PrivacyStatement", "https://example.test/privacy-b"))
        )));

        WalletRelyingParty differentIdentifier =
            partyWithSearchData("Different intended use", "urn:example:intended-use-other", "Intended Use Beta");
        differentIdentifier.getServices().getFirst().setIntendedUses(List.of(new IntendedUse(
            intendedUseIdentifier + "-other",
            "2026-01-01",
            null,
            List.of(new MultiLangString("en", "Other purpose")),
            List.of(new Policy("PrivacyStatement", "https://example.test/privacy-other"))
        )));

        WalletRelyingParty inactiveServiceMatch =
            partyWithSearchData("Inactive intended use service", "urn:example:intended-use-inactive", "Other");
        WalletRelyingPartyService inactiveService = inactiveServiceMatch.getServices().getFirst();
        inactiveService.setIntendedUses(List.of(new IntendedUse(
            intendedUseIdentifier,
            "2026-01-01",
            null,
            List.of(new MultiLangString("en", "Inactive purpose")),
            List.of(new Policy("PrivacyStatement", "https://example.test/privacy-inactive"))
        )));
        inactiveService.setActive(false);
        walletRelyingPartyRepository.saveAllAndFlush(
            List.of(firstMatch, secondMatch, differentIdentifier, inactiveServiceMatch));

        JWSObject firstPage = getCollectionPage(1, null, Map.of("intendedUseIdentifier", intendedUseIdentifier));
        Map<?, ?> firstPagination = (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) firstPagination.get("has_next_page"));
        JWSObject secondPage = getCollectionPage(
            1, (String) firstPagination.get("next_cursor"), Map.of("intendedUseIdentifier", intendedUseIdentifier));
        Map<?, ?> secondPagination = (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        List<String> matchingNames = new ArrayList<>(collectionTradeNames(firstPage));
        matchingNames.addAll(collectionTradeNames(secondPage));
        assertEquals(Set.of("Intended use match A", "Intended use match B"), Set.copyOf(matchingNames));

        Map<?, ?> combinedFilterMatch = (Map<?, ?>) ((List<?>) getCollectionPage(null, null, Map.of(
            "intendedUseIdentifier", intendedUseIdentifier,
            "legalName", "Intended Use Alpha"
        )).getPayload().toJSONObject().get("data")).getFirst();
        assertEquals("Intended use match A", combinedFilterMatch.get("tradeName"));
        assertEquals(2, ((List<?>) combinedFilterMatch.get("services")).size());
        assertTrue(collectionTradeNames(getCollectionPage(
            null, null, Map.of("intendedUseIdentifier", "no-such-intended-use"))).isEmpty());
        assertTrue(collectionTradeNames(getCollectionPage(
            null, null, Map.of("intendedUseIdentifier", "INTENDED-USE-MATCH"))).isEmpty());
    }

    @Test
    void filtersCollectionByExactClaimPathElementBeforeCursorPagination() throws Exception {
        WalletRelyingParty pidMatch =
            partyWithSearchData("Claim path PID match", "urn:example:claim-path-pid", "Claim Path Alpha");
        IntendedUse revokedIntendedUse = new IntendedUse(
            "claim-path-revoked",
            "2026-01-01",
            "2026-02-01",
            List.of(new MultiLangString("en", "Purpose")),
            List.of(new Policy("PrivacyStatement", "https://example.test/privacy"))
        );
        revokedIntendedUse.setCredentials(List.of(new Credential(
            "mso_mdoc",
            Map.of("doctype", "org.iso.18013-5.1.mDL"),
            List.of(
                claimPath("eudi.pid.no", "birthdate"),
                claimPath("another.namespace", "birthdate")
            )
        )));
        pidMatch.getServices().getFirst().setIntendedUses(List.of(revokedIntendedUse));
        pidMatch.setServices(List.of(
            pidMatch.getServices().getFirst(),
            new WalletRelyingPartyService("Additional active service", pidMatch, List.of())
        ));

        WalletRelyingParty drivingLicenceMatch =
            partyWithSearchData("Claim path driving licence match", "urn:example:claim-path-dl", "Claim Path Beta");
        IntendedUse drivingLicenceIntendedUse = intendedUseWithClaims(
            "claim-path-driving-licence",
            List.of(claimPath("drivinglicence.no", "birthdate"))
        );
        drivingLicenceMatch.getServices().getFirst().setIntendedUses(List.of(drivingLicenceIntendedUse));

        WalletRelyingParty substringOnly =
            partyWithSearchData("Claim path substring only", "urn:example:claim-path-substring", "Other");
        substringOnly.getServices().getFirst().setIntendedUses(List.of(intendedUseWithClaims(
            "claim-path-substring",
            List.of(claimPath("birthdateVerified"), claimPath(null, 2))
        )));

        WalletRelyingParty caseOnly =
            partyWithSearchData("Claim path case only", "urn:example:claim-path-case", "Other");
        caseOnly.getServices().getFirst().setIntendedUses(List.of(intendedUseWithClaims(
            "claim-path-case",
            List.of(claimPath("Birthdate"))
        )));

        WalletRelyingParty wildcardCharacters =
            partyWithSearchData("Claim path literal wildcard", "urn:example:claim-path-wildcard", "Other");
        wildcardCharacters.getServices().getFirst().setIntendedUses(List.of(intendedUseWithClaims(
            "claim-path-wildcard",
            List.of(claimPath("IBAN_100%"))
        )));

        WalletRelyingParty inactiveServiceMatch =
            partyWithSearchData("Claim path inactive service", "urn:example:claim-path-inactive", "Other");
        WalletRelyingPartyService inactiveService = inactiveServiceMatch.getServices().getFirst();
        inactiveService.setIntendedUses(List.of(intendedUseWithClaims(
            "claim-path-inactive",
            List.of(claimPath("birthdate"))
        )));
        inactiveService.setActive(false);

        walletRelyingPartyRepository.saveAllAndFlush(List.of(
            pidMatch, drivingLicenceMatch, substringOnly, caseOnly, wildcardCharacters, inactiveServiceMatch
        ));

        JWSObject firstPage = getCollectionPage(1, null, Map.of("claimPath", "birthdate"));
        Map<?, ?> firstPagination = (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) firstPagination.get("has_next_page"));
        JWSObject secondPage = getCollectionPage(
            1, (String) firstPagination.get("next_cursor"), Map.of("claimPath", "birthdate"));
        Map<?, ?> secondPagination = (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        List<String> matchingNames = new ArrayList<>(collectionTradeNames(firstPage));
        matchingNames.addAll(collectionTradeNames(secondPage));
        assertEquals(Set.of("Claim path PID match", "Claim path driving licence match"), Set.copyOf(matchingNames));

        Map<?, ?> combinedFilterMatch = (Map<?, ?>) ((List<?>) getCollectionPage(null, null, Map.of(
            "claimPath", "birthdate",
            "legalName", "Claim Path Alpha"
        )).getPayload().toJSONObject().get("data")).getFirst();
        assertEquals("Claim path PID match", combinedFilterMatch.get("tradeName"));
        assertEquals(2, ((List<?>) combinedFilterMatch.get("services")).size());
        assertEquals(
            List.of("Claim path substring only"),
            collectionTradeNames(getCollectionPage(null, null, Map.of("claimPath", "birthdateVerified")))
        );
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of("claimPath", "birth"))).isEmpty());
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of("claimPath", "2"))).isEmpty());
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of("claimPath", "null"))).isEmpty());
        assertEquals(
            List.of("Claim path case only"),
            collectionTradeNames(getCollectionPage(null, null, Map.of("claimPath", "Birthdate")))
        );
        assertTrue(collectionTradeNames(getCollectionPage(
            null, null, Map.of("claimPath", "IBAN_100%"))).contains("Claim path literal wildcard"));
        assertTrue(collectionTradeNames(getCollectionPage(
            null, null, Map.of("claimPath", "IBAN"))).isEmpty());
    }

    @Test
    void filtersCollectionByExactEntitlementAndSubEntitlementBeforeCursorPagination() throws Exception {
        String entitlement = "https://example.test/entitlements/provider";
        WalletRelyingParty multipleEntitlementMatches =
            partyWithSearchData("Entitlement match A", "urn:example:entitlement-a", "Entitlement Alpha");
        WalletRelyingPartyService firstService = multipleEntitlementMatches.getServices().getFirst();
        firstService.setRelyingPartyInstances(List.of(
            instanceWithEntitlements(entitlement),
            instanceWithEntitlements(entitlement)
        ));
        WalletRelyingPartyService subEntitlementService =
            new WalletRelyingPartyService("Matching sub-entitlement service", multipleEntitlementMatches, List.of());
        subEntitlementService.setSubEntitlements(List.of(entitlement));
        WalletRelyingPartyService otherActiveService =
            new WalletRelyingPartyService("Other active service", multipleEntitlementMatches, List.of());
        multipleEntitlementMatches.setServices(List.of(firstService, subEntitlementService, otherActiveService));

        WalletRelyingParty subEntitlementMatch =
            partyWithSearchData("Entitlement match B", "urn:example:entitlement-b", "Entitlement Beta");
        subEntitlementMatch.getServices().getFirst().setSubEntitlements(List.of(entitlement));

        WalletRelyingParty secondInstanceMatch =
            partyWithSearchData("Entitlement match C", "urn:example:entitlement-c", "Entitlement Gamma");
        secondInstanceMatch.getServices().getFirst().setRelyingPartyInstances(
            List.of(instanceWithEntitlements(entitlement)));

        WalletRelyingParty partialMatch =
            partyWithSearchData("Entitlement partial match", "urn:example:entitlement-partial", "Other Legal Name");
        partialMatch.getServices().getFirst().setSubEntitlements(List.of(entitlement + "/extended"));

        WalletRelyingParty inactiveInstanceMatch =
            partyWithSearchData("Inactive instance entitlement", "urn:example:entitlement-inactive-instance", "Other");
        RelyingPartyInstance inactiveInstance = instanceWithEntitlements(entitlement);
        inactiveInstance.setActive(false);
        inactiveInstanceMatch.getServices().getFirst().setRelyingPartyInstances(List.of(inactiveInstance));

        WalletRelyingParty inactiveServiceMatch =
            partyWithSearchData("Inactive service entitlement", "urn:example:entitlement-inactive-service", "Other");
        WalletRelyingPartyService inactiveService = inactiveServiceMatch.getServices().getFirst();
        inactiveService.setSubEntitlements(List.of(entitlement));
        inactiveService.setActive(false);

        walletRelyingPartyRepository.saveAllAndFlush(List.of(
            multipleEntitlementMatches,
            subEntitlementMatch,
            secondInstanceMatch,
            partialMatch,
            inactiveInstanceMatch,
            inactiveServiceMatch
        ));

        JWSObject firstPage = getCollectionPage(1, null, Map.of("entitlement", entitlement));
        Map<?, ?> firstPagination = (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) firstPagination.get("has_next_page"));
        JWSObject secondPage = getCollectionPage(
            1, (String) firstPagination.get("next_cursor"), Map.of("entitlement", entitlement));
        Map<?, ?> secondPagination = (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) secondPagination.get("has_next_page"));
        JWSObject thirdPage = getCollectionPage(
            1, (String) secondPagination.get("next_cursor"), Map.of("entitlement", entitlement));
        Map<?, ?> thirdPagination = (Map<?, ?>) thirdPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) thirdPagination.get("has_next_page"));

        List<String> matchingTradeNames = new ArrayList<>(collectionTradeNames(firstPage));
        matchingTradeNames.addAll(collectionTradeNames(secondPage));
        matchingTradeNames.addAll(collectionTradeNames(thirdPage));
        assertEquals(
            Set.of("Entitlement match A", "Entitlement match B", "Entitlement match C"),
            Set.copyOf(matchingTradeNames)
        );
        JWSObject matchingWrpPage = getCollectionPage(null, null, Map.of(
            "entitlement", entitlement,
            "legalName", "Entitlement Alpha"
        ));
        assertEquals(
            List.of("Entitlement match A"),
            collectionTradeNames(matchingWrpPage)
        );

        Map<?, ?> matchingWrp = (Map<?, ?>) ((List<?>) matchingWrpPage.getPayload().toJSONObject().get("data"))
            .getFirst();
        assertEquals(3, ((List<?>) matchingWrp.get("services")).size());
    }

    @Test
    void matchesInstanceEntitlementsWithCaseSensitiveUriComparison() throws Exception {
        String storedEntitlement = "https://example.test/entitlements/Provider";
        WalletRelyingParty relyingParty =
            partyWithSearchData("Case-sensitive entitlement", "urn:example:case-sensitive", "Case Sensitive");
        relyingParty.getServices().getFirst().setRelyingPartyInstances(
            List.of(instanceWithEntitlements(storedEntitlement)));
        walletRelyingPartyRepository.saveAndFlush(relyingParty);

        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of(
            "entitlement", "https://example.test/entitlements/provider"
        ))).isEmpty());
        assertEquals(
            List.of("Case-sensitive entitlement"),
            collectionTradeNames(getCollectionPage(null, null, Map.of("entitlement", storedEntitlement)))
        );
    }

    @Test
    void filtersCollectionByExactIntermediaryServiceIdentifierBeforeCursorPagination() throws Exception {
        String intermediaryServiceIdentifier = "https://registry.example/wrp/services/Provider";
        IntermediaryReference intermediaryReference = new IntermediaryReference(
            intermediaryServiceIdentifier, "Intermediary", "https://registry.example/wrp");

        WalletRelyingParty firstMatch =
            partyWithSearchData("Intermediary match A", "urn:example:intermediary-a", "Intermediary Alpha");
        WalletRelyingPartyService firstService = firstMatch.getServices().getFirst();
        firstService.setIntermediaryConfiguration(false, List.of(intermediaryReference), List.of());
        WalletRelyingPartyService secondMatchingService =
            new WalletRelyingPartyService("Second matching service", firstMatch, List.of());
        secondMatchingService.setIntermediaryConfiguration(false, List.of(intermediaryReference), List.of());
        WalletRelyingPartyService otherActiveService =
            new WalletRelyingPartyService("Other active service", firstMatch, List.of());
        firstMatch.setServices(List.of(firstService, secondMatchingService, otherActiveService));

        WalletRelyingParty secondMatch =
            partyWithSearchData("Intermediary match B", "urn:example:intermediary-b", "Intermediary Beta");
        secondMatch.getServices().getFirst().setIntermediaryConfiguration(
            false, List.of(intermediaryReference), List.of());

        WalletRelyingParty differentReference =
            partyWithSearchData("Different intermediary", "urn:example:intermediary-other", "Other");
        differentReference.getServices().getFirst().setIntermediaryConfiguration(
            false,
            List.of(new IntermediaryReference(
                "https://registry.example/wrp/services/Other", "Other intermediary",
                "https://registry.example/wrp")),
            List.of());

        WalletRelyingParty caseMismatch =
            partyWithSearchData("Case-different intermediary", "urn:example:intermediary-case", "Other");
        caseMismatch.getServices().getFirst().setIntermediaryConfiguration(
            false,
            List.of(new IntermediaryReference(
                "https://registry.example/wrp/services/provider", "Intermediary",
                "https://registry.example/wrp")),
            List.of());

        WalletRelyingParty inactiveServiceMatch =
            partyWithSearchData("Inactive intermediary service", "urn:example:intermediary-inactive", "Other");
        WalletRelyingPartyService inactiveService = inactiveServiceMatch.getServices().getFirst();
        inactiveService.setIntermediaryConfiguration(false, List.of(intermediaryReference), List.of());
        inactiveService.setActive(false);

        walletRelyingPartyRepository.saveAllAndFlush(List.of(
            firstMatch, secondMatch, differentReference, caseMismatch, inactiveServiceMatch
        ));

        JWSObject firstPage = getCollectionPage(
            1, null, Map.of("usesIntermediary", intermediaryServiceIdentifier));
        Map<?, ?> firstPagination = (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) firstPagination.get("has_next_page"));
        JWSObject secondPage = getCollectionPage(
            1,
            (String) firstPagination.get("next_cursor"),
            Map.of("usesIntermediary", intermediaryServiceIdentifier));
        Map<?, ?> secondPagination = (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        assertEquals(
            Set.of("Intermediary match A", "Intermediary match B"),
            Set.of(collectionTradeNames(firstPage).getFirst(), collectionTradeNames(secondPage).getFirst())
        );
        assertEquals(
            List.of("Intermediary match A"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "usesIntermediary", intermediaryServiceIdentifier,
                "legalName", "Intermediary Alpha"
            )))
        );

        Map<?, ?> matchingWrp = (Map<?, ?>) ((List<?>) getCollectionPage(null, null, Map.of(
            "usesIntermediary", intermediaryServiceIdentifier,
            "legalName", "Intermediary Alpha"
        )).getPayload().toJSONObject().get("data")).getFirst();
        assertEquals(3, ((List<?>) matchingWrp.get("services")).size());
    }

    @Test
    void filtersCollectionByIntermediaryFlagBeforeCursorPagination() throws Exception {
        WalletRelyingParty firstIntermediary =
            partyWithSearchData("Intermediary true A", "urn:example:intermediary-true-a", "Intermediary Alpha");
        WalletRelyingPartyService firstIntermediaryService = firstIntermediary.getServices().getFirst();
        firstIntermediaryService.setIntermediaryConfiguration(true, List.of(), List.of());
        WalletRelyingPartyService secondIntermediaryService =
            new WalletRelyingPartyService("Second intermediary service", firstIntermediary, List.of());
        secondIntermediaryService.setIntermediaryConfiguration(true, List.of(), List.of());
        firstIntermediary.setServices(List.of(firstIntermediaryService, secondIntermediaryService));

        WalletRelyingParty secondIntermediary =
            partyWithSearchData("Intermediary true B", "urn:example:intermediary-true-b", "Intermediary Beta");
        secondIntermediary.getServices().getFirst().setIntermediaryConfiguration(true, List.of(), List.of());

        WalletRelyingParty firstNonIntermediary =
            partyWithSearchData("Intermediary false A", "urn:example:intermediary-false-a", "Regular Alpha");
        WalletRelyingParty secondNonIntermediary =
            partyWithSearchData("Intermediary false B", "urn:example:intermediary-false-b", "Regular Beta");

        WalletRelyingParty inactiveIntermediary =
            partyWithSearchData("Inactive intermediary", "urn:example:intermediary-inactive", "Inactive");
        WalletRelyingPartyService inactiveService = inactiveIntermediary.getServices().getFirst();
        inactiveService.setIntermediaryConfiguration(true, List.of(), List.of());
        inactiveService.setActive(false);
        WalletRelyingPartyService activeNonIntermediaryService =
            new WalletRelyingPartyService("Active regular service", inactiveIntermediary, List.of());
        inactiveIntermediary.setServices(List.of(inactiveService, activeNonIntermediaryService));

        walletRelyingPartyRepository.saveAllAndFlush(List.of(
            firstIntermediary,
            secondIntermediary,
            firstNonIntermediary,
            secondNonIntermediary,
            inactiveIntermediary
        ));

        JWSObject firstPage = getCollectionPage(1, null, Map.of("isIntermediary", "true"));
        Map<?, ?> firstPagination = (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) firstPagination.get("has_next_page"));
        JWSObject secondPage = getCollectionPage(
            1, (String) firstPagination.get("next_cursor"), Map.of("isIntermediary", "true"));
        Map<?, ?> secondPagination = (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        assertEquals(
            Set.of("Intermediary true A", "Intermediary true B"),
            Set.of(collectionTradeNames(firstPage).getFirst(), collectionTradeNames(secondPage).getFirst())
        );
        assertEquals(
            Set.of("Intermediary false A", "Intermediary false B", "Inactive intermediary"),
            Set.copyOf(collectionTradeNames(getCollectionPage(null, null, Map.of("isIntermediary", "false"))))
        );
        assertEquals(
            List.of("Intermediary true A"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "isIntermediary", "true",
                "legalName", "Intermediary Alpha"
            )))
        );

        Map<?, ?> matchingWrp = (Map<?, ?>) ((List<?>) getCollectionPage(null, null, Map.of(
            "isIntermediary", "true",
            "legalName", "Intermediary Alpha"
        )).getPayload().toJSONObject().get("data")).getFirst();
        assertEquals(2, ((List<?>) matchingWrp.get("services")).size());
    }

    @Test
    void matchesBothIntermediaryFlagsForMixedActiveServices() throws Exception {
        WalletRelyingParty party =
            partyWithSearchData("Mixed services", "urn:example:mixed-services", "Mixed Services");
        WalletRelyingPartyService intermediary = party.getServices().getFirst();
        intermediary.setIntermediaryConfiguration(true, List.of(), List.of());
        WalletRelyingPartyService regular =
            new WalletRelyingPartyService("Regular service", party, List.of());
        regular.setIntermediaryConfiguration(false, List.of(), List.of());
        party.setServices(List.of(intermediary, regular));
        walletRelyingPartyRepository.saveAndFlush(party);

        for (String flag : List.of("true", "false")) {
            JWSObject page = getCollectionPage(null, null, Map.of("isIntermediary", flag));
            assertEquals(List.of("Mixed services"), collectionTradeNames(page));
            Map<?, ?> matchingWrp =
                (Map<?, ?>) ((List<?>) page.getPayload().toJSONObject().get("data")).getFirst();
            assertEquals(2, ((List<?>) matchingWrp.get("services")).size());
        }
    }

    @Test
    void filtersCollectionByExactProvidedAttestationTypeBeforeCursorPagination() throws Exception {
        String attestationType = "https://schemas.example/attestations/payment";
        WalletRelyingParty firstMatch =
            partyWithSearchData("Provided attestation match A", "urn:example:attestation-a", "Attestation Alpha");
        WalletRelyingPartyService firstService = firstMatch.getServices().getFirst();
        firstService.setProvidesAttestations(List.of(
            new ProvidedAttestation("dc+sd-jwt", attestationType),
            new ProvidedAttestation("mso_mdoc", attestationType)
        ));
        WalletRelyingPartyService secondMatchingService =
            new WalletRelyingPartyService("Second matching service", firstMatch, List.of());
        secondMatchingService.setProvidesAttestations(List.of(
            new ProvidedAttestation("dc+sd-jwt", attestationType)
        ));
        WalletRelyingPartyService otherActiveService =
            new WalletRelyingPartyService("Other active service", firstMatch, List.of());
        firstMatch.setServices(List.of(firstService, secondMatchingService, otherActiveService));

        WalletRelyingParty secondMatch =
            partyWithSearchData("Provided attestation match B", "urn:example:attestation-b", "Attestation Beta");
        secondMatch.getServices().getFirst().setProvidesAttestations(List.of(
            new ProvidedAttestation("dc+sd-jwt", attestationType)
        ));

        WalletRelyingParty partialMatch =
            partyWithSearchData("Provided attestation partial", "urn:example:attestation-partial", "Other");
        partialMatch.getServices().getFirst().setProvidesAttestations(List.of(
            new ProvidedAttestation("dc+sd-jwt", attestationType + "/extended")
        ));

        WalletRelyingParty caseMismatch =
            partyWithSearchData("Provided attestation case mismatch", "urn:example:attestation-case", "Other");
        caseMismatch.getServices().getFirst().setProvidesAttestations(List.of(
            new ProvidedAttestation("dc+sd-jwt", "https://schemas.example/attestations/Payment")
        ));

        WalletRelyingParty inactiveServiceMatch =
            partyWithSearchData(
                "Inactive provided attestation service", "urn:example:attestation-inactive", "Other");
        WalletRelyingPartyService inactiveService = inactiveServiceMatch.getServices().getFirst();
        inactiveService.setProvidesAttestations(List.of(
            new ProvidedAttestation("dc+sd-jwt", attestationType)
        ));
        inactiveService.setActive(false);

        walletRelyingPartyRepository.saveAllAndFlush(List.of(
            firstMatch, secondMatch, partialMatch, caseMismatch, inactiveServiceMatch
        ));

        assertEquals(
            Set.of("Provided attestation match A", "Provided attestation match B"),
            Set.copyOf(collectionTradeNames(getCollectionPage(null, null, Map.of(
                "providedAttestation", attestationType
            ))))
        );
        JWSObject firstPage = getCollectionPage(1, null, Map.of("providedAttestation", attestationType));
        Map<?, ?> firstPagination = (Map<?, ?>) firstPage.getPayload().toJSONObject().get("pagination");
        assertTrue((Boolean) firstPagination.get("has_next_page"));
        JWSObject secondPage = getCollectionPage(
            1, (String) firstPagination.get("next_cursor"), Map.of("providedAttestation", attestationType));
        Map<?, ?> secondPagination = (Map<?, ?>) secondPage.getPayload().toJSONObject().get("pagination");
        assertFalse((Boolean) secondPagination.get("has_next_page"));
        assertFalse(secondPagination.containsKey("next_cursor"));
        assertEquals(
            Set.of("Provided attestation match A", "Provided attestation match B"),
            Set.copyOf(
                List.of(collectionTradeNames(firstPage).getFirst(), collectionTradeNames(secondPage).getFirst()))
        );
        assertEquals(
            List.of("Provided attestation match A"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "providedAttestation", attestationType,
                "legalName", "Attestation Alpha"
            )))
        );
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of(
            "providedAttestation", attestationType + "/pay"
        ))).isEmpty());
        assertEquals(
            List.of("Provided attestation case mismatch"),
            collectionTradeNames(getCollectionPage(null, null, Map.of(
                "providedAttestation", "https://schemas.example/attestations/Payment"
            )))
        );
        assertTrue(collectionTradeNames(getCollectionPage(null, null, Map.of(
            "providedAttestation", "dc+sd-jwt"
        ))).isEmpty());

        Map<?, ?> matchingWrp = (Map<?, ?>) ((List<?>) getCollectionPage(null, null, Map.of(
            "providedAttestation", attestationType,
            "legalName", "Attestation Alpha"
        )).getPayload().toJSONObject().get("data")).getFirst();
        assertEquals(3, ((List<?>) matchingWrp.get("services")).size());
    }

    @Test
    void batchFetchesNestedAssociationsForCollectionPage() throws Exception {
        List<WalletRelyingParty> parties = new ArrayList<>();
        for (int index = 0; index < 25; index++) {
            WalletRelyingParty party = partyNamed("Batch Party " + index);
            WalletRelyingPartyService service = party.getServices().getFirst();
            IntendedUse intendedUse = new IntendedUse(
                "intended-use-" + index,
                "2026-01-01",
                null,
                List.of(new MultiLangString("en", "Purpose")),
                List.of(new Policy("PrivacyStatement", "https://example.test/privacy"))
            );
            intendedUse.setCredentials(List.of(new Credential(
                "mso_mdoc",
                Map.of("doctype", "org.iso.18013-5.1.mDL"),
                List.of(Map.of("path", List.of("family_name")))
            )));
            service.setIntendedUses(List.of(intendedUse));
            parties.add(party);
        }
        walletRelyingPartyRepository.saveAllAndFlush(parties);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsWereEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        long statementCount;
        try {
            mockMvc.perform(get("/wrp").param("limit", "25"))
                .andExpect(status().isOk());
            statementCount = statistics.getPrepareStatementCount();
        } finally {
            statistics.setStatisticsEnabled(statisticsWereEnabled);
        }

        assertTrue(statementCount > 0, "Expected Hibernate to measure SQL statements");
        assertTrue(
            statementCount <= 10,
            "Expected batched association loading, but collection mapping used "
                + statementCount + " SQL statements for " + parties.size() + " parties"
        );
    }

    @Test
    void documentsTheCollectionRouteAndPaginationParameters() throws Exception {
        mockMvc.perform(get("/v3/api-docs/WRP"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"/wrp\"")))
            .andExpect(content().string(containsString("limit")))
            .andExpect(content().string(containsString("cursor")))
            .andExpect(content().string(containsString("identifier")))
            .andExpect(content().string(containsString("legalName")))
            .andExpect(content().string(containsString("serviceIdentifier")))
            .andExpect(content().string(containsString("tradeName")))
            .andExpect(content().string(containsString("policy")))
            .andExpect(content().string(containsString("entitlement")))
            .andExpect(content().string(containsString("providedAttestation")))
            .andExpect(content().string(containsString("usesIntermediary")))
            .andExpect(content().string(containsString("isIntermediary")))
            .andExpect(content().string(containsString("intendedUseIdentifier")))
            .andExpect(content().string(containsString("claimPath")));
    }

    @Test
    void includesDistinctEntitlementsFromActiveInstancesOnly() throws Exception {
        String orgno = TestDataGenerator.generateValidOrgno();
        WalletRelyingParty relyingParty = EntityGenerator.generateWalletRelyingParty(1);
        relyingParty.setOrgno(orgno);
        WalletRelyingPartyService service = relyingParty.getServices().getFirst();
        RelyingPartyInstance firstActiveInstance = service.getRelyingPartyInstances().getFirst();
        firstActiveInstance.setRelyingPartyEntitlements(List.of(
            new RelyingPartyEntitlement("urn:example:shared"),
            new RelyingPartyEntitlement("urn:example:first")
        ));

        RelyingPartyInstance secondActiveInstance = instanceWithEntitlements(
            "urn:example:shared", "urn:example:second");
        RelyingPartyInstance inactiveInstance = instanceWithEntitlements("urn:example:inactive-only");
        inactiveInstance.setActive(false);
        service.setRelyingPartyInstances(List.of(
            firstActiveInstance, secondActiveInstance, inactiveInstance));
        walletRelyingPartyRepository.saveAndFlush(relyingParty);

        String token = mockMvc.perform(get("/wrp/{identifier}", orgno))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Map<?, ?> data = (Map<?, ?>) JWSObject.parse(token).getPayload().toJSONObject().get("data");
        Map<?, ?> serviceData = (Map<?, ?>) ((List<?>) data.get("services")).getFirst();

        assertEquals(
            Set.of("urn:example:shared", "urn:example:first", "urn:example:second"),
            Set.copyOf((List<?>) serviceData.get("entitlements"))
        );
    }

    @Test
    void requiresApiKeyForFutureWriteMethodsUnderWrp() throws Exception {
        mockMvc.perform(put("/wrp"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void publishesPublicKeyThatVerifiesSignedWrpResponse() throws Exception {
        String orgno = TestDataGenerator.generateValidOrgno();
        WalletRelyingParty relyingParty = EntityGenerator.generateWalletRelyingParty(1);
        relyingParty.setOrgno(orgno);
        walletRelyingPartyRepository.saveAndFlush(relyingParty);

        String token = mockMvc.perform(get("/wrp/{identifier}", orgno))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JWSObject jws = JWSObject.parse(token);

        String jwksJson = mockMvc.perform(get("/wrp/jwks"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/jwk-set+json")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JWKSet jwkSet = JWKSet.parse(jwksJson);
        assertEquals(1, jwkSet.getKeys().size());
        ECKey key = (ECKey) jwkSet.getKeys().getFirst();
        assertEquals("wrp-test-key", key.getKeyID());
        assertEquals(Curve.P_256, key.getCurve());
        assertEquals(KeyUse.SIGNATURE, key.getKeyUse());
        assertNull(key.getD());
        assertEquals(jws.getHeader().getKeyID(), key.getKeyID());
        assertTrue(jws.verify(new ECDSAVerifier(key.toECPublicKey())));
    }

    private void assertSignedWrpResponse(
        String identifier,
        WalletRelyingParty expectedRelyingParty,
        String additionalIdentifier
    ) throws Exception {
        String token = mockMvc.perform(get("/wrp/{identifier}", identifier))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/jwt")))
            .andExpect(header().string("x-jku-url", JWKS_URL))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JWSObject jws = JWSObject.parse(token);
        assertEquals("JWT", jws.getHeader().getType().getType());
        assertEquals("wrp-test-key", jws.getHeader().getKeyID());
        assertTrue(jws.verify(new ECDSAVerifier((ECPublicKey) signingKeyPair.getPublic())));

        Map<String, Object> payload = jws.getPayload().toJSONObject();
        assertEquals("https://registry.example.test", payload.get("iss"));
        assertTrue(((Number) payload.get("iat")).longValue() > 0);

        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        assertEquals(expectedRelyingParty.getTradeName(), data.get("tradeName"));
        assertEquals("https://registry.example.test/wrp", data.get("registryURI"));
        assertTrue(data.containsKey("isPSB"));
        assertFalse(data.containsKey("postalAddress"));
        assertFalse(data.containsKey("id"));
        assertFalse(data.containsKey("active"));
        assertEquals(
            List.of(Map.of("identifier", additionalIdentifier, "type", "urn:example:euid")),
            data.get("identifier")
        );
        Map<?, ?> service = (Map<?, ?>) ((List<?>) data.get("services")).getFirst();
        assertEquals(
            expectedRelyingParty.getServices().getFirst().getServiceTradeName(),
            service.get("serviceTradeName")
        );
        assertFalse(service.containsKey("serviceIdentifier"));
        assertFalse(service.containsKey("supportURI"));
        Map<?, ?> intendedUse = (Map<?, ?>) ((List<?>) service.get("intendedUses")).getFirst();
        Map<?, ?> credential = (Map<?, ?>) ((List<?>) intendedUse.get("credentials")).getFirst();
        assertEquals(Map.of("doctype_value", "org.iso.18013-5.1.mDL"), credential.get("meta"));
        assertEquals(
            List.of(Map.of("path", List.of("org.iso.18013.5.1", "family_name"))),
            credential.get("claims")
        );
    }

    private JWSObject getCollectionPage(Integer limit, String cursor) throws Exception {
        return getCollectionPage(limit, cursor, Map.of());
    }

    private JWSObject getCollectionPage(
        Integer limit,
        String cursor,
        Map<String, String> filters
    ) throws Exception {
        var request = get("/wrp");
        if (limit != null) {
            request.param("limit", limit.toString());
        }
        if (cursor != null) {
            request.param("cursor", cursor);
        }
        filters.forEach((name, value) -> request.param(name, value));
        String token = mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/jwt")))
            .andExpect(header().string("x-jku-url", JWKS_URL))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JWSObject jws = JWSObject.parse(token);
        assertEquals("JWT", jws.getHeader().getType().getType());
        assertEquals("wrp-test-key", jws.getHeader().getKeyID());
        assertTrue(jws.verify(new ECDSAVerifier((ECPublicKey) signingKeyPair.getPublic())));
        return jws;
    }

    private List<String> collectionTradeNames(JWSObject jws) {
        return ((List<?>) jws.getPayload().toJSONObject().get("data")).stream()
            .map(item -> (String) ((Map<?, ?>) item).get("tradeName"))
            .toList();
    }

    private WalletRelyingParty partyWithSearchData(
        String tradeName,
        String registeredIdentifier,
        String legalName
    ) {
        WalletRelyingParty party = partyNamed(tradeName);
        party.setIdentifier(List.of(new Identifier("urn:example:test", registeredIdentifier)));
        party.setLegalPerson(new LegalPerson(List.of(legalName), List.of()));
        return party;
    }

    private IntendedUse intendedUseWithClaims(String identifier, List<Map<String, Object>> claims) {
        IntendedUse intendedUse = new IntendedUse(
            identifier,
            "2026-01-01",
            null,
            List.of(new MultiLangString("en", "Purpose")),
            List.of(new Policy("PrivacyStatement", "https://example.test/privacy"))
        );
        intendedUse.setCredentials(List.of(new Credential(
            "mso_mdoc",
            Map.of("doctype", "org.iso.18013-5.1.mDL"),
            claims
        )));
        return intendedUse;
    }

    private Map<String, Object> claimPath(Object... pathParts) {
        return Map.of("path", Arrays.asList(pathParts));
    }

    private WalletRelyingParty partyNamed(String tradeName) {
        WalletRelyingParty party = EntityGenerator.generateWalletRelyingParty(1);
        party.setOrgno(TestDataGenerator.generateValidOrgno());
        party.setTradeName(tradeName);
        return party;
    }

    private RelyingPartyInstance instanceWithEntitlements(String... entitlements) {
        return new RelyingPartyInstance(
            Arrays.stream(entitlements).map(RelyingPartyEntitlement::new).toList(),
            List.of(),
            List.of()
        );
    }
}
