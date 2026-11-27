package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.form.IntendedUseForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.AuthorizedPartyAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("junit")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@DisplayName("When creating an IntendedUse")
class IntendedUseControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelyingPartiesService relyingPartiesService;

    private RelyingPartyResource registration;

    @BeforeEach
    void setUpRegistration() {
        registration = ResourceGenerator.generateRelyingPartyResource();
        when(relyingPartiesService.get(registration.id())).thenReturn(registration);
    }

    @Test
    @DisplayName("Should return one empty manual proof when the user has access to the registration")
    void shouldReturnOneEmptyManualProofWhenTheUserHasAccessToTheRegistration() throws Exception {
        var result = mockMvc.perform(get("/service/intended-use/create/" + registration.id()))
            .andExpectAll(status().isOk(), view().name("intended_use_form_view"),
                model().attribute("registration", registration),
                model().attributeHasNoErrors("intendedUseForm"))
            .andReturn();

        var form = submittedForm(result);
        assertEquals(1, form.getCredentials().size());
        assertCredentialValues(form.getCredentials().getFirst(), null, null, null);
        verify(relyingPartiesService, never()).createIntendedUse(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Should deny access when the user lacks access to the registration organization")
    void shouldDenyAccessWhenTheUserLacksAccessToTheRegistrationOrganization(boolean hasOtherOrganization) throws Exception {
        var unauthorizedUser = user("employee").roles("USER");
        if (hasOtherOrganization) {
            String otherOrgno = "991825827".equals(registration.orgno()) ? "123456785" : "991825827";
            unauthorizedUser.authorities(new SimpleGrantedAuthority("ROLE_USER"),
                new AuthorizedPartyAuthority(otherOrgno, "Other organization", false));
        }

        mockMvc.perform(get("/service/intended-use/create/" + registration.id()).with(unauthorizedUser))
            .andExpect(status().isNotFound());
        mockMvc.perform(manualSubmission(registration.id()).with(unauthorizedUser))
            .andExpect(status().isNotFound());

        verify(relyingPartiesService, never()).createIntendedUse(any(), any());
    }

    @Test
    @DisplayName("Should save all manual proofs using only permitted fields")
    void shouldSaveAllManualProofsUsingOnlyPermittedFields() throws Exception {
        mockMvc.perform(manualSubmission(registration.id())
                .param("returnUrl", "/search?orgno=991825827")
                .with(submission -> {
                    submission.setParameter("credentials[0].name", " Norsk ID-bevis ");
                    submission.setParameter("credentials[0].vct", " urn:example:norsk-id ");
                    submission.setParameter("credentials[0].claimPaths", "[\"given_name\"]\r\n[\"family_name\"]");
                    return submission;
                })
                .param("credentials[0].format", "forged")
                .param("credentials[0].definitionId", "urn:test:ignored")
                .param("credentials[1].name", "Aldersbevis")
                .param("credentials[1].vct", "urn:example:age")
                .param("credentials[1].claimPaths", "[\"age_over_18\"]"))
            .andExpectAll(status().is3xxRedirection(),
                redirectedUrl("/details/" + registration.id() + "?returnUrl=%2Fsearch%3Forgno%3D991825827"));

        var requestCaptor = ArgumentCaptor.forClass(V2RegisterClient.CreateIntendedUseRequest.class);
        verify(relyingPartiesService).createIntendedUse(eq(registration.relyingPartyServiceId()), requestCaptor.capture());
        var request = requestCaptor.getValue();
        assertEquals(List.of(new V2RegisterClient.IntendedUsePurpose("nn", "Identifisere brukaren")), request.purpose());
        assertEquals(List.of(new V2RegisterClient.IntendedUsePolicy(
            "http://data.europa.eu/eudi/policy/privacy-statement", "https://example.test/privacy")), request.privacyPolicy());
        assertEquals(2, request.credentials().size());
        assertEquals("Norsk ID-bevis", request.credentials().getFirst().displayName());
        assertEquals(Map.of("vct_values", List.of("urn:example:norsk-id")), request.credentials().getFirst().meta());
        assertEquals(List.of(Map.of("path", List.of("given_name")), Map.of("path", List.of("family_name"))),
            request.credentials().getFirst().claims());
        assertEquals("Aldersbevis", request.credentials().get(1).displayName());
        assertEquals(Map.of("vct_values", List.of("urn:example:age")), request.credentials().get(1).meta());
        assertEquals(List.of(Map.of("path", List.of("age_over_18"))), request.credentials().get(1).claims());
        assertTrue(request.credentials().stream().allMatch(credential -> "dc+sd-jwt".equals(credential.format())));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not json", "[]", "{}", "null", "[true]", "[{}]", "[[]]", "[-1]", "[1.0]", "[1e0]", "[\" \"]",
        "[\"name\"] []", "[\"name\"]\n\n[\"age\"]"
    })
    @DisplayName("Should keep all manual proofs without saving when a later DCQL path is invalid")
    void shouldKeepAllManualProofsWithoutSavingWhenALaterDcqlPathIsInvalid(String invalidPath) throws Exception {
        var result = mockMvc.perform(manualSubmission(registration.id())
                .param("credentials[1].name", "Aldersbevis")
                .param("credentials[1].vct", "urn:example:age")
                .param("credentials[1].claimPaths", invalidPath)
                .param("credentials[1].format", "forged")
                .param("credentials[1].definitionId", "urn:test:ignored"))
            .andExpectAll(status().isOk(), view().name("intended_use_form_view"),
                model().attributeHasFieldErrors("intendedUseForm", "credentials[1].claimPaths"))
            .andReturn();

        var form = submittedForm(result);
        assertEquals(2, form.getCredentials().size());
        assertCredentialValues(form.getCredentials().getFirst(), "Norsk ID-bevis", "urn:example:norsk-id", "[\"given_name\"]");
        assertCredentialValues(form.getCredentials().get(1), "Aldersbevis", "urn:example:age", invalidPath);
        assertPurposeAndPrivacyPolicy(form);
        var errors = bindingResult(result);
        assertFalse(errors.getFieldError("credentials[1].claimPaths").getDefaultMessage().isBlank());
        assertTrue(List.of(errors.getSuppressedFields()).containsAll(
            List.of("credentials[1].format", "credentials[1].definitionId")));
        verify(relyingPartiesService, never()).createIntendedUse(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"notFound,inactive", "badRequest,rejected"})
    @DisplayName("Should keep manual input with an error when the register rejects saving")
    void shouldKeepManualInputWithAnErrorWhenTheRegisterRejectsSaving(String rejection, String errorCode) throws Exception {
        RuntimeException serviceException = "notFound".equals(rejection)
            ? new NotFoundException("not found") : new BadRequestException("rejected");
        doThrow(serviceException)
            .when(relyingPartiesService).createIntendedUse(eq(registration.relyingPartyServiceId()), any());

        var result = mockMvc.perform(manualSubmission(registration.id()))
            .andExpectAll(status().isOk(), view().name("intended_use_form_view"))
            .andReturn();

        var error = bindingResult(result).getGlobalError();
        assertNotNull(error);
        assertEquals(errorCode, error.getCode());
        assertFalse(error.getDefaultMessage().isBlank());
        var form = submittedForm(result);
        assertCredentialValues(form.getCredentials().getFirst(), "Norsk ID-bevis", "urn:example:norsk-id", "[\"given_name\"]");
        assertPurposeAndPrivacyPolicy(form);
        verify(relyingPartiesService).createIntendedUse(eq(registration.relyingPartyServiceId()), any());
    }

    @ParameterizedTest
    @CsvSource({
        "credentials[0].name,credentials[0].name", "credentials[0].vct,credentials[0].vct",
        "credentials[0].claimPaths,credentials[0].claimPaths", "purpose[0].lang,purpose",
        "purpose[0].content,purpose", "privacyPolicyUri,privacyPolicyUri"
    })
    @DisplayName("Should identify a missing value without saving")
    void shouldIdentifyAMissingValueWithoutSaving(String field, String errorField) throws Exception {
        mockMvc.perform(manualSubmission(registration.id()).with(submission -> {
                submission.setParameter(field, " ");
                return submission;
            }))
            .andExpectAll(status().isOk(), view().name("intended_use_form_view"),
                model().attributeHasFieldErrors("intendedUseForm", errorField));

        verify(relyingPartiesService, never()).createIntendedUse(any(), any());
    }

    @Test
    @DisplayName("Should reject a request without a manual proof")
    void shouldRejectARequestWithoutAManualProof() throws Exception {
        mockMvc.perform(post("/service/intended-use/create/" + registration.id()).with(csrf())
                .param("purpose[0].lang", "nn")
                .param("purpose[0].content", "Identifisere brukaren")
                .param("privacyPolicyUri", "https://example.test/privacy"))
            .andExpectAll(status().isOk(), view().name("intended_use_form_view"),
                model().attributeHasFieldErrors("intendedUseForm", "credentials"));

        verify(relyingPartiesService, never()).createIntendedUse(any(), any());
    }

    private static MockHttpServletRequestBuilder manualSubmission(UUID id) {
        return post("/service/intended-use/create/" + id).with(csrf())
            .param("purpose[0].lang", "nn")
            .param("purpose[0].content", "Identifisere brukaren")
            .param("privacyPolicyUri", "https://example.test/privacy")
            .param("credentials[0].name", "Norsk ID-bevis")
            .param("credentials[0].vct", "urn:example:norsk-id")
            .param("credentials[0].claimPaths", "[\"given_name\"]");
    }

    private static void assertCredentialValues(IntendedUseForm.Credential credential, String name, String vct, String paths) {
        assertEquals(name, credential.getName());
        assertEquals(vct, credential.getVct());
        assertEquals(paths, credential.getClaimPaths());
    }

    private static void assertPurposeAndPrivacyPolicy(IntendedUseForm form) {
        assertEquals(1, form.getPurpose().size());
        assertEquals("nn", form.getPurpose().getFirst().getLang());
        assertEquals("Identifisere brukaren", form.getPurpose().getFirst().getContent());
        assertEquals("https://example.test/privacy", form.getPrivacyPolicyUri());
    }

    private static BindingResult bindingResult(MvcResult result) {
        return (BindingResult) result.getModelAndView().getModel()
            .get(BindingResult.MODEL_KEY_PREFIX + "intendedUseForm");
    }

    private static IntendedUseForm submittedForm(MvcResult result) {
        return (IntendedUseForm) result.getModelAndView().getModel().get("intendedUseForm");
    }
}
