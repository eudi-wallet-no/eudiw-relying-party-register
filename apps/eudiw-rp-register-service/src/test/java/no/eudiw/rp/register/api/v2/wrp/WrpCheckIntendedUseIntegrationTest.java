package no.eudiw.rp.register.api.v2.wrp;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.MultiLangString;
import no.eudiw.rp.register.domain.Policy;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class WrpCheckIntendedUseIntegrationTest {

    private static final String JWKS_URL = "https://registry.example.test/wrp/jwks";
    private static KeyPair signingKeyPair;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

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
    void returnsSignedTrueForCurrentlyValidIntendedUse() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        storeIntendedUses(List.of(
            intendedUse("age-check", today.minusDays(1), null),
            intendedUse("future-revocation", today.minusDays(1), today.plusDays(1))
        ));

        assertEquals(Boolean.TRUE, signedResult("age-check").get("isRegistered"));
        assertEquals(Boolean.TRUE, signedResult("future-revocation").get("isRegistered"));
    }

    @Test
    void returnsSignedFalseForRevokedOrNotYetValidIntendedUse() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        storeIntendedUses(List.of(
            intendedUse("revoked-use", today.minusDays(2), today),
            intendedUse("future-use", today.plusDays(1), null)
        ));

        assertEquals(Boolean.FALSE, signedResult("revoked-use").get("isRegistered"));
        assertEquals(Boolean.FALSE, signedResult("future-use").get("isRegistered"));
    }

    @Test
    void validReplacementWinsOverRevokedHistoryWithTheSameIdentifier() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        storeIntendedUses(List.of(
            intendedUse("replacement-use", today.minusDays(2), today),
            intendedUse("replacement-use", today, null)
        ));

        assertEquals(Boolean.TRUE, signedResult("replacement-use").get("isRegistered"));
    }

    @Test
    void returnsNotFoundWhenNoExactIntendedUseIdentifierMatches() throws Exception {
        storeIntendedUses(List.of(intendedUse(
            "case-sensitive-use", LocalDate.now(ZoneOffset.UTC).minusDays(1), null)));

        for (String identifier : List.of("unknown-use", "CASE-SENSITIVE-USE")) {
            mockMvc.perform(get("/wrp/check-intended-use")
                    .param("intendedUseIdentifier", identifier))
                .andExpect(status().isNotFound());
        }
    }

    @Test
    void rejectsEmptyBlankRepeatedAndNotYetSupportedFilters() throws Exception {
        mockMvc.perform(get("/wrp/check-intended-use"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp/check-intended-use").param("intendedUseIdentifier", " "))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp/check-intended-use")
                .param("intendedUseIdentifier", "one", "two"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp/check-intended-use").param("identifier", "org-123"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/wrp/check-intended-use").param("unknownFilter", "value"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void documentsTheCheckRouteAndSupportedFilter() throws Exception {
        mockMvc.perform(get("/v3/api-docs/WRP"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"/wrp/check-intended-use\"")))
            .andExpect(content().string(containsString("intendedUseIdentifier")))
            .andExpect(content().string(containsString("No registered intended use matches the query")));
    }

    private Map<?, ?> signedResult(String identifier) throws Exception {
        MvcResult result = mockMvc.perform(get("/wrp/check-intended-use")
                .param("intendedUseIdentifier", identifier))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/jwt")))
            .andExpect(header().string("x-jku-url", JWKS_URL))
            .andReturn();

        JWSObject jws = JWSObject.parse(result.getResponse().getContentAsString());
        assertEquals("JWT", jws.getHeader().getType().getType());
        assertEquals("wrp-test-key", jws.getHeader().getKeyID());
        assertTrue(jws.verify(new ECDSAVerifier((ECPublicKey) signingKeyPair.getPublic())));

        Map<String, Object> payload = jws.getPayload().toJSONObject();
        assertEquals("https://registry.example.test", payload.get("iss"));
        assertTrue(((Number) payload.get("iat")).longValue() > 0);
        assertFalse(payload.containsKey("pagination"));
        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        assertFalse(data.containsKey("details"));
        return data;
    }

    private void storeIntendedUses(List<IntendedUse> intendedUses) {
        WalletRelyingParty party = EntityGenerator.generateWalletRelyingParty(1);
        party.setOrgno(TestDataGenerator.generateValidOrgno());
        party.getServices().getFirst().setIntendedUses(intendedUses);
        walletRelyingPartyRepository.saveAndFlush(party);
    }

    private IntendedUse intendedUse(String identifier, LocalDate createdAt, LocalDate revokedAt) {
        return new IntendedUse(
            identifier,
            createdAt.toString(),
            revokedAt == null ? null : revokedAt.toString(),
            List.of(new MultiLangString("en", "Age verification")),
            List.of(new Policy("PrivacyStatement", "https://example.test/privacy"))
        );
    }
}
