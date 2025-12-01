package no.eudiw.rp.register.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.data.entity.RelyingPartyEaa;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.repository.LegalEntityRepository;
import no.eudiw.rp.register.data.service.Converter;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.testdata.ResourceGenerator;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static no.eudiw.rp.register.api.ApiTestUtils.toPage;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

import static no.eudiw.rp.register.testdata.ResourceGenerator.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the Relying Parties API")
@ActiveProfiles("junit")
public class RelyingPartiesControllerTest {

    public static final String X_API_KEY_HEADER = "X-API-KEY";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LegalEntityRepository legalEntityRepository;

    @Autowired
    private Converter converter;

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
        }

        @Nested
        @DisplayName("When deleting a relying party ...")
        class DeleteTests {
//            @Test
//            void testDeleteRelyingParty() throws Exception {
//                CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();
//                ObjectWriter ow = new ObjectMapper().writer();
//                String json = ow.writeValueAsString(resource);
//
//                ResultActions createResult =
//                        mockMvc.perform(post("/v1/rp")
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
//                                        .content(json))
//                                .andExpect(status().isOk())
//                                .andExpect(jsonPath("$.org_nr").value(resource.orgNr()))
//                                .andExpect(jsonPath("$.name").value(resource.name()))
//                                .andExpect(jsonPath("$.public_sector").value(resource.publicSector()));
//
//                RelyingPartyResource relyingPartyResource = ApiTestUtils.toRelyingPartyResource(createResult);
//
//                mockMvc.perform(delete("/v1/rp/" + relyingPartyResource.id())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .header(X_API_KEY_HEADER, VALID_API_KEY)
//                        )
//                        .andExpect(status().isNoContent());
//
//                mockMvc.perform(get("/v1/rp/" + relyingPartyResource.id())
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header(X_API_KEY_HEADER, VALID_API_KEY)
//                        )
//                        .andExpect(status().isGone());
//            }

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
                        .andExpect(jsonPath("$.name").value(editResource.name()));
            }

            @Test
            void testEditRelyingPartyMoreEntitlements() throws Exception {
                List<RelyingPartyEntitlementResource> sampleEntitlements =
                    ResourceGenerator.sampleRelyingPartyEntitlementResources(5);
                RelyingPartyEntitlementResource sampleEntitlement = sampleEntitlements.getFirst();
                List<RelyingPartyEntitlementResource> initialEntitlements =
                    sampleEntitlements.subList(1, 3);

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
                entitlements.add(sampleEntitlement);
                EditRelyingPartyResource editResource =
                    generateEditRelyingPartyResource()
                        .withRelyingPartyEntitlements(entitlements)
                        .withRelyingPartyEaas(resource.relyingPartyEaas());

                mockMvc.perform(put("/v1/rp/" + relyingPartyResource.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                                .content(ow.writeValueAsString(editResource)))
                        .andExpect(status().isOk())
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
                        .andExpect(jsonPath("$.name").value(editResource.name()));
                assertTrue(ApiTestUtils.toRelyingPartyResource(editResult).relyingPartyEntitlements().isEmpty());
            }
        }

        @Nested
        @DisplayName("When searching for relying parties ...")
        class SearchTests {
            @Test
            @DisplayName("then search successful when search term exists in an RP orgno")
            void testSearchTermExistsInRpOrgno() throws Exception {
                legalEntityRepository.deleteAll();
                LegalEntity legalEntity = EntityGenerator.generateRelyingParty();
                legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider")));
                legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEaas(List.of(new RelyingPartyEaa("noe", "noe")));
                legalEntityRepository.saveAndFlush(legalEntity);

                SearchRelyingPartyResource searchResource =
                    new SearchRelyingPartyResource(legalEntity.getRelyingPartyInstances().getFirst().getTradeName());

                String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                ResultActions actions =
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.content").exists());

                List<RelyingPartyResource> relyingPartyResources =
                    toPage(actions, RelyingPartyResource.class).content();
                assertEquals(1, relyingPartyResources.size());

                RelyingPartyResource relyingPartyResource = relyingPartyResources.getFirst();
                assertAll(
                    () -> assertEquals(legalEntity.getName(), relyingPartyResource.orgName())
                );

                legalEntityRepository.delete(legalEntity);
            }

            @Test
            @DisplayName("then search successful when search term exists in an RP orgno")
            void testSearchTermExistsInRpName() throws Exception {
                legalEntityRepository.deleteAll();
                LegalEntity legalEntity = EntityGenerator.generateRelyingParty();
                legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider")));
                legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEaas(List.of(new RelyingPartyEaa("noe", "noe")));
                legalEntityRepository.saveAndFlush(legalEntity);

                SearchRelyingPartyResource searchResource =
                    new SearchRelyingPartyResource(legalEntity.getRelyingPartyInstances().getFirst().getTradeName());

                String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                ResultActions actions =
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.content").exists());

                List<RelyingPartyResource> relyingPartyResources =
                    toPage(actions, RelyingPartyResource.class).content();
                assertEquals(1, relyingPartyResources.size());

                RelyingPartyResource relyingPartyResource = relyingPartyResources.getFirst();
                assertAll(
                    () -> assertEquals(legalEntity.getName(), relyingPartyResource.orgName())
                );

                legalEntityRepository.delete(legalEntity);
            }

            @Test
            @DisplayName("then search successful when search term exists in multiple RPs")
            void testSearchTermExistsInDifferentFieldsOfDifferentRps() throws Exception {
                LegalEntity legalEntity1 = EntityGenerator.generateRelyingParty();
                LegalEntity legalEntity2 = EntityGenerator.generateRelyingParty();
                LegalEntity legalEntity3 = EntityGenerator.generateRelyingParty();

                // append some of rp1's orgno to the start of rp2's name
                String searchStr = legalEntity1.getRelyingPartyInstances().getFirst().getTradeName().substring(0, 4);
                legalEntity2.getRelyingPartyInstances().getFirst().setTradeName(searchStr + legalEntity2.getRelyingPartyInstances().getFirst().getTradeName());

                legalEntityRepository.saveAll(List.of(legalEntity1, legalEntity2, legalEntity3));

                SearchRelyingPartyResource searchResource =
                    new SearchRelyingPartyResource(searchStr);

                String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                ResultActions actions =
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.content").exists());

                List<RelyingPartyResource> relyingPartyResources =
                    toPage(actions, RelyingPartyResource.class).content();
                assertEquals(2, relyingPartyResources.size());

                assertAll(
                    () -> assertTrue(relyingPartyResources.contains(converter.toResource(legalEntity1.getRelyingPartyInstances().getFirst()))),
                    () -> assertTrue(relyingPartyResources.contains(converter.toResource(legalEntity2.getRelyingPartyInstances().getFirst())))
                );
            }

            @Test
            @DisplayName("then search successful when search term exists in multiple RPs")
            void testSearchMultipleRpsWithSameNamePrefix() throws Exception {
                LegalEntity legalEntity1 = EntityGenerator.generateRelyingParty();
                LegalEntity legalEntity2 = EntityGenerator.generateRelyingParty();
                LegalEntity legalEntity3 = EntityGenerator.generateRelyingParty();

                String searchStr = generateName();
                legalEntity1.getRelyingPartyInstances().getFirst().setTradeName(searchStr + legalEntity1.getRelyingPartyInstances().getFirst().getTradeName());
                legalEntity3.getRelyingPartyInstances().getFirst().setTradeName(searchStr + legalEntity3.getRelyingPartyInstances().getFirst().getTradeName());

                legalEntityRepository.saveAll(List.of(legalEntity1, legalEntity2, legalEntity3));

                SearchRelyingPartyResource searchResource =
                    new SearchRelyingPartyResource(searchStr);

                String searchJson = new ObjectMapper().writer().writeValueAsString(searchResource);
                ResultActions actions =
                    mockMvc.perform(post("/v1/rp/search")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(searchJson))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$.content").exists());

                List<RelyingPartyResource> relyingPartyResources =
                    toPage(actions, RelyingPartyResource.class).content();

                assertEquals(2, relyingPartyResources.size());

                assertAll(
                    () -> assertTrue(relyingPartyResources.contains(converter.toResource(legalEntity1.getRelyingPartyInstances().getFirst()))),
                    () -> assertTrue(relyingPartyResources.contains(converter.toResource(legalEntity3.getRelyingPartyInstances().getFirst())))
                );
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
