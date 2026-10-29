package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.integrations.certificateservice.CertificateServiceClient;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V2ApiIntegrationTest {

    private static final String API_KEY = "junit-api-key";
    private static final String RELYING_PARTY = """
        {"tradeName":"Updated party","isPSB":false,"country":"NO",
         "legalPerson":{"legalName":["Legal name"],"establishedByLaw":[]},
         "identifier":[{"type":"EUID","identifier":"NO-123"}],
         "policy":[{"type":"PrivacyPolicy","policyURI":"https://example.test/privacy"}],
         "email":["contact@example.test"]}
        """;
    private static final String RELYING_PARTY_SERVICE = """
        {"serviceTradeName":"Test service","serviceIdentifier":"service-1",
         "supportURI":"https://example.test/support",
         "srvDescription":[[{"lang":"en","content":"Test service"}]],
         "isIntermediary":false}
        """;
    private static final String RELYING_PARTY_INSTANCE = """
        {"relyingPartyEntitlements":[],"relyingPartyEaas":[]}
        """;
    private static final String INTENDED_USE = """
        {"purpose":[{"lang":"en","content":"Verify identity"}],
         "privacyPolicy":[{"type":"PrivacyPolicy","policyURI":"https://example.test/privacy"}],
         "credentials":[{"format":"dc+sd-jwt","meta":{"vct_values":["urn:example:pid"]},
                         "claims":[{"path":["given_name"]}]}]}
        """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WalletRelyingPartyRepository relyingPartyRepository;
    @Autowired
    private WalletRelyingPartyServiceRepository relyingPartyServiceRepository;
    @Autowired
    private RelyingPartyInstanceRepository relyingPartyInstanceRepository;
    @Autowired
    private EntitlementRepository entitlementRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private CertificateServiceClient certificateServiceClient;

    private Entitlement testEntitlement;

    @BeforeEach
    void clearRegistrations() {
        relyingPartyInstanceRepository.deleteAll();
        relyingPartyRepository.deleteAll();
    }

    @AfterEach
    void removeTestEntitlement() {
        if (testEntitlement != null) {
            relyingPartyInstanceRepository.deleteAll();
            relyingPartyRepository.deleteAll();
            entitlementRepository.delete(testEntitlement);
        }
    }

    @Test
    void createsIndependentResourcesAndReadsThemThroughV2AndV1() throws Exception {
        String orgno = TestDataGenerator.generateValidOrgno();
        assertTrue(read(get("/v2/parties").param("orgno", orgno)).get("content").isEmpty());
        assertEquals(0, relyingPartyRepository.count());

        JsonNode relyingParty = read(post("/v2/parties"), "{\"orgno\":\"" + orgno + "\"}");
        String relyingPartyId = relyingParty.get("id").asString();
        assertEquals("https://registry.example.test/wrp", relyingParty.get("registryURI").asString());
        assertEquals(0, relyingPartyServiceRepository.count());
        request(post("/v2/parties"), "{\"orgno\":\"" + orgno + "\"}")
            .andExpect(status().isBadRequest());
        assertEquals(relyingPartyId,
            read(get("/v2/parties").param("orgno", orgno)).at("/content/0/id").asString());

        String relyingPartyServiceId = read(post("/v2/parties/" + relyingPartyId + "/services"),
            RELYING_PARTY_SERVICE).get("id").asString();
        assertEquals(0, relyingPartyInstanceRepository.count());
        String relyingPartyInstanceId = read(post("/v2/services/" + relyingPartyServiceId + "/instances"),
            RELYING_PARTY_INSTANCE).get("id").asString();

        assertEquals(relyingPartyServiceId,
            read(get("/v2/parties/" + relyingPartyId)).at("/serviceIds/0").asString());
        assertEquals(relyingPartyId,
            read(get("/v2/services/" + relyingPartyServiceId)).get("partyId").asString());
        assertEquals(relyingPartyServiceId,
            read(get("/v2/instances/" + relyingPartyInstanceId)).get("serviceId").asString());
        assertEquals(relyingPartyServiceId,
            read(get("/v2/services").param("partyId", relyingPartyId)).at("/content/0/id").asString());
        assertEquals(relyingPartyInstanceId,
            read(get("/v2/instances").param("serviceId", relyingPartyServiceId).param("orgno", orgno))
                .at("/content/0/id").asString());
        assertEquals(relyingPartyInstanceId, read(get("/v1/rp/" + relyingPartyInstanceId)).get("id").asString());
        assertEquals(1, relyingPartyRepository.count());
        assertEquals(1, relyingPartyServiceRepository.count());
        assertEquals(1, relyingPartyInstanceRepository.count());
    }

    @Test
    @DisplayName("Should return organization and service names in instance search without certificate service calls")
    void shouldReturnOrganizationAndServiceNamesInInstanceSearch() throws Exception {
        Registration registration = createRegistration();
        JsonNode relyingParty = read(put(registration.relyingPartyPath()), RELYING_PARTY);

        JsonNode searchResult = read(get("/v2/instances")
            .param("orgno", relyingParty.get("orgno").asString())
            .param("searchTerm", "Legal name")).at("/content/0");

        assertAll(
            () -> assertEquals(registration.relyingPartyInstanceId(), searchResult.get("id").asString()),
            () -> assertEquals(relyingParty.get("orgno"), searchResult.get("orgno")),
            () -> assertEquals("Legal name", searchResult.get("organizationName").asString()),
            () -> assertEquals("Test service", searchResult.get("serviceTradeName").asString()));
        verifyNoInteractions(certificateServiceClient);
    }

    @Test
    void replacementsPersistOwnFieldsWithoutChangingIdentityHierarchyOrStatus() throws Exception {
        Registration registration = createRegistration();
        JsonNode intendedUse = read(post(registration.intendedUsesPath()), INTENDED_USE);
        read(put(registration.relyingPartyPath()), RELYING_PARTY);
        read(put(registration.relyingPartyPath()), RELYING_PARTY.replace("Legal name", "Changed legal name"));
        read(put(registration.relyingPartyServicePath()), RELYING_PARTY_SERVICE.replace("service-1", "service-2"));
        read(put(registration.relyingPartyInstancePath()), """
            {"relyingPartyEntitlements":[],"relyingPartyEaas":[{"namespace":"example","intent":"read"}]}
            """);

        JsonNode relyingParty = read(get(registration.relyingPartyPath()));
        JsonNode relyingPartyService = read(get(registration.relyingPartyServicePath()));
        JsonNode relyingPartyInstance = read(get(registration.relyingPartyInstancePath()));
        assertEquals("Changed legal name", relyingParty.at("/legalPerson/legalName/0").asString());
        assertEquals(registration.relyingPartyServiceId(), relyingParty.at("/serviceIds/0").asString());
        assertEquals("service-2", relyingPartyService.get("serviceIdentifier").asString());
        assertEquals(registration.relyingPartyId(), relyingPartyService.get("partyId").asString());
        assertEquals(registration.relyingPartyInstanceId(), relyingPartyService.at("/instanceIds/0").asString());
        assertEquals(intendedUse.get("id"), relyingPartyService.at("/intendedUseIds/0"));
        assertEquals("read", relyingPartyInstance.at("/relyingPartyEaas/0/intent").asString());
        assertEquals(registration.relyingPartyServiceId(), relyingPartyInstance.get("serviceId").asString());

        request(patch(registration.relyingPartyPath() + "/deactivate")).andExpect(status().isNoContent());
        read(put(registration.relyingPartyPath()), RELYING_PARTY.replace(
            "\"email\":[\"contact@example.test\"]", "\"email\":null"));
        read(put(registration.relyingPartyServicePath()), RELYING_PARTY_SERVICE.replace(
            "\"serviceIdentifier\":\"service-1\",", ""));
        read(put(registration.relyingPartyInstancePath()), RELYING_PARTY_INSTANCE);
        assertTrue(read(get(registration.relyingPartyPath())).get("email").isEmpty());
        assertTrue(read(get(registration.relyingPartyServicePath())).get("serviceIdentifier").isNull());
        assertTrue(read(get(registration.relyingPartyInstancePath())).get("relyingPartyEaas").isEmpty());
        for (String path : registration.resourcePaths()) {
            assertFalse(read(get(path)).get("active").asBoolean());
        }
        assertEquals(intendedUse.get("credentials"),
            read(get(registration.intendedUsesPath())).get(0).get("credentials"));
    }

    @Test
    void invalidRequestsAndUnknownResourcesReturnErrorsWithoutPersistingChanges() throws Exception {
        Registration registration = createRegistration();
        read(put(registration.relyingPartyPath()), RELYING_PARTY);
        List<JsonNode> before = List.of(read(get(registration.relyingPartyPath())),
            read(get(registration.relyingPartyServicePath())), read(get(registration.relyingPartyInstancePath())));

        for (String content : List.of("{", "{}", RELYING_PARTY.replace("\"identifier\":[{\"type\":\"EUID\",\"identifier\":\"NO-123\"}]", "\"identifier\":[]"),
            RELYING_PARTY.replace("\"establishedByLaw\":[]", "\"establishedByLaw\":[null]"))) {
            request(put(registration.relyingPartyPath()), content).andExpect(status().isBadRequest());
        }
        request(put(registration.relyingPartyServicePath()),
            RELYING_PARTY_SERVICE.replace("https://example.test/support", "invalid URI"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description").isNotEmpty());
        for (String content : List.of(
            "{\"relyingPartyEntitlements\":null,\"relyingPartyEaas\":[]}",
            "{\"relyingPartyEntitlements\":[{\"entitlement\":\"unknown\"},{\"entitlement\":\"unknown\"}],\"relyingPartyEaas\":[]}",
            "{\"relyingPartyEntitlements\":[{\"entitlement\":\"unknown\"}],\"relyingPartyEaas\":[]}",
            "{\"relyingPartyEntitlements\":[],\"relyingPartyEaas\":[],\"active\":false}")) {
            request(put(registration.relyingPartyInstancePath()), content).andExpect(status().isBadRequest());
        }
        assertEquals(before, List.of(read(get(registration.relyingPartyPath())),
            read(get(registration.relyingPartyServicePath())), read(get(registration.relyingPartyInstancePath()))));
        for (String content : List.of(INTENDED_USE.replace("[\"urn:example:pid\"]", "[]"),
            INTENDED_USE.replace("\"vct_values\":[\"urn:example:pid\"]", "\"doctype\":\"type\",\"extra\":\"value\""))) {
            request(post(registration.intendedUsesPath()), content).andExpect(status().isBadRequest());
        }
        assertTrue(read(get(registration.intendedUsesPath())).isEmpty());
        request(get("/v2/parties").param("pageSize", "101")).andExpect(status().isBadRequest());
        request(get("/v2/parties").param("sort", "unknown")).andExpect(status().isBadRequest());
        request(put("/v2/parties/" + UUID.randomUUID()), RELYING_PARTY).andExpect(status().isNotFound());
        request(post("/v2/services/" + UUID.randomUUID() + "/instances"), RELYING_PARTY_INSTANCE)
            .andExpect(status().isNotFound());
        assertEquals(1, relyingPartyInstanceRepository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "wrong-api-key"})
    void v2ReadsAndWritesRejectMissingOrInvalidApiKeys(String apiKey) throws Exception {
        Registration registration = createRegistration();
        for (MockHttpServletRequestBuilder builder : List.of(
            get(registration.relyingPartyPath()), get("/v2/services"), get("/v2/instances"), get("/v2/entitlements"),
            get("/v2/registrations/" + registration.relyingPartyInstanceId()),
            post("/v2/parties"), post(registration.relyingPartyPath() + "/services"),
            post(registration.relyingPartyServicePath() + "/instances"),
            put(registration.relyingPartyPath()), put(registration.relyingPartyServicePath()),
            put(registration.relyingPartyInstancePath()), patch(registration.relyingPartyPath() + "/deactivate"),
            post(registration.intendedUsesPath()), patch(registration.intendedUsesPath() + "/" + UUID.randomUUID() + "/revoke"),
            post(registration.relyingPartyInstancePath() + "/certificates/access"))) {
            if (!apiKey.isEmpty()) {
                builder.header("X-API-KEY", apiKey);
            }
            mockMvc.perform(builder).andExpect(status().isUnauthorized());
        }
        assertTrue(read(get(registration.relyingPartyPath())).get("active").asBoolean());
        assertTrue(read(get(registration.intendedUsesPath())).isEmpty());
        assertEquals(1, relyingPartyInstanceRepository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"parties", "services"})
    void inactiveAncestorsRejectChildAndIntendedUseWrites(String level) throws Exception {
        Registration registration = createRegistration();
        JsonNode intendedUse = read(post(registration.intendedUsesPath()), INTENDED_USE);
        String ancestorPath = level.equals("parties")
            ? registration.relyingPartyPath() : registration.relyingPartyServicePath();
        request(patch(ancestorPath + "/deactivate")).andExpect(status().isNoContent());

        request(post(registration.relyingPartyServicePath() + "/instances"), RELYING_PARTY_INSTANCE)
            .andExpect(status().isNotFound());
        if (level.equals("parties")) {
            request(post(registration.relyingPartyPath() + "/services"), RELYING_PARTY_SERVICE)
                .andExpect(status().isNotFound());
        }
        request(post(registration.intendedUsesPath()), INTENDED_USE).andExpect(status().isNotFound());
        request(post(registration.intendedUsesPath() + "/" + intendedUse.get("id").asString() + "/replace"), INTENDED_USE)
            .andExpect(status().isNotFound());
        request(patch(registration.intendedUsesPath() + "/" + intendedUse.get("id").asString() + "/revoke"))
            .andExpect(status().isNotFound());
        assertEquals(intendedUse, read(get(registration.intendedUsesPath())).get(0));
        assertEquals(1, relyingPartyServiceRepository.count());
        assertEquals(1, relyingPartyInstanceRepository.count());
    }

    @Test
    void intendedUseReplacementKeepsHistoryAndRejectsWrongServiceOrRevokedSnapshots() throws Exception {
        Registration registration = createRegistration();
        JsonNode original = read(post(registration.intendedUsesPath()), INTENDED_USE);
        String originalPath = registration.intendedUsesPath() + "/" + original.get("id").asString();
        String otherServiceId = read(post(registration.relyingPartyPath() + "/services"), RELYING_PARTY_SERVICE)
            .get("id").asString();
        request(post("/v2/services/" + otherServiceId + "/intended-uses/" + original.get("id").asString() + "/replace"),
            INTENDED_USE).andExpect(status().isNotFound());
        assertEquals(original, read(get(registration.intendedUsesPath())).get(0));

        JsonNode replacement = read(post(originalPath + "/replace"), INTENDED_USE.replace("given_name", "family_name"));
        String currentId = replacement.at("/current/id").asString();
        assertNotEquals(original.get("id").asString(), currentId);
        assertEquals(original.get("credentials"), replacement.get("revoked").get("credentials"));
        assertEquals(replacement.at("/current/createdAt"), replacement.at("/revoked/revokedAt"));
        request(post(originalPath + "/replace"), INTENDED_USE).andExpect(status().isBadRequest());
        read(patch(registration.intendedUsesPath() + "/" + currentId + "/revoke"));
        JsonNode history = read(get(registration.intendedUsesPath()));
        assertEquals(2, history.size());
        for (JsonNode snapshot : history) {
            assertFalse(snapshot.get("revokedAt").isNull());
            String claim = snapshot.at("/credentials/0/claims/0/path/0").asString();
            assertEquals(snapshot.get("id").equals(original.get("id")) ? "given_name" : "family_name", claim);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://issuer.example.test", "urn:example:issuer", "https:///issuer", "https://bad host", "issuer.example.test", "HTTPS://issuer.example.test", ""})
    @DisplayName("Should reject issuer addresses that are not HTTPS addresses with a host without changing the registration")
    void shouldRejectIssuerAddressesThatAreNotHttpsAddressesWithAHostWithoutChangingTheRegistration(String issuerUrl) throws Exception {
        Registration registration = createRegistration();
        String entitlementValue = "urn:test:issuer:" + UUID.randomUUID();
        testEntitlement = entitlementRepository.saveAndFlush(new Entitlement(entitlementValue, true, "Issuer", "test-ca"));
        String content = objectMapper.writeValueAsString(Map.of(
            "relyingPartyEntitlements", List.of(Map.of("entitlement", entitlementValue, "credentialIssuerUrl", issuerUrl)),
            "relyingPartyEaas", List.of()));
        JsonNode before = read(get(registration.relyingPartyInstancePath()));

        request(post(registration.relyingPartyServicePath() + "/instances"), content)
            .andExpect(status().isBadRequest());
        request(put(registration.relyingPartyInstancePath()), content)
            .andExpect(status().isBadRequest());

        assertEquals(1, relyingPartyInstanceRepository.count());
        assertEquals(before, read(get(registration.relyingPartyInstancePath())));
    }

    @Test
    @DisplayName("Should register an issuer with an HTTPS address and allow its address to be omitted")
    void shouldRegisterAnIssuerWithAnHttpsAddressAndAllowItsAddressToBeOmitted() throws Exception {
        Registration registration = createRegistration();
        String entitlementValue = "urn:test:issuer:" + UUID.randomUUID();
        testEntitlement = entitlementRepository.saveAndFlush(new Entitlement(entitlementValue, true, "Issuer", "test-ca"));
        String content = """
            {"relyingPartyEntitlements":[{"entitlement":"%s","credentialIssuerUrl":"https://issuer.example.test/Issuer"}],
             "relyingPartyEaas":[]}
            """.formatted(entitlementValue);

        JsonNode issuer = read(post(registration.relyingPartyServicePath() + "/instances"), content);
        String issuerPath = "/v2/instances/" + issuer.get("id").asString();
        assertEquals("https://issuer.example.test/Issuer", read(get(issuerPath))
            .at("/relyingPartyEntitlements/0/credentialIssuerUrl").asString());

        read(put(issuerPath), content.replace("\"https://issuer.example.test/Issuer\"", "null"));

        assertTrue(read(get(issuerPath)).at("/relyingPartyEntitlements/0/credentialIssuerUrl").isNull());
    }

    @Test
    @DisplayName("Should preserve existing issuer address registration rules for legacy clients")
    void shouldPreserveExistingIssuerAddressRegistrationRulesForLegacyClients() throws Exception {
        String entitlementValue = "urn:test:issuer:" + UUID.randomUUID();
        testEntitlement = entitlementRepository.saveAndFlush(new Entitlement(entitlementValue, true, "Issuer", "test-ca"));
        String content = """
            {"org_nr":"%s","name":"Legacy issuer",
             "relying_party_entitlements":[{"entitlement":"%s","credential_issuer_url":"http://issuer.example.test"}],
             "relying_party_eaas":[]}
            """.formatted(TestDataGenerator.generateValidOrgno(), entitlementValue);

        JsonNode issuer = read(post("/v1/rp"), content);
        String issuerId = issuer.get("id").asString();

        assertEquals("http://issuer.example.test", read(get("/v1/rp/" + issuerId))
            .at("/relying_party_entitlements/0/credential_issuer_url").asString());
        assertEquals("http://issuer.example.test", read(get("/v2/instances/" + issuerId))
            .at("/relyingPartyEntitlements/0/credentialIssuerUrl").asString());
    }

    @Test
    void instanceUpdatesPreserveCertificatesAndRejectRemovingOrChangingTheirEntitlement() throws Exception {
        Registration registration = createRegistration();
        String entitlementValue = "urn:test:issuer:" + UUID.randomUUID();
        testEntitlement = entitlementRepository.saveAndFlush(new Entitlement(entitlementValue, true, "Issuer", "test-ca"));
        var instance = relyingPartyInstanceRepository.findById(UUID.fromString(registration.relyingPartyInstanceId())).orElseThrow();
        var entitlement = new RelyingPartyEntitlement(entitlementValue, "https://issuer.example.test");
        var issuerCertificate = new IssuerCertificate(CertificatesGenerator.generateX509Certificate(), "test-ca", entitlement);
        entitlement.addIssuerCertificate(issuerCertificate);
        instance.setRelyingPartyEntitlements(List.of(entitlement));
        var accessCertificate = new AccessCertificate("test-ca", CertificatesGenerator.generateX509Certificate(), instance);
        instance.setAccessCertificates(List.of(accessCertificate));
        relyingPartyInstanceRepository.saveAndFlush(instance);
        JsonNode issuerCertificates = read(get(registration.relyingPartyInstancePath() + "/certificates/issuer"));
        JsonNode accessCertificates = read(get(registration.relyingPartyInstancePath() + "/certificates/access"));
        assertEquals(1, issuerCertificates.get("certificates").size());
        assertEquals(1, accessCertificates.get("certificates").size());
        String content = """
            {"relyingPartyEntitlements":[{"entitlement":"%s","credentialIssuerUrl":"https://issuer.example.test"}],
             "relyingPartyEaas":[{"namespace":"example","intent":"read"}]}
            """.formatted(entitlementValue);

        read(put(registration.relyingPartyInstancePath()), content);
        JsonNode before = read(get(registration.relyingPartyInstancePath()));
        request(put(registration.relyingPartyInstancePath()), RELYING_PARTY_INSTANCE).andExpect(status().isBadRequest());
        request(put(registration.relyingPartyInstancePath()), content.replace("https://issuer.example.test", "https://changed.example.test"))
            .andExpect(status().isBadRequest());
        assertEquals(before, read(get(registration.relyingPartyInstancePath())));
        assertEquals(issuerCertificates, read(get(registration.relyingPartyInstancePath() + "/certificates/issuer")));
        assertEquals(accessCertificates, read(get(registration.relyingPartyInstancePath() + "/certificates/access")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"services", "instances", "certificates/access", "certificates/issuer"})
    void writesWaitForDeactivationAndCannotCommitUnderInactiveParent(String resource) throws Exception {
        Registration registration = createRegistration();
        String parentPath = switch (resource) {
            case "services" -> registration.relyingPartyPath();
            case "instances" -> registration.relyingPartyServicePath();
            default -> registration.relyingPartyInstancePath();
        };
        String content;
        if (resource.startsWith("certificates/")) {
            String entitlement = "urn:test:concurrent-issuer:" + UUID.randomUUID();
            testEntitlement = entitlementRepository.saveAndFlush(new Entitlement(entitlement, true, "Issuer", "test-ca"));
            read(put(registration.relyingPartyInstancePath()), """
                {"relyingPartyEntitlements":[{"entitlement":"%s"}],"relyingPartyEaas":[]}
                """.formatted(entitlement));
            String csr = PKCS10CertificationRequestConverter.convert(CertificatesGenerator.generatePKCS10Csr());
            content = objectMapper.writeValueAsString(resource.endsWith("issuer")
                ? Map.of("csr", csr, "entitlement", entitlement) : Map.of("csr", csr));
        } else {
            content = resource.equals("services") ? RELYING_PARTY_SERVICE : RELYING_PARTY_INSTANCE;
        }
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var deactivation = executor.submit(() -> transaction.executeWithoutResult(state -> {
                try {
                    request(patch(registration.relyingPartyPath() + "/deactivate")).andExpect(status().isNoContent());
                    locked.countDown();
                    assertTrue(release.await(10, TimeUnit.SECONDS));
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }));
            try {
                assertTrue(locked.await(10, TimeUnit.SECONDS));
                CountDownLatch started = new CountDownLatch(1);
                var creation = executor.submit(() -> {
                    started.countDown();
                    request(post(parentPath + "/" + resource), content).andExpect(status().isNotFound());
                    return null;
                });
                assertTrue(started.await(10, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> creation.get(500, TimeUnit.MILLISECONDS));
                release.countDown();
                deactivation.get(10, TimeUnit.SECONDS);
                creation.get(10, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
        }
        assertEquals(1, relyingPartyServiceRepository.count());
        assertEquals(1, relyingPartyInstanceRepository.count());
        verifyNoInteractions(certificateServiceClient);
    }

    @Test
    @DisplayName("Retrieve organization registrations only with authentication and an organization number")
    void shouldRequireAuthenticationAndOrganizationToRetrieveRegistrations() throws Exception {
        Registration registration = createRegistration();
        String orgno = relyingPartyRepository.findById(UUID.fromString(registration.relyingPartyId())).orElseThrow().getOrgno();

        mockMvc.perform(get("/v2/registrations").param("orgno", orgno)).andExpect(status().isUnauthorized());
        request(get("/v2/registrations")).andExpect(status().isBadRequest());
        request(get("/v2/registrations").param("orgno", " ")).andExpect(status().isBadRequest());
        request(get("/v2/registrations").param("orgno", orgno))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(registration.relyingPartyInstanceId()));
    }

    @Test
    @DisplayName("Should retrieve the owning organization, roles and existing certificates after the relying party is deactivated")
    void shouldRetrieveExistingRegistrationDetailsAndCertificatesAfterPartyDeactivation() throws Exception {
        Registration registration = createRegistration();
        String orgno = read(get(registration.relyingPartyPath())).get("orgno").asString();
        String entitlementUri = "urn:test:registration-details";
        testEntitlement = entitlementRepository.saveAndFlush(new Entitlement(entitlementUri, false, "Issuer", "test-ca"));
        var relyingPartyInstance = relyingPartyInstanceRepository.findById(UUID.fromString(registration.relyingPartyInstanceId()))
            .orElseThrow();
        var relyingPartyEntitlement = new RelyingPartyEntitlement(entitlementUri, "https://issuer.example.test");
        var issuerCertificate = new IssuerCertificate(
            CertificatesGenerator.generateX509Certificate(), "test-ca", relyingPartyEntitlement);
        relyingPartyEntitlement.addIssuerCertificate(issuerCertificate);
        relyingPartyInstance.setRelyingPartyEntitlements(List.of(relyingPartyEntitlement));
        var accessCertificate = new AccessCertificate(
            "test-ca", CertificatesGenerator.generateX509Certificate(), relyingPartyInstance);
        relyingPartyInstance.setAccessCertificates(List.of(accessCertificate));
        var savedRelyingPartyInstance = relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);
        UUID accessCertificateId = savedRelyingPartyInstance.getAccessCertificates().getFirst().getId();
        UUID issuerCertificateId = savedRelyingPartyInstance.getIssuerCertificates().getFirst().getId();

        request(patch(registration.relyingPartyPath() + "/deactivate"))
            .andExpect(status().isNoContent());

        JsonNode registrationDetails = read(get("/v2/registrations/" + registration.relyingPartyInstanceId()));
        JsonNode searchResult = read(get("/v2/instances").param("orgno", orgno)
            .param("includeInactive", "true")).at("/content/0");

        assertEquals(accessCertificateId.toString(), searchResult.at("/accessCertificateIds/0").asString());
        assertEquals(issuerCertificateId.toString(), searchResult.at("/issuerCertificateIds/0").asString());
        assertAll(
            () -> assertEquals(orgno, registrationDetails.get("orgno").asString()),
            () -> assertFalse(registrationDetails.get("active").asBoolean()),
            () -> assertEquals("Issuer", registrationDetails.at("/relyingPartyEntitlements/0/displayName").asString()),
            () -> assertEquals(
                "https://issuer.example.test",
                registrationDetails.at("/relyingPartyEntitlements/0/credentialIssuerUrl").asString()),
            () -> assertEquals(accessCertificateId.toString(), registrationDetails.at("/accessCertificates/0/id").asString()),
            () -> assertEquals(issuerCertificateId.toString(), registrationDetails.at("/issuerCertificates/0/id").asString()));
        verifyNoInteractions(certificateServiceClient);
    }

    @Test
    @DisplayName("Should return not found when requesting details for a nonexistent registration")
    void shouldReturnNotFoundForNonexistentRegistration() throws Exception {
        request(get("/v2/registrations/" + new UUID(0, 999)))
            .andExpect(status().isNotFound());
    }

    private Registration createRegistration() throws Exception {
        String relyingPartyId = read(post("/v2/parties"), "{\"orgno\":\"" + TestDataGenerator.generateValidOrgno() + "\"}")
            .get("id").asString();
        String relyingPartyServiceId = read(post("/v2/parties/" + relyingPartyId + "/services"), RELYING_PARTY_SERVICE)
            .get("id").asString();
        String relyingPartyInstanceId = read(post("/v2/services/" + relyingPartyServiceId + "/instances"), RELYING_PARTY_INSTANCE)
            .get("id").asString();
        return new Registration(relyingPartyId, relyingPartyServiceId, relyingPartyInstanceId);
    }

    private ResultActions request(MockHttpServletRequestBuilder builder) throws Exception {
        return mockMvc.perform(builder.header("X-API-KEY", API_KEY));
    }

    private ResultActions request(MockHttpServletRequestBuilder builder, String content) throws Exception {
        return request(builder.contentType(MediaType.APPLICATION_JSON).content(content));
    }

    private JsonNode read(MockHttpServletRequestBuilder builder) throws Exception {
        return read(request(builder));
    }

    private JsonNode read(MockHttpServletRequestBuilder builder, String content) throws Exception {
        return read(request(builder, content));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private record Registration(String relyingPartyId, String relyingPartyServiceId, String relyingPartyInstanceId) {
        String relyingPartyPath() { return "/v2/parties/" + relyingPartyId; }
        String relyingPartyServicePath() { return "/v2/services/" + relyingPartyServiceId; }
        String relyingPartyInstancePath() { return "/v2/instances/" + relyingPartyInstanceId; }
        String intendedUsesPath() { return relyingPartyServicePath() + "/intended-uses"; }
        List<String> resourcePaths() { return List.of(relyingPartyPath(), relyingPartyServicePath(), relyingPartyInstancePath()); }
    }
}
