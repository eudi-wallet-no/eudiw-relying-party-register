package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.DetailedViewController;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificatesResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the RP detailed view controller")
@AutoConfigureMockMvc
public class DetailedViewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

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

            RelyingPartyCertificatesResource certsResource =
                new RelyingPartyCertificatesResource(List.of(
                    ResourceGenerator.generateCertificateResource()));
            when(mockRpService.getCertificatesForRelyingParty(id))
                .thenReturn(certsResource);
            RelyingPartyEntitlementsResource resource = new RelyingPartyEntitlementsResource(List.of());
            when(mockRpService.getIssuerCertificateForRelyingParty(id))
                .thenReturn(resource);

            mockMvc.perform(get("/details/" + id))
                   .andExpect(status().isOk())
                   .andExpect(view().name("details_view"))
                   .andExpect(model().attribute(DetailedViewController.detailedViewDataAttrId, rpResource))
                   .andExpect(model().attribute(DetailedViewController.certificateSummariesAttrId, certsResource.toSummaries()));

            verify(mockRpService).get(eq(id));
            verify(mockRpService).getCertificatesForRelyingParty(eq(id));
        }
    }
}
