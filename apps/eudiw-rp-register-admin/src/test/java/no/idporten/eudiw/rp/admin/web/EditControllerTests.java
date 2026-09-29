package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.AlreadyExistsException;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.EditController;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.utils.WebTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

        @Test
        @DisplayName("then form rejected when EAA namespace contains a double quote")
        void testEditFormRejectedOnDoubleQuoteInEaaNamespace() throws Exception {
            RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            AdminEditRelyingPartyForm editForm =
                AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(rpResource);
            editForm.setEaas(List.of(new RelyingPartyEaaFormField("\"onerror", "test")));

            var request = post("/admin/edit/%s".formatted(id)).with(csrf());
            mockMvc.perform(WebTestUtils.withEditForm(request, editForm))
                   .andExpect(model().attributeHasFieldErrors(
                       EditController.EDIT_FORM_ATTR, "eaas[0].namespace"))
                   .andExpect(status().isOk())
                   .andExpect(view().name("edit_form_view"));

            verify(mockRpService, never()).edit(any(), any());
        }

        @Test
        @DisplayName("then HTTP 400 with bad request page when the register service rejects the request")
        void testEditFormReturnsBadRequestWhenServiceRejectsRequest() throws Exception {
            RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);
            when(mockRpService.edit(any(), any())).thenThrow(new BadRequestException("rejected"));

            AdminEditRelyingPartyForm editForm =
                AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(rpResource);

            var request = post("/admin/edit/%s".formatted(id)).with(csrf());
            mockMvc.perform(WebTestUtils.withEditForm(request, editForm))
                   .andExpect(status().isBadRequest())
                   .andExpect(view().name("error/bad_request"));
        }

        @Test
        @DisplayName("then HTTP 409 with conflict page when the register service reports a conflict")
        void testEditFormReturnsConflictWhenServiceReportsConflict() throws Exception {
            RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);
            when(mockRpService.edit(any(), any()))
                .thenThrow(new AlreadyExistsException("already exists"));

            AdminEditRelyingPartyForm editForm =
                AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(rpResource);

            var request = post("/admin/edit/%s".formatted(id)).with(csrf());
            mockMvc.perform(WebTestUtils.withEditForm(request, editForm))
                   .andExpect(status().isConflict())
                   .andExpect(view().name("error/conflict"));
        }

        @Test
        @DisplayName("then HTTP 500 with generic error page on unexpected service failure")
        void testEditFormReturnsServerErrorOnUnexpectedServiceFailure() throws Exception {
            RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);
            when(mockRpService.edit(any(), any()))
                .thenThrow(new UnrecognizedErrorResponseException("boom"));

            AdminEditRelyingPartyForm editForm =
                AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(rpResource);

            var request = post("/admin/edit/%s".formatted(id)).with(csrf());
            mockMvc.perform(WebTestUtils.withEditForm(request, editForm))
                   .andExpect(status().isInternalServerError())
                   .andExpect(view().name("error/5xx"));
        }

        @Test
        @DisplayName("then a raw request with an encoded double quote in the EAA namespace is rejected and never persisted")
        void testRawRequestWithEncodedDoubleQuoteIsRejected() throws Exception {
            RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            mockMvc.perform(post("/admin/edit/%s".formatted(id))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .content("tradeName=dos-repro"
                                             + "&entitlements%5B0%5D.entitlement=https%3A%2F%2Furi.etsi.org%2F19475%2FEntitlement%2FService_Provider"
                                             + "&eaas%5B0%5D.namespace=%22onerror"
                                             + "&eaas%5B0%5D.intent=test"))
                   .andExpect(model().attributeHasFieldErrors(
                       EditController.EDIT_FORM_ATTR, "eaas[0].namespace"))
                   .andExpect(status().isOk())
                   .andExpect(view().name("edit_form_view"));

            verify(mockRpService, never()).edit(any(), any());
        }
    }
}
