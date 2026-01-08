package no.idporten.eudiw.rp.admin.web.selfservice;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminCreateRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.utils.WebTestUtils;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the /create controller as selfservice user")
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
        @DisplayName("then create view shown if user is authenticated")
        void testEditViewShownWhenUserLoggedIn() throws Exception {
            mockMvc.perform(get("/create").with(SecurityTestUtils.oidcLoginForOrgno(TestDataGenerator.generateValidOrgno())))
                   .andExpectAll(
                       status().isOk(),
                       view().name("create_form_view"));
        }
    }

    @Nested
    @DisplayName("When POST'ing create forms to the /admin/create endpoint")
    class AdminCreateEndpointPostTests {
        @Test
        @DisplayName("then form accepted if user is logged in, and unexpected entitlements are ignored")
        void testCreateFormAcceptedIfWellFormed() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();

            when(mockRpService.create(any())).thenReturn(rpResource);

            AdminCreateRelyingPartyForm createForm = new AdminCreateRelyingPartyForm(
                rpResource.orgno(),
                rpResource.tradeName(),
                rpResource.relyingPartyEntitlements().stream().map(RelyingPartyEntitlementFormField::fromResource).toList(),
                rpResource.relyingPartyEaas().stream().map(RelyingPartyEaaFormField::fromResource).toList()
            );
            CreateRelyingPartyResource createResource = createForm.toResource();

            when(mockRpService.create(createResource)).thenReturn(rpResource);
            var request = post("/create").with(SecurityTestUtils.oidcLoginForOrgno(rpResource.orgno())).with(csrf());
            mockMvc.perform(WebTestUtils.withCreateForm(request, createForm))
                   .andExpectAll(
                       status().is3xxRedirection(),
                       redirectedUrl("/details/" + rpResource.id()));

            CreateRelyingPartyResource expectedCreateResource =
                createResource.withRelyingPartyEntitlements(List.of(
                    new RelyingPartyEntitlementResource("https://uri.etsi.org/19475/Entitlement/Service_Provider", null, null)));

            verify(mockRpService, times(1)).create(eq(expectedCreateResource));
        }
    }
}
