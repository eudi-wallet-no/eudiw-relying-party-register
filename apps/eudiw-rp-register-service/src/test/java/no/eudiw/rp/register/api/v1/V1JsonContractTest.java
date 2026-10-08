package no.eudiw.rp.register.api.v1;

import no.eudiw.rp.register.api.v1.resource.relyingparty.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEntitlementResource;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V1JsonContractTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    @Transactional
    void returnsRelyingPartyInTheExistingV1JsonFormat() throws Exception {
        var request = new CreateRelyingPartyResource(
            "123456785",
            "Contract Service",
            List.of(new RelyingPartyEntitlementResource(
                "https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
                "Qualified EAA Provider",
                "https://issuer.example")),
            List.of());
        var created = mapper.readTree(mvc.perform(post("/v1/rp")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-KEY", "junit-api-key")
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        String expected = """
            {
              "id": "%s",
              "org_nr": "123456785",
              "org_name": "Syntetisk organisasjon 123456785",
              "name": "Contract Service",
              "public_sector": true,
              "relying_party_entitlements": [
                {
                  "entitlement": "https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
                  "display_name": "Qualified EAA Provider",
                  "credential_issuer_url": "https://issuer.example"
                }
              ],
              "relying_party_eaas": [],
              "access_certificates": [],
              "issuer_certificates": [],
              "created_ms": %d,
              "last_updated_ms": %d,
              "active": true
            }
            """.formatted(
            created.get("id").asString(),
            created.get("created_ms").asLong(),
            created.get("last_updated_ms").asLong());

        JsonNode actual = mapper.readTree(mvc.perform(get("/v1/rp/" + created.get("id").asString())
                .header("X-API-KEY", "junit-api-key"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        assertEquals(mapper.readTree(expected), actual);
    }
}
