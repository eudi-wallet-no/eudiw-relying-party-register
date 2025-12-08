package no.eudiw.rp.register.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.service.RelyingPartyService;
import no.eudiw.rp.register.data.service.exception.NotFoundException;
import no.eudiw.rp.register.testdata.ResourceGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static no.eudiw.rp.register.testdata.ResourceGenerator.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the Relying Parties API")
@ActiveProfiles("junit")
public class RelyingPartiesControllerTest {

    public static final String X_API_KEY_HEADER = "X-API-KEY";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    @SuppressWarnings("unused")
    private RelyingPartyService relyingPartyService;

    @DisplayName("When using the Relying Parties API with valid API key")
    @Nested
    class APITests {

        public static final String VALID_API_KEY = "junit-api-key";

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Nested
        @DisplayName("When using the create endpoint ...")
        class CreateTests {
            @Test
            @DisplayName("with a valid create resource for a known ID")
            void testCreateRelyingParty() throws Exception {
                CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

                RelyingPartyResource expectedResponse = ResourceGenerator.generateRelyingPartyResource();
                when(relyingPartyService.createRelyingParty(any())).thenReturn(expectedResponse);

                String json = objectMapper.writeValueAsString(resource);

                RelyingPartyResource response =
                    ApiTestUtils.toRelyingPartyResource(
                        mockMvc.perform(post("/v1/rp")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                                            .content(json))
                               .andExpect(status().isOk()));

                assertEquals(expectedResponse, response);

                verify(relyingPartyService, times(1)).createRelyingParty(resource);
                verifyNoMoreInteractions(relyingPartyService);
            }

            @Test
            @DisplayName("with a valid create resource for an unknown ID")
            void testCreateRelyingPartyUnknownId() throws Exception {
                when(relyingPartyService.createRelyingParty(any())).thenThrow(new NotFoundException(""));

                CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();
                String json = objectMapper.writeValueAsString(resource);

                mockMvc.perform(post("/v1/rp")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(json))
                       .andExpect(status().isNotFound());

                verify(relyingPartyService, times(1)).createRelyingParty(resource);
                verifyNoMoreInteractions(relyingPartyService);
            }
            @Test
            @DisplayName("with an invalid create resource")
            void testCreateRelyingPartyInvalidOrgno() throws Exception {
                CreateRelyingPartyResource resource =
                    generateCreateRelyingPartyResource().withOrgNr(generateInvalidOrgno());

                String json = objectMapper.writeValueAsString(resource);

                mockMvc.perform(post("/v1/rp")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(json))
                       .andExpect(status().isBadRequest())
                       .andExpect(jsonPath("$.error").value("invalid_request"))
                       .andExpect(jsonPath("$.error_description", containsString("invalid_orgnr")));
                verifyNoInteractions(relyingPartyService);
            }
        }

        @Nested
        @DisplayName("When using the Get RP endpoint ...")
        class RetrieveTests {

            @Test
            @DisplayName("with a valid and known ID")
            void testGetRelyingPartyValidAndKnownID() throws Exception {
                RelyingPartyResource expectedResponse = ResourceGenerator.generateRelyingPartyResource();
                when(relyingPartyService.findRelyingParty(any())).thenReturn(expectedResponse);

                RelyingPartyResource response =
                    ApiTestUtils.toRelyingPartyResource(
                        mockMvc.perform(get("/v1/rp/" + expectedResponse.id())
                                            .accept(MediaType.APPLICATION_JSON)
                                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                               .andExpect(status().isOk()));

                assertEquals(expectedResponse, response);

                verify(relyingPartyService, times(1)).findRelyingParty(expectedResponse.id());
                verifyNoMoreInteractions(relyingPartyService);
            }

            @Test
            @DisplayName("with a valid but unknown ID")
            void testGetRelyingPartyValidButUnknownID() throws Exception {
                when(relyingPartyService.findRelyingParty(any())).thenThrow(new NotFoundException(""));

                UUID id = UUID.randomUUID();
                mockMvc.perform(get("/v1/rp/" + id)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY))
                       .andExpect(status().isNotFound());

                verify(relyingPartyService, times(1)).findRelyingParty(id);
            }

