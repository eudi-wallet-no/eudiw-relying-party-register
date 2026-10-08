package no.eudiw.rp.register.api.v1;

import no.eudiw.rp.register.api.v1.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.integrations.certificateservice.CertificateServiceClient;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V1JsonContractTest {

    private static final String ENTITLEMENT = "https://uri.etsi.org/19475/Entitlement/QEAA_Provider";
    private static final String REQUEST = """
        {"org_nr":"123456785","name":"Contract Service","active":true,
         "relying_party_entitlements":[
           {"entitlement":"https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
            "credential_issuer_url":"https://issuer.example"}],
         "relying_party_eaas":[{"namespace":"contract","intent":"read"}]}
        """;
    private static final String PARTY = """
        {"id":"<id>","org_nr":"123456785","org_name":"Syntetisk organisasjon 123456785",
         "name":"Contract Service","public_sector":true,
         "relying_party_entitlements":[
           {"entitlement":"https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
            "display_name":"Qualified EAA Provider","credential_issuer_url":"https://issuer.example"}],
         "relying_party_eaas":[{"namespace":"contract","intent":"read"}],
         "access_certificates":[],"issuer_certificates":[],
         "created_ms":0,"last_updated_ms":0,"active":true}
        """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private CertificateServiceClient caClient;

    @Test
    void snapshotsCreateFindSearchUpdateCertificatesAndDelete() throws Exception {
        JsonNode created = response(post("/v1/rp").content(REQUEST));
        assertSnapshot(PARTY, created);
        String path = "/v1/rp/" + created.get("id").asString();
        assertSnapshot(PARTY, response(get(path)));

        for (String sortKey : new String[]{"name", "tradeName", "orgno", "legalEntity.orgno"}) {
            assertSnapshot("""
                {"content":[%s],"page":{"size":25,"number":0,"totalElements":1,"totalPages":1}}
                """.formatted(PARTY), response(post("/v1/rp/search").content("""
                {"search_term":"Contract Service","order_by":"%s"}
                """.formatted(sortKey))));
        }

        JsonNode edited = response(put(path).content(
            REQUEST.replace("Contract Service", "Renamed Service")));
        assertSnapshot(PARTY.replace("Contract Service", "Renamed Service"), edited);

        var certificate = CertificatesGenerator.generateX509Certificate();
        when(caClient.requestCertificate(any(), eq("123456785"),
            eq("Syntetisk organisasjon 123456785"), eq("Renamed Service"), anyString()))
            .thenReturn(certificate);
        String pem = "-----BEGIN CERTIFICATE-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(certificate.getEncoded())
            + "\n-----END CERTIFICATE-----\n";
        String accessSnapshot = """
            {"certificate":%s,"id":"<id>","revocation_status":-1}
            """.formatted(mapper.writeValueAsString(pem));
        String issuerSnapshot = """
            {"certificate":%s,"entitlement":"%s","id":"<id>","revocation_status":-1}
            """.formatted(mapper.writeValueAsString(pem), ENTITLEMENT);
        var csr = CertificatesGenerator.generatePKCS10Csr();
        JsonNode access = response(post(path + "/certs/access")
            .content(mapper.writeValueAsString(new RelyingPartyCsrResource(csr))));
        JsonNode issuer = response(post(path + "/certs/issuer")
            .content(mapper.writeValueAsString(new IssuerCsrResource(csr, ENTITLEMENT))));
        assertSnapshot(accessSnapshot, access);
        assertSnapshot(issuerSnapshot, issuer);
        assertSnapshot(accessSnapshot, response(get(path + "/certs/access/" + access.get("id").asString())));
        assertSnapshot(issuerSnapshot, response(get(path + "/certs/issuer/" + issuer.get("id").asString())));
        assertSnapshot("{\"certificates\":[" + accessSnapshot + "]}", response(get(path + "/certs/access")));
        assertSnapshot("{\"certificates\":[" + issuerSnapshot + "]}", response(get(path + "/certs/issuer")));
        String withCertificates = PARTY.replace("Contract Service", "Renamed Service")
            .replace("\"access_certificates\":[]", "\"access_certificates\":[" + accessSnapshot + "]")
            .replace("\"issuer_certificates\":[]", "\"issuer_certificates\":[" + issuerSnapshot + "]");
        assertSnapshot(withCertificates, response(get(path)));

        when(caClient.revokeCertificate(anyString(), eq(0), anyString())).thenReturn(HttpStatus.NO_CONTENT);
        for (String kind : new String[]{"access", "issuer"}) {
            JsonNode cert = kind.equals("access") ? access : issuer;
            mvc.perform(auth(patch(path + "/certs/" + kind + "/" + cert.get("id").asString() + "/revoke")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        }
        mvc.perform(auth(delete(path))).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(auth(get(path))).andExpect(status().isNotFound()).andExpect(content().json("""
            {"error":"not_found","error_description":"Relying party not found"}
            """));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("X-API-KEY", "junit-api-key").contentType("application/json");
    }

    private JsonNode response(MockHttpServletRequestBuilder request) throws Exception {
        return mapper.readTree(mvc.perform(auth(request)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
    }

    private void assertSnapshot(String expected, JsonNode actual) {
        JsonNode normalized = actual.deepCopy();
        normalizeGeneratedValues(normalized);
        assertEquals(mapper.readTree(expected), normalized);
    }

    private void normalizeGeneratedValues(JsonNode node) {
        if (node instanceof ObjectNode object) {
            if (object.has("id")) {
                assertDoesNotThrow(() -> java.util.UUID.fromString(object.get("id").asString()));
                object.put("id", "<id>");
            }
            for (String field : new String[]{"created_ms", "last_updated_ms"}) {
                if (object.has(field)) {
                    assertTrue(object.get(field).isIntegralNumber());
                    assertTrue(object.get(field).asLong() > 0);
                    object.put(field, 0);
                }
            }
        }
        node.forEach(this::normalizeGeneratedValues);
    }
}
