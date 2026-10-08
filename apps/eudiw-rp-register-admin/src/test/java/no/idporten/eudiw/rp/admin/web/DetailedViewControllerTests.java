package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.DetailedViewController;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummaryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the RP detailed view controller")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class DetailedViewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelyingPartyCertificateSummaryBuilder relyingPartyCertificateSummaryBuilder;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @MockitoBean
    private CredentialsService mockCredentialsService;

    @Nested
    @DisplayName("when GET'ing the /details endpoint for a given RP ID ...")
    class DetailsEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the correct RP and certificates is loaded")
        public void testCorrectViewAndModelAttributes()
            throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();

            when(mockRpService.get(id)).thenReturn(rpResource);

            var expectedAccessCerts =
                rpResource.accessCertificates()
                          .stream()
                          .map(relyingPartyCertificateSummaryBuilder::build)
                          .toList();
            var expectedIssuerCerts =
                rpResource.issuerCertificates()
                          .stream()
                          .map(relyingPartyCertificateSummaryBuilder::build)
                          .toList();

            mockMvc.perform(get("/details/" + id))
                   .andExpect(status().isOk())
                   .andExpect(view().name("details_view"))
                   .andExpect(model().attribute(DetailedViewController.DETAILED_VIEW_DATA_ATTR, rpResource))
                   .andExpect(model().attribute(DetailedViewController.ACCESS_CERTIFICATE_SUMMARIES_ATTR, expectedAccessCerts))
                   .andExpect(model().attribute(DetailedViewController.ISSUER_CERTIFICATE_SUMMARIES_ATTR, expectedIssuerCerts));

            verify(mockRpService).get(eq(id));
        }
    }
}
