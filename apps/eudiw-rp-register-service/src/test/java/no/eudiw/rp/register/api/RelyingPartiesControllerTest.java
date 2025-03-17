package no.eudiw.rp.register.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static no.eudiw.rp.register.testdata.EntityGenerator.generateRelyingPartyNoId;
import static no.eudiw.rp.register.testdata.ResourceGenerator.generateCreateRelyingPartyResource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the Relying Parties API")
@ActiveProfiles("test")
public class RelyingPartiesControllerTest {

    public static final String X_API_KEY_HEADER = "X-API-KEY";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelyingPartyRepository relyingPartyRepository;

    @DisplayName("When using the Relying Parties API with valid API key")
    @Nested
    class APITests {

        public static final String VALID_API_KEY = "junit-api-key";

        @Test
        void testCreateRelyingParty() throws Exception {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            ObjectWriter ow = new ObjectMapper().writer();
            String json = ow.writeValueAsString(resource);

            mockMvc.perform(post("/v1/rp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                                .content(json))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.org_nr").value(resource.getOrgNr()))
                   .andExpect(jsonPath("$.name").value(resource.getName()))
                   .andExpect(jsonPath("$.public_sector").value(resource.isPublicSector()));
        }


        @Test
        void testEditRelyingParty() throws Exception {

            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            ObjectWriter ow = new ObjectMapper().writer();
            String json = ow.writeValueAsString(resource);

            ResultActions createResult =
                mockMvc.perform(post("/v1/rp")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(json))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.org_nr").value(resource.getOrgNr()))
                       .andExpect(jsonPath("$.name").value(resource.getName()))
                       .andExpect(jsonPath("$.public_sector").value(resource.isPublicSector()));

            RelyingPartyResource relyingPartyResource = ApiTestUtils.toRelyingPartyResource(createResult);
            EditRelyingPartyResource editResource = new EditRelyingPartyResource(
                TestDataGenerator.generateName(),
                TestDataGenerator.generatePublicSector(),
                new ArrayList<>(),
                new ArrayList<>(),
                true
            );

            mockMvc.perform(put("/v1/rp/" + relyingPartyResource.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                                .content(ow.writeValueAsString(editResource)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.org_nr").value(resource.getOrgNr()))
                   .andExpect(jsonPath("$.name").value(editResource.getName()))
                   .andExpect(jsonPath("$.public_sector").value(editResource.isPublicSector()));
        }

        @Test
        void testGetRelyingParty() throws Exception {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();
            ObjectWriter ow = new ObjectMapper().writer();
            String json = ow.writeValueAsString(resource);

            ResultActions createResult = mockMvc.perform(post("/v1/rp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.org_nr").value(resource.getOrgNr()))
                    .andExpect(jsonPath("$.name").value(resource.getName()))
                    .andExpect(jsonPath("$.public_sector").value(resource.isPublicSector()));

            RelyingPartyResource response = ApiTestUtils.toRelyingPartyResource(createResult);

            mockMvc.perform(get("/v1/rp/" + response.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.org_nr").value(resource.getOrgNr()))
                    .andExpect(jsonPath("$.name").value(resource.getName()))
                    .andExpect(jsonPath("$.public_sector").value(resource.isPublicSector()));
        }

        @Test
        void testGetAllRelyingParties() throws Exception {
            mockMvc.perform(get("/v1/rp")
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.relying_parties").exists())
                    .andExpect(jsonPath("$.relying_parties").isArray());
        }

        @Test
        void testDeleteRelyingParty() throws Exception {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();
            ObjectWriter ow = new ObjectMapper().writer();
            String json = ow.writeValueAsString(resource);

            ResultActions createResult =
                mockMvc.perform(post("/v1/rp")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(json))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.deleted").value("false"))
                       .andExpect(jsonPath("$.org_nr").value(resource.getOrgNr()))
                       .andExpect(jsonPath("$.name").value(resource.getName()))
                       .andExpect(jsonPath("$.public_sector").value(resource.isPublicSector()));

            RelyingPartyResource relyingPartyResource = ApiTestUtils.toRelyingPartyResource(createResult);

            mockMvc.perform(delete("/v1/rp/" + relyingPartyResource.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                   )
                   .andExpect(status().isNoContent());

            mockMvc.perform(get("/v1/rp/" + relyingPartyResource.getId().toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                   )
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.deleted").value("true"))
                   .andExpect(jsonPath("$.org_nr").value(resource.getOrgNr()))
                   .andExpect(jsonPath("$.name").value(resource.getName()))
                   .andExpect(jsonPath("$.public_sector").value(resource.isPublicSector()));
        }

        @Test
        void testDeleteNotFoundRelyingParty() throws Exception {
            mockMvc.perform(delete("/v1/rp/" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                    )
                    .andExpect(status().isNotFound());
        }

        @Nested
        @DisplayName("When searching for relying parties ...")
        class SearchTests {

            @Nested
            @DisplayName("Given include_inactives = false ...")
            class SearchWithInactiveFalseTests {

                @Test
                @DisplayName("then search is successful when orgno exists for active RP")
                void testSearchByExistentOrgno() throws Exception {
                    RelyingParty relyingParty = generateRelyingPartyNoId();
                    relyingPartyRepository.save(relyingParty);

                    SearchRelyingPartyResource searchResource =
                        new SearchRelyingPartyResource(relyingParty.getOrgno(), null, false);

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                    ResultActions actions =
                        mockMvc.perform(post("/v1/rp/search")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                                            .content(searchJson))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.relying_parties").exists());

                    List<RelyingPartyResource> relyingPartyResources =
                        ApiTestUtils.toRelyingPartiesResource(actions).getRelyingParties();
                    assertEquals(1, relyingPartyResources.size());

                    RelyingPartyResource relyingPartyResource = relyingPartyResources.getFirst();
                    assertAll(
                        () -> assertEquals(relyingParty.getOrgno(), relyingPartyResource.getOrgNr()),
                        () -> assertEquals(relyingParty.getName(), relyingPartyResource.getName()),
                        () -> assertEquals(relyingParty.getPublicSector(), relyingPartyResource.isPublicSector())
                    );
                }

                @Test
                @DisplayName("then search succeeds with empty result when orgno associated "
                                 + "with an inactive RP")
                void testSearchProperlyIgnoresInactiveRelyingParty() throws Exception {
                    RelyingParty relyingParty = generateRelyingPartyNoId();
                    relyingParty.setActive(false);
                    relyingPartyRepository.save(relyingParty);

                    SearchRelyingPartyResource searchResource =
                        new SearchRelyingPartyResource(relyingParty.getOrgno(), null, false);

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.relying_parties").exists())
                           .andExpect(jsonPath("$.relying_parties").isArray());
                }


                @Test
                @DisplayName("then search succeeds with empty result when orgno associated "
                                 + "with RP not in searched sector")
                void testSearchRespectsPublicSectorFlag() throws Exception {
                    RelyingParty relyingParty = generateRelyingPartyNoId();
                    relyingPartyRepository.save(relyingParty);

                    SearchRelyingPartyResource searchResource =
                        new SearchRelyingPartyResource(
                            relyingParty.getOrgno(), !relyingParty.getPublicSector(), false);

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.relying_parties").exists())
                           .andExpect(jsonPath("$.relying_parties").isArray());
                }

                @Test
                @DisplayName("then search succeeds with empty result if orgno not in database")
                void testSearchByNonexistentOrgno() throws Exception {

                    String nonexistentOrgno = TestDataGenerator.generateOrgno();

                    SearchRelyingPartyResource searchResource =
                        new SearchRelyingPartyResource(nonexistentOrgno, null, false);

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.relying_parties").exists())
                           .andExpect(jsonPath("$.relying_parties").isArray());
                }
            }

            @Nested
            @DisplayName("Given include_inactives = true ...")
            class SearchWithIncludeInactiveTrueTests {

                @ParameterizedTest
                @ValueSource(booleans = {true, false})
                @DisplayName("then searching for active/inactive relying party succeeds when "
                                 + "include_inactive = true is specified")
                void testSearchWithIncludeInactiveTrue(boolean relyingPartyIsActive) throws Exception {
                    RelyingParty relyingParty = generateRelyingPartyNoId();
                    relyingParty.setActive(relyingPartyIsActive);
                    relyingPartyRepository.save(relyingParty);

                    SearchRelyingPartyResource searchResource =
                        new SearchRelyingPartyResource(relyingParty.getOrgno(), null, true);

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);

                    ResultActions actions =
                        mockMvc.perform(post("/v1/rp/search")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                                            .content(searchJson))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.relying_parties").exists());

                    List<RelyingPartyResource> relyingPartyResources =
                        ApiTestUtils.toRelyingPartiesResource(actions).getRelyingParties();
                    assertEquals(1, relyingPartyResources.size());

                    RelyingPartyResource relyingPartyResource = relyingPartyResources.getFirst();
                    assertAll(
                        () -> assertEquals(relyingParty.getOrgno(), relyingPartyResource.getOrgNr()),
                        () -> assertEquals(relyingParty.getName(), relyingPartyResource.getName()),
                        () -> assertEquals(relyingParty.getPublicSector(), relyingPartyResource.isPublicSector())
                    );
                }
            }
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
