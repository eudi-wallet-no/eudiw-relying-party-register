package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.EditController;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.utils.WebTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the RP edit controller")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class EditControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @Nested
    @DisplayName("When GET'ing the /edit endpoint for a given RP ID ...")
    class EditEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the expected edit form and RP is loaded")
        void testCorrectViewAndModelAttributes() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            AdminEditRelyingPartyForm expectedEditForm =
                AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(rpResource);

            mockMvc.perform(get("/edit/%s".formatted(id)))
                .andExpect(status().isOk())
                .andExpect(view().name("edit_form_view"))
                .andExpect(model().attribute(EditController.EDIT_FORM_ATTR, expectedEditForm))
                .andExpect(model().attribute(EditController.DETAILED_VIEW_DATA_ATTR, rpResource));

            verify(mockRpService).get(eq(id));
        }
    }

    @Nested
    @DisplayName("When POST'ing edit forms to the /admin/edit endpoint for a given RP ID ...")
    class EditEndpointPostTests {
        @Test
        @DisplayName("then form accepted if well-formed, and correct services called, view, and model")
        void testEditFormAcceptedIfWellFormed() throws Exception {
            RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();

            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            AdminEditRelyingPartyForm editForm = AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(rpResource);

            var request = post("/admin/edit/%s".formatted(id)).with(csrf());
            mockMvc.perform(WebTestUtils.withEditForm(request, editForm))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/details/" + id));

            EditRelyingPartyResource expectedEditResource = editForm.toResource();
            verify(mockRpService).edit(id, expectedEditResource);
        }

        @Test
        @DisplayName("then form rejected on invalid fields, and view returns to the edit form")
        void testEditFormRejectedOnFieldInvalidation() throws Exception {
            String invalidName = "fooBar$";
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource()
                    .withTradeName(invalidName);
            UUID id = UUID.randomUUID();
            when(mockRpService.get(id)).thenReturn(rpResource);

            AdminEditRelyingPartyForm editForm =
                AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(rpResource);

            var request = post("/admin/edit/%s".formatted(id)).with(csrf());
            mockMvc.perform(WebTestUtils.withEditForm(request, editForm))
                   // assert edit form invalid (should only have error in the name field)
                   .andExpect(model().attributeHasFieldErrors(EditController.EDIT_FORM_ATTR, "tradeName"))
                   .andExpect(model().attributeErrorCount(EditController.EDIT_FORM_ATTR, 1))

                   // assert that view returns to edit form, for the given RP and
                   // with the unsubmitted form data.
                   .andExpect(status().isOk())
                   .andExpect(view().name("edit_form_view"))
                   .andExpect(model().attribute(EditController.DETAILED_VIEW_DATA_ATTR, rpResource))
                   .andExpect(model().attribute(EditController.EDIT_FORM_ATTR, editForm));

            verify(mockRpService).get(id);
        }
    }
}
