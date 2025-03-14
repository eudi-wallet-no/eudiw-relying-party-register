package no.eudiw.rp.register.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the Relying Parties API")
@ActiveProfiles("test")
public class RelyingPartiesControllerTest {

    public static final String X_API_KEY_HEADER = "X-API-KEY";
    @Autowired
    private MockMvc mockMvc;

    @DisplayName("When using the Relying Parties API with valid API key")
    @Nested
    class APITests {

        public static final String VALID_API_KEY = "junit-api-key";

        protected CreateRelyingPartyResource getRelyingPartyContract(String orgNr, String name, String entitlement) {
            CreateRelyingPartyResource contract = new CreateRelyingPartyResource(
                    orgNr,
                    name,
                    false,
                    null,
                    List.of(getRelyingPartyEaaContract()),
                    true
            );

            if (entitlement != null) {
                contract.setRelyingPartyEntitlements(List.of(getRelyingPartyEntitlementContract(entitlement)));
            }

            return contract;
        }

        protected RelyingPartyEntitlementResource getRelyingPartyEntitlementContract(String entitlement) {
            return new RelyingPartyEntitlementResource(
                    entitlement
            );
        }

        protected RelyingPartyEaaResource getRelyingPartyEaaContract() {
            return new RelyingPartyEaaResource(
                    "namespace",
                    "intent"
            );
        }

        @Test
        void testCreateRelyingParty() throws Exception {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(getRelyingPartyContract("1337", "Relying Party", "access"));

            mockMvc.perform(post("/v1/rp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.org_nr").value("1337"))
                    .andExpect(jsonPath("$.name").value("Relying Party"));
        }

        private RelyingPartyResponse readResultActions(ResultActions result) throws UnsupportedEncodingException, JsonProcessingException {
            return new ObjectMapper()
                    .readValue(
                            result
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString(),
                            RelyingPartyResponse.class
                    );
        }

        @Test
        void testEditRelyingParty() throws Exception {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(getRelyingPartyContract("1234", "Batman", "batcave"));

            ResultActions createResult = mockMvc.perform(post("/v1/rp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.org_nr").value("1234"))
                    .andExpect(jsonPath("$.name").value("Batman"));

            RelyingPartyResponse relyingPartyResponse = readResultActions(createResult);
            EditRelyingPartyResource editContract = new EditRelyingPartyResource(
                    "Robin",
                    true,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    true
            );

            mockMvc.perform(put("/v1/rp/" + relyingPartyResponse.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(ow.writeValueAsString(editContract)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.org_nr").value("1234"))
                    .andExpect(jsonPath("$.name").value("Robin"));
        }

        @Test
        void testGetRelyingParty() throws Exception {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(getRelyingPartyContract("2345", "GetTest", "noe"));

            ResultActions createResult = mockMvc.perform(post("/v1/rp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.org_nr").value("2345"))
                    .andExpect(jsonPath("$.name").value("GetTest"));

            RelyingPartyResponse response = readResultActions(createResult);

            mockMvc.perform(get("/v1/rp/" + response.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.org_nr").value("2345"))
                    .andExpect(jsonPath("$.name").value("GetTest"));
        }

        @Test
        void testGetAllRelyingParties() throws Exception {
            mockMvc.perform(get("/v1/rp")
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isOk());
        }

        @Test
        void testDeleteRelyingParty() throws Exception {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(getRelyingPartyContract("54654321", "Harry", "Galtvort"));

            ResultActions createResult = mockMvc.perform(post("/v1/rp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.org_nr").value("54654321"))
                    .andExpect(jsonPath("$.name").value("Harry"));

            RelyingPartyResponse relyingPartyResponse = readResultActions(createResult);

            mockMvc.perform(delete("/v1/rp/" + relyingPartyResponse.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                    )
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/v1/rp/" + relyingPartyResponse.getId().toString())
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").value("true"));
        }

        @Test
        void testDeleteNotFoundRelyingParty() throws Exception {
            mockMvc.perform(delete("/v1/rp/" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                    )
                    .andExpect(status().isNotFound());
        }
    }


    @DisplayName("When using the Relying Parties API with invalid API key")
    @Nested
    class APISecurityTests {

        @DisplayName("then an error is created when API key header is missing")
        @Test
        void testGetAllRelyingPartiesWithMissingAPIKey() throws Exception {
            mockMvc.perform(get("/v1/rp")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @DisplayName("then an error is created when API key is invalid")
        @Test
        void testGetAllRelyingPartiesWithInvalidAPIKey() throws Exception {
            mockMvc.perform(get("/v1/rp")
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, "junit-invalid-api-key"))
                    .andExpect(status().isUnauthorized());
        }

    }

}