            @Test
            @DisplayName("with an invalid ID")
            void testGetRelyingPartyInValidID() throws Exception {

                String invalidId = UUID.randomUUID().toString().substring(0, 10);
                mockMvc.perform(get("/v1/rp/" + invalidId)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY))
                       .andExpect(status().isBadRequest());

                verifyNoInteractions(relyingPartyService);
            }
        }

        @Nested
        @DisplayName("When using the edit endpoint ...")
        class EditTests {
            @Test
            @DisplayName("with a valid edit resource for a known ID")
            void testEditRelyingPartyValidEditResourceKnownId() throws Exception {

                RelyingPartyResource expectedResponse = ResourceGenerator.generateRelyingPartyResource();
                when(relyingPartyService.updateRelyingParty(any(), any())).thenReturn(expectedResponse);

                EditRelyingPartyResource editResource = ResourceGenerator.generateEditRelyingPartyResource();
                String json = objectMapper.writeValueAsString(editResource);

                RelyingPartyResource response = ApiTestUtils.toRelyingPartyResource(
                    mockMvc.perform(put("/v1/rp/" + expectedResponse.id())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                                        .content(json))
                           .andExpect(status().isOk()));

                assertEquals(expectedResponse, response);
                verify(relyingPartyService, times(1)).updateRelyingParty(expectedResponse.id(), editResource);
                verifyNoMoreInteractions(relyingPartyService);
            }

            @Test
            @DisplayName("with a valid edit resource for an unknown ID")
            void testEditRelyingPartyValidEditResourceUnknownId() throws Exception {

                RelyingPartyResource expectedResponse = ResourceGenerator.generateRelyingPartyResource();
                when(relyingPartyService.updateRelyingParty(any(), any())).thenThrow(new NotFoundException(""));

                EditRelyingPartyResource editResource = ResourceGenerator.generateEditRelyingPartyResource();
                String json = objectMapper.writeValueAsString(editResource);

                mockMvc.perform(put("/v1/rp/" + expectedResponse.id())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(json))
                       .andExpect(status().isNotFound());

                verify(relyingPartyService, times(1)).updateRelyingParty(expectedResponse.id(), editResource);
                verifyNoMoreInteractions(relyingPartyService);
            }

            @Test
            @DisplayName("with invalid edit resource")
            void testEditRelyingPartyInvalidEditResource() throws Exception {

                EditRelyingPartyResource invalidEditResource =
                    ResourceGenerator.generateEditRelyingPartyResource()
                                     .withTradeName("foobar$");
                String json = objectMapper.writeValueAsString(invalidEditResource);

                UUID id = UUID.randomUUID();
                mockMvc.perform(put("/v1/rp/" + id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(json))
                       .andExpect(status().isBadRequest());

                verifyNoInteractions(relyingPartyService);
            }
        }


        @Nested
        @DisplayName("When using the search endpoint ...")
        class SearchTests {
            @Test
            @DisplayName("with a valid search resource")
            void testSearchValidSearchResource() throws Exception {

                List<RelyingPartyResource> dummySearchResult =
                    List.of(ResourceGenerator.generateRelyingPartyResource());
                when(relyingPartyService.searchRelyingParties(any()))
                    .thenReturn(new PagedModel<>(new PageImpl<>(dummySearchResult)));

                SearchRelyingPartyResource searchResource =
                    new SearchRelyingPartyResource(TestDataGenerator.generateName());

                String json = objectMapper.writeValueAsString(searchResource);
                mockMvc.perform(post("/v1/rp/search")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(json))
                       .andExpect(status().isOk());

                verify(relyingPartyService, times(1)).searchRelyingParties(searchResource);
                verifyNoMoreInteractions(relyingPartyService);
            }

            @Test
            @DisplayName("with an inalid search resource")
            void testSearchInvalidSearchResource() throws Exception {

                String invalidSearchResourceJson = "{ \"search_term\": null }";

                mockMvc.perform(post("/v1/rp/search")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                                    .content(invalidSearchResourceJson))
                       .andExpect(status().isBadRequest());
                verifyNoInteractions(relyingPartyService);
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
