package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V2ResourcesIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String API_KEY = "junit-api-key";
    private static final String EMPTY_INSTANCE = """
        {"relyingPartyEntitlements":[],"relyingPartyEaas":[]}
        """;
    private static final String WALLET_RELYING_PARTY = """
        {
          "tradeName":null,"isPSB":false,"country":"NO",
          "legalPerson":{"legalName":["Updated legal name"],"establishedByLaw":[]},
          "identifier":[{"type":"EUID","identifier":"NO-123"}],
          "policy":[{"type":"PrivacyPolicy","policyURI":"https://example.test/policy"}],
          "x5c":[],"postalAddress":[],"email":[],"phone":[],"infoURI":[]
        }
        """;
    private static final String SERVICE = """
        {
          "serviceTradeName":"Test service",
          "serviceIdentifier":"service-1",
          "supportURI":"https://example.test/support",
          "email":null,
          "phone":null,
          "srvDescription":[[{"lang":"en","content":"Test service"}]],
          "subEntitlements":[],
          "providesAttestations":[],
          "usesIntermediaries":[],
          "isIntermediary":false,
          "servedWRPServices":[]
        }
        """;
    private static final String INTENDED_USE = """
        {
          "purpose":[{"lang":"en","content":"Issue a credential"}],
          "privacyPolicy":[{"type":"PrivacyPolicy","policyURI":"https://example.test/privacy"}],
          "credentials":[{
            "format":"dc+sd-jwt",
            "meta":{"vct_values":["urn:example:pid"]},
            "claims":[{"path":["given_name"]}]
          }]
        }
        """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;
    @Autowired
    private WalletRelyingPartyServiceRepository walletRelyingPartyServiceRepository;
    @Autowired
    private RelyingPartyInstanceRepository relyingPartyInstanceRepository;
    @Autowired
    private EntitlementRepository entitlementRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearResources() {
        relyingPartyInstanceRepository.deleteAll();
        walletRelyingPartyRepository.deleteAll();
    }

    @Test
    void resourcesAreCreatedIndependentlyAndPartyLookupIsSeparateFromCreation() throws Exception {
        String orgno = TestDataGenerator.generateValidOrgno();
        JsonNode walletRelyingParty = createWalletRelyingParty(orgno);
        String walletRelyingPartyId = walletRelyingParty.get("id").asString();

        assertEquals(1, walletRelyingPartyRepository.count());
        assertEquals(0, walletRelyingPartyServiceRepository.count());
        assertEquals(0, relyingPartyInstanceRepository.count());
        postJson("/v2/parties", """
            {"orgno":"%s"}
            """.formatted(orgno)).andExpect(status().isBadRequest());
        assertEquals(walletRelyingPartyId, findWalletRelyingParty(orgno).get("id").asString());

        JsonNode walletRelyingPartyService =
            read(postJson("/v2/parties/" + walletRelyingPartyId + "/services", SERVICE));
        String walletRelyingPartyServiceId = walletRelyingPartyService.get("id").asString();
        assertEquals(walletRelyingPartyId, walletRelyingPartyService.get("partyId").asString());
        assertEquals(1, walletRelyingPartyServiceRepository.count());
        assertEquals(0, relyingPartyInstanceRepository.count());

        JsonNode relyingPartyInstance = read(postJson(
            "/v2/services/" + walletRelyingPartyServiceId + "/instances", EMPTY_INSTANCE));
        assertEquals(walletRelyingPartyServiceId, relyingPartyInstance.get("serviceId").asString());
        assertEquals(1, relyingPartyInstanceRepository.count());

        mockMvc.perform(get("/v2/instances/" + relyingPartyInstance.get("id").asString())
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.serviceId").value(walletRelyingPartyServiceId));
    }

    @Test
    void everyV2HttpMethodRequiresApiKey() throws Exception {
        String partyPath = "/v2/parties/" + UUID.randomUUID();

        for (MockHttpServletRequestBuilder request : List.of(
            get(partyPath), post("/v2/parties"), put(partyPath), patch(partyPath + "/deactivate")
        )) {
            mockMvc.perform(request).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void swaggerSeparatesV1FromV2AndDocumentsApiKey() throws Exception {
        JsonNode swaggerConfig = read(mockMvc.perform(get("/v3/api-docs/swagger-config")));
        List<String> swaggerGroups = new ArrayList<>();
        swaggerConfig.get("urls").forEach(url -> swaggerGroups.add(url.get("name").asString()));

        JsonNode v1Paths = read(mockMvc.perform(get("/v3/api-docs/V1"))).get("paths");
        JsonNode v2Documentation = read(mockMvc.perform(get("/v3/api-docs/V2")));
        JsonNode v2Paths = v2Documentation.get("paths");

        assertTrue(swaggerGroups.containsAll(List.of("V1", "V2")));
        assertEquals("V2", swaggerConfig.get("urls.primaryName").asString());
        assertNotNull(v1Paths.get("/v1/rp"));
        assertNull(v1Paths.get("/v2/parties"));
        assertNotNull(v2Paths.get("/v2/parties"));
        assertNull(v2Paths.get("/v1/rp"));
        assertEquals("X-API-KEY",
            v2Documentation.get("components").get("securitySchemes").get("API Key").get("name").asString());
        assertFalse(v2Documentation.get("security").isEmpty());
    }

    @Test
    void unknownAndInactiveParentsAreRejectedWithoutCreatingChildren() throws Exception {
        postJson("/v2/parties/" + UUID.randomUUID() + "/services", SERVICE)
            .andExpect(status().isNotFound());
        postJson("/v2/services/" + UUID.randomUUID() + "/instances", EMPTY_INSTANCE)
            .andExpect(status().isNotFound());

        Hierarchy hierarchy = createHierarchy();
        mockMvc.perform(patch("/v2/parties/" + hierarchy.walletRelyingPartyId() + "/deactivate")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNoContent());
        postJson("/v2/parties/" + hierarchy.walletRelyingPartyId() + "/services", SERVICE)
            .andExpect(status().isBadRequest());
        postJson("/v2/services/" + hierarchy.walletRelyingPartyServiceId() + "/instances", EMPTY_INSTANCE)
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/v2/parties/" + hierarchy.walletRelyingPartyId() + "/activate")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNoContent());
        postJson("/v2/services/" + hierarchy.walletRelyingPartyServiceId() + "/instances", EMPTY_INSTANCE)
            .andExpect(status().isBadRequest());
        assertEquals(1, walletRelyingPartyServiceRepository.count());
        assertEquals(1, relyingPartyInstanceRepository.count());
    }

    @Test
    void completeUpdatesReplaceOwnFieldsButPreserveHierarchyAndIntendedUse() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        String intendedUsePath = "/v2/services/" + hierarchy.walletRelyingPartyServiceId() + "/intended-uses";
        JsonNode intendedUse = read(postJson(intendedUsePath, INTENDED_USE));

        putJson("/v2/parties/" + hierarchy.walletRelyingPartyId(), WALLET_RELYING_PARTY)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(hierarchy.walletRelyingPartyId()))
            .andExpect(jsonPath("$.tradeName").doesNotExist())
            .andExpect(jsonPath("$.legalPerson.legalName[0]").value("Updated legal name"))
            .andExpect(jsonPath("$.serviceIds[0]").value(hierarchy.walletRelyingPartyServiceId()));
        putJson("/v2/parties/" + hierarchy.walletRelyingPartyId(), WALLET_RELYING_PARTY)
            .andExpect(status().isOk());

        putJson("/v2/services/" + hierarchy.walletRelyingPartyServiceId(),
            SERVICE.replace("\"email\":null", "\"email\":\"updated@example.test\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("updated@example.test"))
            .andExpect(jsonPath("$.partyId").value(hierarchy.walletRelyingPartyId()))
            .andExpect(jsonPath("$.instanceIds[0]").value(hierarchy.relyingPartyInstanceId()))
            .andExpect(jsonPath("$.intendedUseIds[0]").value(intendedUse.get("id").asString()));

        putJson("/v2/instances/" + hierarchy.relyingPartyInstanceId(), """
            {"relyingPartyEntitlements":[],
             "relyingPartyEaas":[{"namespace":"example","intent":"read"}]}
            """).andExpect(status().isOk()).andExpect(jsonPath("$.relyingPartyEaas.length()").value(1));
        putJson("/v2/instances/" + hierarchy.relyingPartyInstanceId(), EMPTY_INSTANCE)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relyingPartyEaas").isEmpty())
            .andExpect(jsonPath("$.serviceId").value(hierarchy.walletRelyingPartyServiceId()));

        mockMvc.perform(get(intendedUsePath).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(intendedUse.get("id").asString()))
            .andExpect(jsonPath("$[0].credentials[0].claims[0].path[0]").value("given_name"));
    }

    @Test
    void invalidReplacementsReturnBadRequestWithoutChangingStoredData() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        String partyPath = "/v2/parties/" + hierarchy.walletRelyingPartyId();
        String servicePath = "/v2/services/" + hierarchy.walletRelyingPartyServiceId();
        String instancePath = "/v2/instances/" + hierarchy.relyingPartyInstanceId();
        putJson(partyPath, WALLET_RELYING_PARTY).andExpect(status().isOk());

        putJson(partyPath, """
            {"tradeName":"Incomplete"}
            """).andExpect(status().isBadRequest());
        putJson(partyPath, WALLET_RELYING_PARTY.replace(
            "\"identifier\":[{\"type\":\"EUID\",\"identifier\":\"NO-123\"}]", "\"identifier\":[]"))
            .andExpect(status().isBadRequest());
        putJson(partyPath, WALLET_RELYING_PARTY.replace("\"establishedByLaw\":[]", "\"establishedByLaw\":[null]"))
            .andExpect(status().isBadRequest());
        putJson(servicePath, SERVICE.replace("\"isIntermediary\":false", "\"isIntermediary\":null"))
            .andExpect(status().isBadRequest());
        putJson(servicePath, SERVICE.replace("https://example.test/support", "invalid URI"))
            .andExpect(status().isBadRequest());
        putJson(instancePath, """
            {"relyingPartyEntitlements":[],"relyingPartyEaas":[],"active":false}
            """).andExpect(status().isBadRequest());

        mockMvc.perform(get(partyPath).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tradeName").doesNotExist())
            .andExpect(jsonPath("$.identifier[0].identifier").value("NO-123"));
        mockMvc.perform(get(servicePath).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supportURI").value("https://example.test/support"));
        mockMvc.perform(get(instancePath).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void omittedOptionalTs5FieldsClearTheirValuesOnReplacement() throws Exception {
        Hierarchy hierarchy = createHierarchy();

        JsonNode party = read(putJson("/v2/parties/" + hierarchy.walletRelyingPartyId(), """
            {
              "isPSB":false,
              "country":"NO",
              "identifier":[{"type":"EUID","identifier":"NO-123"}],
              "policy":[{"type":"PrivacyPolicy","policyURI":"https://example.test/policy"}]
            }
            """));
        assertTrue(party.get("tradeName").isNull());
        assertTrue(party.get("legalPerson").isNull());
        assertTrue(party.get("x5c").isEmpty());
        assertTrue(party.get("postalAddress").isEmpty());
        assertTrue(party.get("email").isEmpty());
        assertTrue(party.get("phone").isEmpty());
        assertTrue(party.get("infoURI").isEmpty());

        JsonNode service = read(putJson("/v2/services/" + hierarchy.walletRelyingPartyServiceId(), """
            {
              "serviceTradeName":"Updated service",
              "supportURI":"https://example.test/support",
              "srvDescription":[[{"lang":"en","content":"Updated service"}]],
              "isIntermediary":false
            }
            """));
        assertTrue(service.get("serviceIdentifier").isNull());
        assertTrue(service.get("email").isNull());
        assertTrue(service.get("phone").isNull());
        assertTrue(service.get("subEntitlements").isEmpty());
        assertTrue(service.get("providesAttestations").isEmpty());
        assertTrue(service.get("usesIntermediaries").isEmpty());
        assertTrue(service.get("servedWRPServices").isEmpty());
    }

    @Test
    void updatesTimestampsForChangesToOwnedAndNestedFields() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        putJson("/v2/parties/" + hierarchy.walletRelyingPartyId(), WALLET_RELYING_PARTY)
            .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE wallet_relying_party SET last_updated_ms = 1 WHERE id = ?",
            hierarchy.walletRelyingPartyId());
        jdbcTemplate.update("UPDATE wallet_relying_party_service SET last_updated_ms = 1 WHERE id = ?",
            hierarchy.walletRelyingPartyServiceId());
        jdbcTemplate.update("UPDATE relying_party_instance SET last_updated_ms = 1 WHERE id = ?",
            hierarchy.relyingPartyInstanceId());

        // Only the nested legal name changes, so the party's own columns stay unchanged.
        JsonNode updatedParty = read(putJson("/v2/parties/" + hierarchy.walletRelyingPartyId(),
            WALLET_RELYING_PARTY.replace("Updated legal name", "Changed legal name")));
        JsonNode updatedService = read(putJson(
            "/v2/services/" + hierarchy.walletRelyingPartyServiceId(),
            SERVICE.replace("\"serviceIdentifier\":\"service-1\"", "\"serviceIdentifier\":\"service-2\"")));
        JsonNode updatedInstance = read(putJson(
            "/v2/instances/" + hierarchy.relyingPartyInstanceId(), """
                {"relyingPartyEntitlements":[],
                 "relyingPartyEaas":[{"namespace":"example","intent":"read"}]}
                """));

        assertTrue(updatedParty.get("lastUpdatedMs").asLong() > 1);
        assertEquals("Changed legal name", updatedParty.get("legalPerson").get("legalName").get(0).asString());
        assertTrue(updatedService.get("lastUpdatedMs").asLong() > 1);
        assertTrue(updatedInstance.get("lastUpdatedMs").asLong() > 1);
    }

    @Test
    void unchangedInactiveEntitlementsCanBeRetainedButChangedOnesAreRejected() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        String inactiveEntitlement = "urn:test:inactive-" + UUID.randomUUID();
        entitlementRepository.saveAndFlush(new Entitlement(inactiveEntitlement, false, "Inactive", "test-ca"));

        var instance = relyingPartyInstanceRepository.findById(UUID.fromString(hierarchy.relyingPartyInstanceId()))
            .orElseThrow();
        instance.setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement(inactiveEntitlement, null)));
        relyingPartyInstanceRepository.saveAndFlush(instance);

        putJson("/v2/instances/" + hierarchy.relyingPartyInstanceId(), """
            {"relyingPartyEntitlements":[{"entitlement":"%s","credentialIssuerUrl":null}],
             "relyingPartyEaas":[{"namespace":"example","intent":"read"}]}
            """.formatted(inactiveEntitlement))
            .andExpect(status().isOk());
        putJson("/v2/instances/" + hierarchy.relyingPartyInstanceId(), """
            {"relyingPartyEntitlements":[
                {"entitlement":"%s","credentialIssuerUrl":"https://issuer.example.test"}
             ],
             "relyingPartyEaas":[{"namespace":"example","intent":"read"}]}
            """.formatted(inactiveEntitlement))
            .andExpect(status().isBadRequest());

        var storedInstance = relyingPartyInstanceRepository.findById(UUID.fromString(hierarchy.relyingPartyInstanceId()))
            .orElseThrow();
        assertNull(storedInstance.getRelyingPartyEntitlements().getFirst().getCredentialIssuerUrl());
        assertEquals("read", storedInstance.getRelyingPartyEaas().getFirst().getIntent());
    }

    @Test
    void updatesDoNotReactivateInactiveResources() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        mockMvc.perform(patch("/v2/parties/" + hierarchy.walletRelyingPartyId() + "/deactivate")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNoContent());

        putJson("/v2/services/" + hierarchy.walletRelyingPartyServiceId(), SERVICE)
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        putJson("/v2/instances/" + hierarchy.relyingPartyInstanceId(), EMPTY_INSTANCE)
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        putJson("/v2/parties/" + hierarchy.walletRelyingPartyId(), WALLET_RELYING_PARTY)
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void intendedUseReplacementAndRevocationPreserveSnapshots() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        String path = "/v2/services/" + hierarchy.walletRelyingPartyServiceId() + "/intended-uses";
        JsonNode original = read(postJson(path, INTENDED_USE));

        JsonNode replacement = read(postJson(path + "/" + original.get("id").asString() + "/replace",
            INTENDED_USE.replace("given_name", "family_name")));
        assertEquals(original.get("id"), replacement.get("revoked").get("id"));
        assertEquals(replacement.get("revoked").get("revokedAt"), replacement.get("current").get("createdAt"));
        assertEquals("given_name",
            replacement.get("revoked").get("credentials").get(0).get("claims").get(0).get("path").get(0).asString());

        mockMvc.perform(patch(path + "/" + replacement.get("current").get("id").asString() + "/revoke")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revokedAt").isNotEmpty())
            .andExpect(jsonPath("$.credentials[0].claims[0].path[0]").value("family_name"));
        mockMvc.perform(get(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void intendedUseOperationsAreRejectedForInactiveAncestors() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        String path = "/v2/services/" + hierarchy.walletRelyingPartyServiceId() + "/intended-uses";
        JsonNode intendedUse = read(postJson(path, INTENDED_USE));
        String intendedUseId = intendedUse.get("id").asString();

        mockMvc.perform(patch("/v2/services/" + hierarchy.walletRelyingPartyServiceId() + "/deactivate")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNoContent());

        postJson(path, INTENDED_USE).andExpect(status().isBadRequest());
        postJson(path + "/" + intendedUseId + "/replace", INTENDED_USE).andExpect(status().isBadRequest());
        mockMvc.perform(patch(path + "/" + intendedUseId + "/revoke").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(patch("/v2/services/" + hierarchy.walletRelyingPartyServiceId() + "/activate")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNoContent());
        mockMvc.perform(patch("/v2/parties/" + hierarchy.walletRelyingPartyId() + "/deactivate")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNoContent());

        postJson(path, INTENDED_USE).andExpect(status().isBadRequest());
        mockMvc.perform(patch(path + "/" + intendedUseId + "/revoke").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());
    }

    @Test
    void revokedIntendedUseCannotBeRevokedAgainOrReplaced() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        String path = "/v2/services/" + hierarchy.walletRelyingPartyServiceId() + "/intended-uses";
        String intendedUseId = read(postJson(path, INTENDED_USE)).get("id").asString();

        mockMvc.perform(patch(path + "/" + intendedUseId + "/revoke").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.revokedAt").isNotEmpty());

        mockMvc.perform(patch(path + "/" + intendedUseId + "/revoke").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_description").value("IntendedUse is already revoked"));
        postJson(path + "/" + intendedUseId + "/replace", INTENDED_USE).andExpect(status().isBadRequest());

        postJson(path + "/" + UUID.randomUUID() + "/replace", INTENDED_USE).andExpect(status().isNotFound());
        mockMvc.perform(patch(path + "/" + UUID.randomUUID() + "/revoke").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNotFound());
    }

    @Test
    void pagingAndSortParametersAreValidated() throws Exception {
        createHierarchy();

        mockMvc.perform(get("/v2/parties").queryParam("page", "-1").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v2/parties").queryParam("pageSize", "0").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v2/parties").queryParam("pageSize", "101").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v2/parties").queryParam("sort", "unknownProperty").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/v2/parties").queryParam("pageSize", "100")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk());
        mockMvc.perform(get("/v2/parties").queryParam("sort", "tradeName")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk());
    }

    @Test
    void entitlementsAndInstanceSearchAreListed() throws Exception {
        Hierarchy hierarchy = createHierarchy();

        mockMvc.perform(get("/v2/entitlements").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entitlements").isArray());

        mockMvc.perform(get("/v2/instances")
                .queryParam("serviceId", hierarchy.walletRelyingPartyServiceId())
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(hierarchy.relyingPartyInstanceId()));

        mockMvc.perform(get("/v2/instances")
                .queryParam("orgno", hierarchy.orgno())
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(get("/v2/instances")
                .queryParam("orgno", hierarchy.orgno().substring(0, 8))
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/v2/services")
                .queryParam("partyId", hierarchy.walletRelyingPartyId())
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/v2/instances").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/v2/services").header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());
    }

    @Test
    void completeUpdatesReturnNotFoundForUnknownResources() throws Exception {
        putJson("/v2/parties/" + UUID.randomUUID(), WALLET_RELYING_PARTY).andExpect(status().isNotFound());
        putJson("/v2/services/" + UUID.randomUUID(), SERVICE).andExpect(status().isNotFound());
        putJson("/v2/instances/" + UUID.randomUUID(), EMPTY_INSTANCE).andExpect(status().isNotFound());
    }

    @Test
    void issuerCertificateRequestWithoutEntitlementIsRejected() throws Exception {
        Hierarchy hierarchy = createHierarchy();
        String csr = PKCS10CertificationRequestConverter.convert(CertificatesGenerator.generatePKCS10Csr());

        postJson("/v2/instances/" + hierarchy.relyingPartyInstanceId() + "/certificates/issuer",
            objectMapper.writeValueAsString(Map.of("csr", csr)))
            .andExpect(status().isBadRequest());
    }

    private Hierarchy createHierarchy() throws Exception {
        String orgno = TestDataGenerator.generateValidOrgno();
        String walletRelyingPartyId = createWalletRelyingParty(orgno).get("id").asString();
        String walletRelyingPartyServiceId =
            read(postJson("/v2/parties/" + walletRelyingPartyId + "/services", SERVICE)).get("id").asString();
        String relyingPartyInstanceId = read(postJson(
            "/v2/services/" + walletRelyingPartyServiceId + "/instances", EMPTY_INSTANCE)).get("id").asString();
        return new Hierarchy(orgno, walletRelyingPartyId, walletRelyingPartyServiceId, relyingPartyInstanceId);
    }

    private JsonNode createWalletRelyingParty(String orgno) throws Exception {
        return read(postJson("/v2/parties", """
            {"orgno":"%s"}
            """.formatted(orgno)));
    }

    private JsonNode findWalletRelyingParty(String orgno) throws Exception {
        JsonNode parties = read(mockMvc.perform(get("/v2/parties")
            .queryParam("orgno", orgno)
            .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk()));
        return parties.get("content").get(0);
    }

    private ResultActions postJson(String path, String content) throws Exception {
        return mockMvc.perform(post(path).header(API_KEY_HEADER, API_KEY)
            .contentType(MediaType.APPLICATION_JSON).content(content));
    }

    private ResultActions putJson(String path, String content) throws Exception {
        return mockMvc.perform(put(path).header(API_KEY_HEADER, API_KEY)
            .contentType(MediaType.APPLICATION_JSON).content(content));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString());
    }

    private record Hierarchy(
        String orgno,
        String walletRelyingPartyId,
        String walletRelyingPartyServiceId,
        String relyingPartyInstanceId
    ) { }
}
