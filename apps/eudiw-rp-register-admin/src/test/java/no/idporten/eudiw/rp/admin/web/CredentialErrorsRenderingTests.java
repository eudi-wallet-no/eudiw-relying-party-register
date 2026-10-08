package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.form.BaseEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.utils.WebTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.thymeleaf.enabled=true")
class CredentialErrorsRenderingTests {
    private static final String ISSUER = "https://agent.paradym.id/oid4vci/a3614c5c-aad7-4c72-b1be-2837ffd644af";
    @Autowired MockMvc mvc;
    @MockitoBean RelyingPartiesService rpService;
    @MockitoBean CredentialsService credentialsService;

    @Test
    void savedIssuerErrorsAppearOnDetailsAndRefreshWithoutAddingControlsToEditForm() throws Exception {
        var rp = ResourceGenerator.generateRelyingPartyResource()
                .withAccessCertificates(List.of()).withIssuerCertificates(List.of())
                .withRelyingPartyEaas(List.of()).withRelyingPartyEntitlements(List.of(
                        new RelyingPartyEntitlementResource("https://uri.etsi.org/19475/Entitlement/PID_Provider", "Utstedar", ISSUER)));
        when(rpService.get(rp.id())).thenReturn(rp);
        when(credentialsService.getCredentialErrors(ISSUER)).thenReturn(List.of(
                new CredentialsService.CredentialValidationError("EUCC <invalid>", List.of("credential_metadata.claims[0].path: invalid index"))));
        var login = SecurityTestUtils.oidcLoginForOrgno(rp.orgno());
        BaseEditRelyingPartyForm form = new BaseEditRelyingPartyForm();
        form.setTradeName(rp.tradeName());
        form.setEaas(List.of());
        form.setEntitlements(rp.relyingPartyEntitlements().stream().map(RelyingPartyEntitlementFormField::fromResource).toList());
        mvc.perform(WebTestUtils.withEditForm(post("/edit/{id}", rp.id()).with(login).with(csrf()), form))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/details/" + rp.id()));
        verifyNoInteractions(credentialsService);

        String html = mvc.perform(get("/details/{id}", rp.id()).with(login)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("EUCC &lt;invalid&gt;:", "credential_metadata.claims[0].path: invalid index");
        assertThat(html).doesNotContain("Sjekk bevis", "Metadata-URL:", "av 3 bevistypar", "Sjekka ", "EBWOID", "EUCC <invalid>");

        when(credentialsService.getCredentialErrors(ISSUER)).thenReturn(List.of());
        String refreshed = mvc.perform(get("/details/{id}", rp.id()).with(login)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(refreshed).doesNotContain("EUCC", "ds-validation-message");
        verify(credentialsService, times(2)).getCredentialErrors(ISSUER);

        String edit = mvc.perform(get("/edit/{id}", rp.id()).with(login)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(edit).doesNotContain("Sjekk bevis", "issuer_validation", "Bruk URL-en frå feltet");
    }

    @Test
    void onlyAuthorizedUserCanReadErrorsForSavedIssuer() throws Exception {
        var rp = ResourceGenerator.generateRelyingPartyResource();
        when(rpService.get(rp.id())).thenReturn(rp);
        mvc.perform(get("/details/{id}", rp.id()).with(SecurityTestUtils.oidcLoginForOrgno("123456789")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(credentialsService);
    }
}
