package no.idporten.eudiw.rp.admin.web.selfservice;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.V2RegistrationsService;
import no.idporten.eudiw.rp.admin.web.resource.RegistrationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@SpringBootTest
@ActiveProfiles("junit")
@AutoConfigureMockMvc
class RegistrationsControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private V2RegistrationsService registrationsService;

    @Test
    @DisplayName("Show registrations for the signed-in organization with links to their details")
    void shouldShowRegistrationsForAuthorizedOrganization() throws Exception {
        String authorizedOrgno = "123456785";
        RegistrationSummary registration = new RegistrationSummary(new UUID(0, 42), "Test service",
            List.of(new RegistrationSummary.Role("Test role"), new RegistrationSummary.Role(null)),
            1700000000000L, 1710000000000L);
        RegistrationSummary missingFields = new RegistrationSummary(new UUID(0, 43), null, null, null, null);
        when(registrationsService.searchByOrgno(authorizedOrgno, "")).thenReturn(List.of(registration, missingFields));

        String html = mockMvc.perform(get("/registrations").param("orgno", "987654321")
                .with(SecurityTestUtils.oidcLoginForOrgno(authorizedOrgno)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/details/" + registration.id())))
            .andExpect(content().string(containsString("Test service")))
            .andExpect(content().string(containsString("Test role")))
            .andExpect(content().string(containsString("/details/" + missingFields.id())))
            .andExpect(content().string(containsString("aria-label=\"Vis registrering " + missingFields.id() + "\"")))
            .andReturn().getResponse().getContentAsString();

        assertEquals(0, StringUtils.countOccurrencesOf(html, "Ikkje oppgitt"));
        verify(registrationsService).searchByOrgno(authorizedOrgno, "");
    }

    @Test
    @DisplayName("Should search within the signed-in organization regardless of the organization in the URL")
    void shouldSearchWithinSignedInOrganizationRegardlessOfOrganizationInUrl() throws Exception {
        when(registrationsService.searchByOrgno("123456785", "Test")).thenReturn(List.of());

        mockMvc.perform(get("/registrations").param("searchTerm", "  Test  ").param("orgno", "987654321")
                .with(SecurityTestUtils.oidcLoginForOrgno("123456785")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"Test\"")))
            .andExpect(content().string(containsString("Fann ingen registrerte brukarstader.")));
        verify(registrationsService).searchByOrgno("123456785", "Test");
        verifyNoMoreInteractions(registrationsService);
    }

    @Test
    @DisplayName("Should reject an invalid registration search without calling the register service")
    void shouldRejectInvalidRegistrationSearchWithoutCallingRegisterService() throws Exception {
        mockMvc.perform(get("/registrations").param("searchTerm", "invalid$")
                .with(SecurityTestUtils.oidcLoginForOrgno("123456785")))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("searchFormAttr", "searchTerm"))
            .andExpect(content().string(containsString("aria-describedby=\"search_term_errors\"")));
        verifyNoInteractions(registrationsService);
    }
}
