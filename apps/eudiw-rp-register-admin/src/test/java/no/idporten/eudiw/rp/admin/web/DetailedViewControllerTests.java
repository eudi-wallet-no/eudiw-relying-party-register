package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.DetailedViewController;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEditForm;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the RP detailed view and edit controller")
@AutoConfigureMockMvc(addFilters = false)
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

    @Nested
    @DisplayName("When GET'ing the /details/edit endpoint for a given RP ID ...")
    class EditEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the expected edit form and RP is loaded")
        void testCorrectViewAndModelAttributes() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            RelyingPartyEditForm expectedEditForm =
                RelyingPartyEditForm.prefillFromRelyingPartyResource(rpResource);

            mockMvc.perform(get("/details/%s/edit".formatted(id)))
                .andExpect(status().isOk())
                .andExpect(view().name("edit_form_view"))
                .andExpect(model().attribute(DetailedViewController.editFormAttrId, expectedEditForm))
                .andExpect(model().attribute(DetailedViewController.detailedViewDataAttrId, rpResource));

            verify(mockRpService).get(eq(id));
        }
    }

    @Nested
    @DisplayName("When POST'ing edit forms to the /details/edit endpoint for a given RP ID ...")
    class EditEndpointPostTests {
        @Test
        @DisplayName("then form accepted if well-formed, and correct services called, view, and model")
        void testEditFormAcceptedIfWellFormed() throws Exception {
            RelyingPartyEntitlementResource entitlement = ResourceGenerator.generateRelyingPartyEntitlementResource();
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource()
                                 .withRelyingPartyEntitlements(List.of(entitlement))
                                 .withRelyingPartyEaas(List.of());
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            RelyingPartyEditForm editForm =
                RelyingPartyEditForm.prefillFromRelyingPartyResource(rpResource);

            mockMvc.perform(post("/details/%s/edit".formatted(id))
                                .formField("name", editForm.getName())
                                .formField("publicSector", Boolean.toString(editForm.isPublicSector()))
                                .formField("active", Boolean.toString(editForm.isActive()))
                                .formField("entitlements", entitlement.entitlement()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/details/" + id));

            EditRelyingPartyResource expectedEditResource = editForm.toResource();
            verify(mockRpService).edit(id, expectedEditResource);
        }

        @Test
        @DisplayName("then form rejected on invalid fields, and view returns to the edit form")
        void testEditFormRejectedOnFieldInvalidation() throws Exception {
            RelyingPartyEntitlementResource entitlement = ResourceGenerator.generateRelyingPartyEntitlementResource();
            String invalidName = "fooBar$";
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource()
                    .withName(invalidName)
                    .withRelyingPartyEntitlements(List.of(entitlement))
                    .withRelyingPartyEaas(List.of());
            UUID id = UUID.randomUUID();
            when(mockRpService.get(id)).thenReturn(rpResource);

            RelyingPartyEditForm editForm =
                RelyingPartyEditForm.prefillFromRelyingPartyResource(rpResource);

            mockMvc.perform(post("/details/%s/edit".formatted(id))
                                .formField("name", invalidName)
                                .formField("publicSector", Boolean.toString(rpResource.publicSector()))
                                .formField("active", Boolean.toString(rpResource.active()))
                                .formField("entitlements", entitlement.entitlement()))
                   // assert edit form invalid (should only have error in the name field)
                   .andExpect(model().attributeHasFieldErrors(DetailedViewController.editFormAttrId, "name"))
                   .andExpect(model().attributeErrorCount(DetailedViewController.editFormAttrId, 1))

                   // assert that view returns to edit form, for the given RP and
                   // with the unsubmitted form data.
                   .andExpect(status().isOk())
                   .andExpect(view().name("edit_form_view"))
                   .andExpect(model().attribute(DetailedViewController.detailedViewDataAttrId, rpResource))
                   .andExpect(model().attribute(DetailedViewController.editFormAttrId, editForm));

            verify(mockRpService).get(id);
        }
    }
}
