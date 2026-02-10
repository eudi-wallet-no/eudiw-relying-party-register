package no.idporten.eudiw.rp.admin.web.selfservice;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.admin.web.form.BaseEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.utils.WebTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
@DisplayName("When using the /edit controller as selfservice user")
@AutoConfigureMockMvc
public class EditControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @Nested
    @DisplayName("When GET'ing the /edit endpoint for a given RP ...")
    class EditEndpointGetTests {
        @Test
        @DisplayName("then the edit view is shown when user has authority for the given orgno")
        void testEditViewShownWhenUserLoggedIn() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            var oidcLogin = SecurityTestUtils.oidcLoginForOrgno(rpResource.orgno());
            mockMvc.perform(get("/edit/%s".formatted(id)).with(oidcLogin))
                .andExpect(status().isOk())
                .andExpect(view().name("edit_form_view"));

            verify(mockRpService).get(eq(id));
        }

        @Test
        @DisplayName("then 404 view shown when user does not have authority for the given orgno")
        void testCorrectViewAndModelAttributes() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            var oidcLoginForOtherOrgno = SecurityTestUtils.oidcLoginForOrgno(TestDataGenerator.generateValidOrgno());
            mockMvc.perform(get("/edit/%s".formatted(id)).with(oidcLoginForOtherOrgno))
                   .andExpect(status().isNotFound())
                   .andExpect(view().name("error/404"));

            verify(mockRpService).get(eq(id));
        }
    }

    @Nested
    @DisplayName("When POST'ing edit forms to the /edit endpoint for a given RP ID ...")
    class EditEndpointPostTests {
        @Test
        @DisplayName("then form accepted if user has authority to the given orgno, and extra entitlements are ignored")
        void testEditPostAcceptedIfUserHasAuthorityToOrgno() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            BaseEditRelyingPartyForm editForm = new BaseEditRelyingPartyForm();
            editForm.setTradeName(rpResource.tradeName());
            editForm.setEaas(rpResource.relyingPartyEaas().stream().map(RelyingPartyEaaFormField::fromResource).toList());
            editForm.setEntitlements(rpResource.relyingPartyEntitlements().stream().map(RelyingPartyEntitlementFormField::fromResource).toList());

            var oidcLogin = SecurityTestUtils.oidcLoginForOrgno(rpResource.orgno());
            var request = post("/edit/%s".formatted(id))
                              .with(csrf())
                              .with(oidcLogin);
            mockMvc.perform(WebTestUtils.withEditForm(request, editForm))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/details/" + id));

            EditRelyingPartyResource expectedEditResource =
                editForm.toResource().withActive(rpResource.active());

            verify(mockRpService).edit(id, expectedEditResource);
        }

        @Test
        @DisplayName("then authorization error thrown when edit form contains illegal entitlements changes")
        void testAuthorizationErrorWhenEditFormContainsInvalidEntitlementChanges() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            BaseEditRelyingPartyForm editForm = new BaseEditRelyingPartyForm();
            editForm.setTradeName(rpResource.tradeName());
            editForm.setEaas(rpResource.relyingPartyEaas().stream().map(RelyingPartyEaaFormField::fromResource).toList());

            List<RelyingPartyEntitlementFormField> illegalEntitlementFields =
                List.of(new RelyingPartyEntitlementFormField(
                    "invalid-extra-entitlement", TestDataGenerator.generateIssuerUrl(), null));
            editForm.setEntitlements(illegalEntitlementFields);

            var oidcLogin = SecurityTestUtils.oidcLoginForOrgno(rpResource.orgno());
            var request = post("/edit/%s".formatted(id))
                              .with(csrf())
                              .with(oidcLogin);
            mockMvc.perform(WebTestUtils.withEditForm(request, editForm))
                   .andExpect(status().isNotFound());

            verify(mockRpService, times(0)).edit(any(), any());
        }
    }
}
