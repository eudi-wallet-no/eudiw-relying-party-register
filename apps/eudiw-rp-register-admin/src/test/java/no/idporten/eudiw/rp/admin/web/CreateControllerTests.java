package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.CreateController;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminCreateRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.utils.WebTestUtils;
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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the admin create controller")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class CreateControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @Nested
    @DisplayName("When GET'ing the /create endpoint")
    class CreateEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the expected create form is loaded")
        void testCorrectViewAndModelAttributes() throws Exception {
            AdminCreateRelyingPartyForm createForm = new AdminCreateRelyingPartyForm();

            mockMvc.perform(get("/create"))
                .andExpectAll(
                    status().isOk(),
                    view().name("create_form_view"),
                    model().attribute(CreateController.CREATE_FORM_ATTR, createForm));
        }
    }

    @Nested
    @DisplayName("When POST'ing create forms to the /admin/create endpoint")
    class AdminCreateEndpointPostTests {
        @Test
        @DisplayName("then form accepted if well-formed, and correct services called, view, and model")
        void testCreateFormAcceptedIfWellFormed() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();

            AdminCreateRelyingPartyForm createForm = new AdminCreateRelyingPartyForm(
                rpResource.orgno(),
                rpResource.tradeName(),
                rpResource.relyingPartyEntitlements().stream().map(RelyingPartyEntitlementFormField::fromResource).toList(),
                rpResource.relyingPartyEaas().stream().map(RelyingPartyEaaFormField::fromResource).toList()
            );
            CreateRelyingPartyResource createResource = createForm.toResource();

            when(mockRpService.create(createResource)).thenReturn(rpResource);
            var request = post("/admin/create").with(csrf());
            mockMvc.perform(WebTestUtils.withCreateForm(request, createForm))
                .andExpectAll(
                    status().is3xxRedirection(),
                    redirectedUrl("/details/" + rpResource.id()));

            verify(mockRpService).create(createResource);
        }
    }
}
