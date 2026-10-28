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
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.MultiLangString;
import no.eudiw.rp.register.domain.Policy;
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
            .andExpect(content().string(containsString("policy")));
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
