package no.eudiw.rp.register.api.v1;

import no.eudiw.rp.register.api.v1.resource.relyingparty.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.EditRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.certificates.X509CertificateConverter;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V1JsonContractTest {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String API_KEY = "junit-api-key";
    private static final String ENTITLEMENT = "https://uri.etsi.org/19475/Entitlement/QEAA_Provider";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RelyingPartyInstanceRepository instanceRepository;

    @Test
    @Transactional
    void relyingPartyEndpointsKeepTheExistingJsonContract() throws Exception {
        var createRequest = new CreateRelyingPartyResource(
            "123456785",
            "Contract Service",
            List.of(new RelyingPartyEntitlementResource(
                ENTITLEMENT,
                "Qualified EAA Provider",
                "https://issuer.example")),
            List.of());

        JsonNode created = mapper.readTree(mvc.perform(post("/v1/rp")
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, API_KEY)
                .content(mapper.writeValueAsString(createRequest)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        String id = created.get("id").asString();

        String createdExpected = relyingPartyJson(
            id, "Contract Service", created.get("created_ms").asLong(),
            created.get("last_updated_ms").asLong(), true);
        assertEquals(mapper.readTree(createdExpected), created);

        JsonNode found = mapper.readTree(mvc.perform(get("/v1/rp/" + id)
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertEquals(mapper.readTree(createdExpected), found);

        JsonNode searchResult = mapper.readTree(mvc.perform(post("/v1/rp/search")
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, API_KEY)
                .content("""
                    {"search_term":"123456785","page":0,"page_size":25}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertEquals(mapper.readTree("""
            {
              "content": [%s],
              "page": {
                "size": 25,
                "totalElements": 1,
                "totalPages": 1,
                "number": 0
              }
            }
            """.formatted(createdExpected)), searchResult);

        var editRequest = new EditRelyingPartyResource(
            "Updated Contract Service",
            createRequest.relyingPartyEntitlements(),
            List.of(),
            false);
        JsonNode updated = mapper.readTree(mvc.perform(put("/v1/rp/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, API_KEY)
                .content(mapper.writeValueAsString(editRequest)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        String updatedExpected = relyingPartyJson(
            id, "Updated Contract Service", created.get("created_ms").asLong(),
            updated.get("last_updated_ms").asLong(), false);
        assertEquals(mapper.readTree(updatedExpected), updated);

        JsonNode foundAfterUpdate = mapper.readTree(mvc.perform(get("/v1/rp/" + id)
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertEquals(mapper.readTree(updatedExpected), foundAfterUpdate);

        mvc.perform(delete("/v1/rp/" + id)
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
    }

    @Test
    @Transactional
    void certificateEndpointsKeepTheExistingJsonContract() throws Exception {
        var createRequest = new CreateRelyingPartyResource(
            "123456785",
            "Certificate Contract Service",
            List.of(new RelyingPartyEntitlementResource(ENTITLEMENT, "Qualified EAA Provider", null)),
            List.of());
        JsonNode created = mapper.readTree(mvc.perform(post("/v1/rp")
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, API_KEY)
                .content(mapper.writeValueAsString(createRequest)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        UUID relyingPartyId = UUID.fromString(created.get("id").asString());

        RelyingPartyInstance instance = instanceRepository.findById(relyingPartyId).orElseThrow();
        X509Certificate accessCertificate = CertificatesGenerator.generateX509Certificate();
        var accessEntity = new AccessCertificate("test-ca", accessCertificate, instance);
        instance.setAccessCertificates(List.of(accessEntity));

        X509Certificate issuerCertificate = CertificatesGenerator.generateX509Certificate();
        var issuerEntity = new IssuerCertificate(
            issuerCertificate, "test-ca", instance.getRelyingPartyEntitlements().getFirst());
        instance.getRelyingPartyEntitlements().getFirst().addIssuerCertificate(issuerEntity);
        RelyingPartyInstance savedInstance = instanceRepository.saveAndFlush(instance);
        AccessCertificate savedAccessCertificate = savedInstance.getAccessCertificates().getFirst();
        IssuerCertificate savedIssuerCertificate = savedInstance.getIssuerCertificates().getFirst();

        JsonNode accessExpected = certificateJson(
            savedAccessCertificate.getCertificate(), savedAccessCertificate.getId(), null);
        JsonNode issuerExpected = certificateJson(
            savedIssuerCertificate.getCertificate(), savedIssuerCertificate.getId(), ENTITLEMENT);

        JsonNode accessList = mapper.readTree(mvc.perform(get("/v1/rp/" + relyingPartyId + "/certs/access")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertEquals(mapper.createObjectNode().set("certificates", mapper.createArrayNode().add(accessExpected)),
            accessList);

        JsonNode accessById = mapper.readTree(mvc.perform(get(
                "/v1/rp/" + relyingPartyId + "/certs/access/" + savedAccessCertificate.getId())
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertEquals(accessExpected, accessById);

        JsonNode issuerList = mapper.readTree(mvc.perform(get("/v1/rp/" + relyingPartyId + "/certs/issuer")
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertEquals(mapper.createObjectNode().set("certificates", mapper.createArrayNode().add(issuerExpected)),
            issuerList);

        JsonNode issuerById = mapper.readTree(mvc.perform(get(
                "/v1/rp/" + relyingPartyId + "/certs/issuer/" + savedIssuerCertificate.getId())
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertEquals(issuerExpected, issuerById);
    }

    private String relyingPartyJson(
        String id,
        String tradeName,
        long createdMs,
        long lastUpdatedMs,
        boolean active
    ) {
        return """
            {
              "id": "%s",
              "org_nr": "123456785",
              "org_name": "Syntetisk organisasjon 123456785",
              "name": "%s",
              "public_sector": true,
              "relying_party_entitlements": [
                {
                  "entitlement": "%s",
                  "display_name": "Qualified EAA Provider",
                  "credential_issuer_url": "https://issuer.example"
                }
              ],
              "relying_party_eaas": [],
              "access_certificates": [],
              "issuer_certificates": [],
              "created_ms": %d,
              "last_updated_ms": %d,
              "active": %b
            }
            """.formatted(id, tradeName, ENTITLEMENT, createdMs, lastUpdatedMs, active);
    }

    private JsonNode certificateJson(X509Certificate certificate, UUID id, String entitlement) {
        var json = mapper.createObjectNode();
        json.put("certificate", X509CertificateConverter.convert(certificate));
        json.put("id", id.toString());
        json.put("revocation_status", -1);
        if (entitlement != null) {
            json.put("entitlement", entitlement);
        }
        return json;
    }
}
