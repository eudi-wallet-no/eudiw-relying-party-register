package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.CreateController;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminCreateRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the admin create controller")
@AutoConfigureMockMvc
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
                    model().attribute(CreateController.createFormAttrId, createForm));
        }
    }

    @Nested
    @DisplayName("When POST'ing create forms to the /admin/create endpoint")
    class AdminCreateEndpointPostTests {
        @Test
        @DisplayName("then form accepted if well-formed, and correct services called, view, and model")
        void testCreateFormAcceptedIfWellFormed() throws Exception {
            RelyingPartyEntitlementResource entitlement = ResourceGenerator.generateRelyingPartyEntitlementResource();

            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource()
                                 .withRelyingPartyEntitlements(List.of(entitlement))
                                 .withRelyingPartyEaas(List.of());
            CreateRelyingPartyResource createResource =
                new CreateRelyingPartyResource(
                    rpResource.orgno(),
                    rpResource.name(),
                    rpResource.publicSector(),
                    rpResource.relyingPartyEntitlements(),
                    rpResource.relyingPartyEaas());

            when(mockRpService.create(createResource)).thenReturn(rpResource);
            mockMvc.perform(post("/admin/create")
                    .formField("orgno", createResource.orgno())
                    .formField("name", createResource.name())
                    .formField("publicSector",
                               Boolean.toString(createResource.publicSector()))
                    .formField("entitlements", entitlement.entitlement()))
                .andExpectAll(
                    status().is3xxRedirection(),
                    redirectedUrl("/details/" + rpResource.id()));

            verify(mockRpService).create(createResource);
        }
    }
}
