package no.eudiw.rp.register.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entitlement.Entitlement;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.data.service.Converter;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static no.eudiw.rp.register.testdata.EntityGenerator.generateRelyingPartyNoId;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

import static no.eudiw.rp.register.testdata.ResourceGenerator.*;
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

        @Nested
        @DisplayName("When creating a relying party ...")
        class CreateTests {
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
                        .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                        .andExpect(jsonPath("$.name").value(resource.name()))
                        .andExpect(jsonPath("$.public_sector").value(resource.publicSector()));
            }

            @Test
            void testCreateRelyingPartyInvalidOrgno() throws Exception {
                CreateRelyingPartyResource resource =
                    generateCreateRelyingPartyResource().withOrgNr(generateInvalidOrgno());

                ObjectWriter ow = new ObjectMapper().writer();
                String json = ow.writeValueAsString(resource);

                mockMvc.perform(post("/v1/rp")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(json))
                       .andExpect(status().isBadRequest())
                       .andExpect(jsonPath("$.error").value("invalid_request"))
                       .andExpect(jsonPath("$.error_description", containsString("invalid_orgnr")));
            }
        }

        @Nested
        @DisplayName("When retrieving a relying party ...")
        class RetrieveTests {
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
                        .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                        .andExpect(jsonPath("$.name").value(resource.name()))
                        .andExpect(jsonPath("$.public_sector").value(resource.publicSector()));

                RelyingPartyResource response = ApiTestUtils.toRelyingPartyResource(createResult);

                mockMvc.perform(get("/v1/rp/" + response.id())
                                .accept(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                        .andExpect(jsonPath("$.name").value(resource.name()))
                        .andExpect(jsonPath("$.public_sector").value(resource.publicSector()));
            }

            @Test
            void testGetRelyingPartyNoneExists() throws Exception {
                mockMvc.perform(get("/v1/rp/" + UUID.randomUUID())
                                .accept(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY))
                        .andExpect(status().isNotFound());
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
        }

        @Nested
        @DisplayName("When deleting a relying party ...")
        class DeleteTests {
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
                                .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                                .andExpect(jsonPath("$.name").value(resource.name()))
                                .andExpect(jsonPath("$.public_sector").value(resource.publicSector()));

                RelyingPartyResource relyingPartyResource = ApiTestUtils.toRelyingPartyResource(createResult);

                mockMvc.perform(delete("/v1/rp/" + relyingPartyResource.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                        )
                        .andExpect(status().isNoContent());

                mockMvc.perform(get("/v1/rp/" + relyingPartyResource.id())
                                .accept(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                        )
                        .andExpect(status().isGone());
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

        @Nested
        @DisplayName("When editing relying party ...")
        class EditTests {
            @Test
            void testEditRelyingPartySameEntitlements() throws Exception {

                CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

                ObjectWriter ow = new ObjectMapper().writer();
                String json = ow.writeValueAsString(resource);

                ResultActions createResult =
                        mockMvc.perform(post("/v1/rp")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                                .andExpect(jsonPath("$.name").value(resource.name()))
                                .andExpect(jsonPath("$.public_sector").value(resource.publicSector()));

                RelyingPartyResource relyingPartyResource = ApiTestUtils.toRelyingPartyResource(createResult);

                EditRelyingPartyResource editResource =
                    generateEditRelyingPartyResource()
                        .withRelyingPartyEntitlements(resource.relyingPartyEntitlements())
                        .withRelyingPartyEaas(resource.relyingPartyEaas());

                mockMvc.perform(put("/v1/rp/" + relyingPartyResource.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                                .content(ow.writeValueAsString(editResource)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                        .andExpect(jsonPath("$.name").value(editResource.name()))
                        .andExpect(jsonPath("$.public_sector").value(editResource.publicSector()));
            }

            @Test
            void testEditRelyingPartyMoreEntitlements() throws Exception {
                List<RelyingPartyEntitlementResource> initialEntitlements =
                    List.of(Entitlement.SERVICE_PROVIDER.toResource(),
                            Entitlement.NON_Q_EAA_PROVIDER.toResource());

                CreateRelyingPartyResource resource =
                    generateCreateRelyingPartyResource()
                        .withRelyingPartyEntitlements(initialEntitlements);

                ObjectWriter ow = new ObjectMapper().writer();
                String json = ow.writeValueAsString(resource);

                ResultActions createResult =
                        mockMvc.perform(post("/v1/rp")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                                .andExpect(jsonPath("$.name").value(resource.name()))
                                .andExpect(jsonPath("$.public_sector").value(resource.publicSector()));
                RelyingPartyResource relyingPartyResource = ApiTestUtils.toRelyingPartyResource(createResult);

                List<RelyingPartyEntitlementResource> entitlements = new ArrayList<>(resource.relyingPartyEntitlements());
                entitlements.add(Entitlement.PID_PROVIDER.toResource());
                EditRelyingPartyResource editResource =
                    generateEditRelyingPartyResource()
                        .withRelyingPartyEntitlements(entitlements)
                        .withRelyingPartyEaas(resource.relyingPartyEaas());

                mockMvc.perform(put("/v1/rp/" + relyingPartyResource.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                                .content(ow.writeValueAsString(editResource)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                        .andExpect(jsonPath("$.name").value(editResource.name()))
                        .andExpect(jsonPath("$.public_sector").value(editResource.publicSector()));
            }

            @Test
            void testEditRelyingPartyLessEntitlements() throws Exception {

                CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

                ObjectWriter ow = new ObjectMapper().writer();
                String json = ow.writeValueAsString(resource);

                ResultActions createResult =
                        mockMvc.perform(post("/v1/rp")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                                .andExpect(jsonPath("$.name").value(resource.name()))
                                .andExpect(jsonPath("$.public_sector").value(resource.publicSector()));

                RelyingPartyResource relyingPartyResource = ApiTestUtils.toRelyingPartyResource(createResult);
                EditRelyingPartyResource editResource =
                    generateEditRelyingPartyResource()
                        .withRelyingPartyEntitlements(List.of());

                ResultActions editResult = mockMvc.perform(put("/v1/rp/" + relyingPartyResource.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                                .content(ow.writeValueAsString(editResource)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
                        .andExpect(jsonPath("$.name").value(editResource.name()))
                        .andExpect(jsonPath("$.public_sector").value(editResource.publicSector()));
                assertTrue(ApiTestUtils.toRelyingPartyResource(editResult).relyingPartyEntitlements().isEmpty());
            }
        }

        @Nested
        @DisplayName("When searching for relying parties ...")
        class SearchTests {
            @Test
            @DisplayName("then search successful when search term exists in an RP orgno")
            void testSearchTermExistsInRpOrgno() throws Exception {
                RelyingParty relyingParty = generateRelyingPartyNoId();
                relyingPartyRepository.save(relyingParty);

                SearchRelyingPartyResource searchResource =
                    SearchRelyingPartyResource.empty().withSearchTerm(relyingParty.getOrgno());

                String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                ResultActions actions =
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.relying_parties").exists());

                List<RelyingPartyResource> relyingPartyResources =
                    ApiTestUtils.toRelyingPartiesResource(actions).relyingParties();
                assertEquals(1, relyingPartyResources.size());

                RelyingPartyResource relyingPartyResource = relyingPartyResources.getFirst();
                assertAll(
                    () -> assertEquals(relyingParty.getOrgno(), relyingPartyResource.orgNr())
                );
            }

            @Test
            @DisplayName("then search successful when search term exists in an RP orgno")
            void testSearchTermExistsInRpName() throws Exception {
                RelyingParty relyingParty = generateRelyingPartyNoId();
                relyingPartyRepository.save(relyingParty);

                SearchRelyingPartyResource searchResource =
                    SearchRelyingPartyResource.empty().withSearchTerm(relyingParty.getName());

                String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                ResultActions actions =
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.relying_parties").exists());

                List<RelyingPartyResource> relyingPartyResources =
                    ApiTestUtils.toRelyingPartiesResource(actions).relyingParties();
                assertEquals(1, relyingPartyResources.size());

                RelyingPartyResource relyingPartyResource = relyingPartyResources.getFirst();
                assertAll(
                    () -> assertEquals(relyingParty.getOrgno(), relyingPartyResource.orgNr())
                );
            }

            @Test
            @DisplayName("then search successful when search term exists in multiple RPs")
            void testSearchTermExistsInDifferentFieldsOfDifferentRps() throws Exception {
                RelyingParty relyingParty1 = generateRelyingPartyNoId();
                RelyingParty relyingParty2 = generateRelyingPartyNoId();
                RelyingParty relyingParty3 = generateRelyingPartyNoId();

                // append some of rp1's orgno to the start of rp2's name
                String searchStr = relyingParty1.getOrgno().substring(0, 4);
                relyingParty2.setName(searchStr + relyingParty2.getName());

                relyingPartyRepository.saveAll(List.of(relyingParty1, relyingParty2, relyingParty3));

                SearchRelyingPartyResource searchResource =
                    SearchRelyingPartyResource.empty().withSearchTerm(searchStr);

                String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                ResultActions actions =
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.relying_parties").exists());

                List<RelyingPartyResource> relyingPartyResources =
                    ApiTestUtils.toRelyingPartiesResource(actions).relyingParties();
                assertEquals(2, relyingPartyResources.size());

                assertAll(
                    () -> assertTrue(relyingPartyResources.contains(Converter.toResource(relyingParty1))),
                    () -> assertTrue(relyingPartyResources.contains(Converter.toResource(relyingParty2)))
                );
            }

            @Test
            @DisplayName("then search successful when search term exists in multiple RPs")
            void testSearchMultipleRpsWithSameNamePrefix() throws Exception {
                RelyingParty relyingParty1 = generateRelyingPartyNoId();
                RelyingParty relyingParty2 = generateRelyingPartyNoId();
                RelyingParty relyingParty3 = generateRelyingPartyNoId();

                String searchStr = generateName();
                relyingParty1.setName(searchStr + relyingParty1.getName());
                relyingParty3.setName(searchStr + relyingParty3.getName());

                relyingPartyRepository.saveAll(List.of(relyingParty1, relyingParty2, relyingParty3));

                SearchRelyingPartyResource searchResource =
                    SearchRelyingPartyResource.empty().withSearchTerm(searchStr);

                String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                ResultActions actions =
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.relying_parties").exists());

                List<RelyingPartyResource> relyingPartyResources =
                    ApiTestUtils.toRelyingPartiesResource(actions).relyingParties();
                assertEquals(2, relyingPartyResources.size());

                assertAll(
                    () -> assertTrue(relyingPartyResources.contains(Converter.toResource(relyingParty1))),
                    () -> assertTrue(relyingPartyResources.contains(Converter.toResource(relyingParty3)))
                );
            }


        }


        @Nested
        @DisplayName("When using the advanced search ...")
        class AdvancedSearchTests {

            @Nested
            @DisplayName("Given include_inactives = false ...")
            class AdvancedSearchWithInactiveFalseTests {

                @Test
                @DisplayName("then search is successful when orgno exists for active RP")
                void testSearchByExistentOrgno() throws Exception {
                    RelyingParty relyingParty = generateRelyingPartyNoId();
                    relyingPartyRepository.save(relyingParty);

                    AdvancedSearchRelyingPartyResource searchResource =
                        AdvancedSearchRelyingPartyResource.empty().withOrgno(relyingParty.getOrgno());

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                    ResultActions actions =
                        mockMvc.perform(post("/v1/rp/search/advanced")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                                            .content(searchJson))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.relying_parties").exists());

                    List<RelyingPartyResource> relyingPartyResources =
                        ApiTestUtils.toRelyingPartiesResource(actions).relyingParties();
                    assertEquals(1, relyingPartyResources.size());

                    RelyingPartyResource relyingPartyResource = relyingPartyResources.getFirst();
                    assertAll(
                        () -> assertEquals(relyingParty.getOrgno(), relyingPartyResource.orgNr()),
                        () -> assertEquals(relyingParty.getName(), relyingPartyResource.name()),
                        () -> assertEquals(relyingParty.getPublicSector(), relyingPartyResource.publicSector())
                    );
                }

                @Test
                @DisplayName("then search succeeds with empty result when orgno associated "
                                 + "with an inactive RP")
                void testSearchProperlyIgnoresInactiveRelyingParty() throws Exception {
                    RelyingParty relyingParty = generateRelyingPartyNoId();
                    relyingParty.setActive(false);
                    relyingPartyRepository.save(relyingParty);

                    AdvancedSearchRelyingPartyResource searchResource =
                        AdvancedSearchRelyingPartyResource.empty().withOrgno(relyingParty.getOrgno());

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                    mockMvc.perform(post("/v1/rp/search/advanced")
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

                    AdvancedSearchRelyingPartyResource searchResource =
                        AdvancedSearchRelyingPartyResource.empty()
                                                          .withOrgno(relyingParty.getOrgno())
                                                          .withPublicSector(!relyingParty.getPublicSector());

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                    mockMvc.perform(post("/v1/rp/search/advanced")
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

                    String nonexistentOrgno = TestDataGenerator.generateValidOrgno();

                    AdvancedSearchRelyingPartyResource searchResource =
                        AdvancedSearchRelyingPartyResource.empty().withOrgno(nonexistentOrgno);

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                    mockMvc.perform(post("/v1/rp/search/advanced")
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
            class AdvancedSearchWithIncludeInactiveTrueTests {

                @ParameterizedTest
                @ValueSource(booleans = {true, false})
                @DisplayName("then searching for active/inactive relying party succeeds when "
                                 + "include_inactive = true is specified")
                void testSearchWithIncludeInactiveTrue(boolean relyingPartyIsActive) throws Exception {
                    RelyingParty relyingParty = generateRelyingPartyNoId();
                    relyingParty.setActive(relyingPartyIsActive);
                    relyingPartyRepository.save(relyingParty);

                    AdvancedSearchRelyingPartyResource searchResource =
                        AdvancedSearchRelyingPartyResource.empty()
                                                          .withOrgno(relyingParty.getOrgno())
                                                          .withIncludeInactive(true);

                    String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);

                    ResultActions actions =
                        mockMvc.perform(post("/v1/rp/search/advanced")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                                            .content(searchJson))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.relying_parties").exists());

                    List<RelyingPartyResource> relyingPartyResources =
                        ApiTestUtils.toRelyingPartiesResource(actions).relyingParties();
                    assertEquals(1, relyingPartyResources.size());

                    RelyingPartyResource relyingPartyResource = relyingPartyResources.getFirst();
                    assertAll(
                        () -> assertEquals(relyingParty.getOrgno(), relyingPartyResource.orgNr()),
                        () -> assertEquals(relyingParty.getName(), relyingPartyResource.name()),
                        () -> assertEquals(relyingParty.getPublicSector(), relyingPartyResource.publicSector())
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
