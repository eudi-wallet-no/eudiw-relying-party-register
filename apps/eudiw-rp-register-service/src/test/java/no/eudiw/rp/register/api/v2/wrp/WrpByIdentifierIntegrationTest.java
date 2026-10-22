package no.eudiw.rp.register.api.v2.wrp;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import no.eudiw.rp.register.domain.Credential;
import no.eudiw.rp.register.domain.Identifier;
import no.eudiw.rp.register.domain.IntendedUse;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        WalletRelyingParty party = EntityGenerator.generateWalletRelyingParty(1);
        party.setOrgno(orgno);
        party.setIdentifier(List.of(new Identifier("urn:example:euid", additionalIdentifier)));
        party.setPostalAddress(List.of("Private address"));
        WalletRelyingPartyService service = party.getServices().getFirst();
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
        service.setIntendedUses(List.of(intendedUse));
        walletRelyingPartyRepository.saveAndFlush(party);

        assertSignedWrpResponse(orgno, party, additionalIdentifier);
        assertSignedWrpResponse(additionalIdentifier, party, additionalIdentifier);
    }

    @Test
    void returnsNotFoundForUnknownIdentifier() throws Exception {
        mockMvc.perform(get("/wrp/unknown-identifier"))
            .andExpect(status().isNotFound());
    }

    @Test
    void includesDistinctEntitlementsFromActiveInstancesOnly() throws Exception {
        String orgno = TestDataGenerator.generateValidOrgno();
        WalletRelyingParty party = EntityGenerator.generateWalletRelyingParty(1);
        party.setOrgno(orgno);
        WalletRelyingPartyService service = party.getServices().getFirst();
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
        walletRelyingPartyRepository.saveAndFlush(party);

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
        WalletRelyingParty party = EntityGenerator.generateWalletRelyingParty(1);
        party.setOrgno(orgno);
        walletRelyingPartyRepository.saveAndFlush(party);

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
        WalletRelyingParty expectedParty,
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
        assertEquals(expectedParty.getTradeName(), data.get("tradeName"));
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
            expectedParty.getServices().getFirst().getServiceTradeName(),
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

    private RelyingPartyInstance instanceWithEntitlements(String... entitlements) {
        return new RelyingPartyInstance(
            Arrays.stream(entitlements).map(RelyingPartyEntitlement::new).toList(),
            List.of(),
            List.of()
        );
    }
}
