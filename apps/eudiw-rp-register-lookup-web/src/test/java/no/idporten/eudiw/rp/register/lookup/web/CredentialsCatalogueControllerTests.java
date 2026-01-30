package no.idporten.eudiw.rp.register.lookup.web;

import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.service.exception.NotFoundException;
import no.idporten.eudiw.rp.register.lookup.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.register.lookup.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.register.lookup.web.controllers.CredentialsCatalogueController;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialsResource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the credentials catalogue controller")
@AutoConfigureMockMvc
public class CredentialsCatalogueControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private CredentialsService mockCredentialsService;

    @Nested
    @DisplayName("when GET'ing the /credentials-catalogue endpoint")
    class CredentialsCatalogueGetTests {

        private Comparator<CredentialResource> byNorwegianCredentialTypeDisplayName =
            Comparator.comparing(credential ->
                credential.getCredentialTypeDisplayName("no"));

        @Test
        @DisplayName("then credentials service invoked, and credentials view shown with default sorting")
        public void testGetCredentialsWithDefaultSorting() throws Exception {
            CredentialsResource credentialsResource = ResourceGenerator.generateCredentialsResource();
            when(mockCredentialsService.getAvailableCredentials()).thenReturn(credentialsResource);

            List<CredentialResource> credentialsWithDefaultSorting =
                credentialsResource
                    .credentials()
                    .stream()
                    .sorted(byNorwegianCredentialTypeDisplayName)
                    .toList();

            mockMvc.perform(get("/credentials-catalogue"))
                .andExpectAll(
                   status().isOk(),
                   view().name("credentials_view"),
                   model().attribute(CredentialsCatalogueController.credentialsAttrId,
                                     credentialsWithDefaultSorting)
                );

            verify(mockCredentialsService, times(1)).getAvailableCredentials();
        }

        @Nested
        @DisplayName("when the sort request parameter is specified")
        class WithSortRequestParameter {
            @Test
            @DisplayName("then credentials table is sorted accordingly")
            public void testGetCredentialsSortedByNumClaims() throws Exception {
                CredentialsResource credentialsResource = ResourceGenerator.generateCredentialsResource();
                when(mockCredentialsService.getAvailableCredentials()).thenReturn(credentialsResource);

                Comparator<CredentialResource> byNumClaims =
                    Comparator.comparing(cred -> cred.getMetadata().getClaims().size());

                List<CredentialResource> credentialsSortedByNumClaims =
                    credentialsResource
                        .credentials()
                        .stream()
                        .sorted(byNumClaims.thenComparing(byNorwegianCredentialTypeDisplayName))
                        .toList();

                mockMvc.perform(get("/credentials-catalogue?sort=num_claims"))
                       .andExpectAll(
                           status().isOk(),
                           view().name("credentials_view"),
                           model().attribute(CredentialsCatalogueController.credentialsAttrId,
                                             credentialsSortedByNumClaims)
                       );

                verify(mockCredentialsService, times(1)).getAvailableCredentials();
            }

            @Test
            @DisplayName("then default sorting is used when sort key is unrecognized")
            public void testGetCredentialsWithUnknownSortKey() throws Exception {
                CredentialsResource credentialsResource = ResourceGenerator.generateCredentialsResource();
                when(mockCredentialsService.getAvailableCredentials()).thenReturn(credentialsResource);

                List<CredentialResource> credentialsWithDefaultSorting =
                    credentialsResource
                        .credentials()
                        .stream()
                        .sorted(byNorwegianCredentialTypeDisplayName)
                        .toList();

                String unknownSortKey = TestDataGenerator.generateName();
                mockMvc.perform(get("/credentials-catalogue?sort=%s".formatted(unknownSortKey)))
                       .andExpectAll(
                           status().isOk(),
                           view().name("credentials_view"),
                           model().attribute(CredentialsCatalogueController.credentialsAttrId,
                                             credentialsWithDefaultSorting)
                       );

                verify(mockCredentialsService, times(1)).getAvailableCredentials();
            }
        }
    }

    @Nested
    @DisplayName("when GET'ing the /credential endpoint")
    class CredentialGetTests {

        @Test
        @DisplayName("then issuer-uri is decoded, credentials service is invoked, and model and view correct")
        public void testGetCredential() throws Exception {

            CredentialResource credentialResource = ResourceGenerator.generateCredentialResource();
            String issuer = "https://" + TestDataGenerator.generateName() + ".net";

            String configId = TestDataGenerator.generateName();
            when(mockCredentialsService.getCredential(issuer, configId))
                .thenReturn(credentialResource);

            mockMvc.perform(get("/credential")
                                .param("issuer", issuer)
                                .param("config-id", configId))
                   .andExpectAll(
                       status().isOk(),
                       view().name("credential_details_view"),
                       model().attribute(CredentialsCatalogueController.credentialAttrId,
                                         credentialResource)
                   );

            verify(mockCredentialsService, times(1)).getCredential(issuer, configId);
        }

        @Test
        @DisplayName("then 404 view shown when credential is not found")
        public void test404ThrownWhenCredentialNotExists() throws Exception {
            when(mockCredentialsService.getCredential(any(), any()))
                .thenThrow(NotFoundException.class);

            String issuer = "https:%2F%2F" + TestDataGenerator.generateName() + ".net";
            String configId = TestDataGenerator.generateName();

            String requestUri = "/credential/%s/%s".formatted(issuer, configId);
            System.out.println(requestUri);
            mockMvc.perform(get(requestUri))
                   .andExpectAll(
                       status().isNotFound(),
                       view().name("error/404"),
                       model().attributeDoesNotExist(CredentialsCatalogueController.credentialAttrId)
                   );
        }
    }
}
