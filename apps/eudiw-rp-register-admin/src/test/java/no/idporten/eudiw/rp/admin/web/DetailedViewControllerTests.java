package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.DetailedViewController;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummaryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the RP detailed view controller")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class DetailedViewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelyingPartyCertificateSummaryBuilder relyingPartyCertificateSummaryBuilder;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRelyingPartiesService;

    @ParameterizedTest
    @ValueSource(strings = {"https://example.org", "//example.org", "javascript:alert(1)", "/search/other"})
    @DisplayName("Should return to search instead of an untrusted destination")
    void shouldReturnToSearchInsteadOfUntrustedDestination(String returnUrl) throws Exception {
        var relyingPartyResource = ResourceGenerator.generateRelyingPartyResource();
        when(mockRelyingPartiesService.get(relyingPartyResource.id())).thenReturn(relyingPartyResource);

        mockMvc.perform(get("/details/" + relyingPartyResource.id()).param("returnUrl", returnUrl))
            .andExpectAll(status().isOk(), content().string(containsString("href=\"/search\"")));
    }

    @Nested
    @DisplayName("when GET'ing the /details endpoint for a given RP ID ...")
    class DetailsEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the correct RP and certificates is loaded")
        public void testCorrectViewAndModelAttributes()
            throws Exception {
            RelyingPartyResource relyingPartyResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID relyingPartyInstanceId = relyingPartyResource.id();

            when(mockRelyingPartiesService.get(relyingPartyInstanceId)).thenReturn(relyingPartyResource);

            var expectedAccessCerts =
                relyingPartyResource.accessCertificates()
                          .stream()
                          .map(relyingPartyCertificateSummaryBuilder::build)
                          .toList();
            var expectedIssuerCerts =
                relyingPartyResource.issuerCertificates()
                          .stream()
                          .map(relyingPartyCertificateSummaryBuilder::build)
                          .toList();

            mockMvc.perform(get("/details/" + relyingPartyInstanceId))
                   .andExpect(status().isOk())
                   .andExpect(view().name("details_view"))
                   .andExpect(content().string(containsString("href=\"/search\"")))
                   .andExpect(content().string(containsString("Ikkje registrert.")))
                   .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                       "(?s).*Tenesta nyttar mellomledd</th>\\s*<td>\\s*<span>Ikkje registrert\\.</span>.*")))
                   .andExpect(model().attribute(
                       DetailedViewController.DETAILED_VIEW_DATA_ATTR, relyingPartyResource))
                   .andExpect(model().attribute(DetailedViewController.ACCESS_CERTIFICATE_SUMMARIES_ATTR, expectedAccessCerts))
                   .andExpect(model().attribute(DetailedViewController.ISSUER_CERTIFICATE_SUMMARIES_ATTR, expectedIssuerCerts));

            verify(mockRelyingPartiesService).get(eq(relyingPartyInstanceId));
        }

        @Test
        @DisplayName("Should display relying party, relying party service and intended-use fields")
        void shouldDisplayRelyingPartyServiceAndIntendedUseFields() throws Exception {
            var relyingPartyResource = ResourceGenerator.generateRelyingPartyResource()
                .withRelyingParty(new RelyingPartyResource.RelyingPartyDetails(
                    "Party trade name",
                    "NO",
                    "WalletRelyingParty",
                    new RelyingPartyResource.LegalPersonDetails(
                        List.of("Legal name"),
                        List.of(new RelyingPartyResource.LawDetails("law-1", "Act 1"))),
                    List.of(new RelyingPartyResource.IdentifierDetails("EUID", "NO-123")),
                    List.of(new RelyingPartyResource.PolicyDetails("PrivacyPolicy", "https://example.test/privacy")),
                    List.of("Street 1"),
                    List.of("party@example.test"),
                    List.of("+4712345678"),
                    List.of("https://example.test/info"),
                    List.of("certificate-chain"),
                    new RelyingPartyResource.SupervisoryAuthorityDetails(
                        "Datatilsynet", "NO", List.of(), List.of("postkasse@datatilsynet.no"), List.of()),
                    "https://registry.example.test/wrp"))
                .withRelyingPartyService(new RelyingPartyResource.RelyingPartyServiceDetails(
                    "service-1",
                    List.of(List.of(new RelyingPartyResource.MultiLangStringDetails("en", "Service description"))),
                    "https://example.test/support",
                    "service@example.test",
                    "+4798765432",
                    List.of("urn:example:sub"),
                    List.of(new RelyingPartyResource.ProvidedAttestationDetails("jwt", "attestation")),
                    true,
                    List.of(),
                    List.of("service-2"),
                    true))
                .withIntendedUses(List.of(
                    new RelyingPartyResource.IntendedUseDetails(
                        UUID.randomUUID(),
                        "pid-verification",
                        "2026-01-01T00:00:00Z",
                        null,
                        List.of(new RelyingPartyResource.MultiLangStringDetails("en", "Verify identity")),
                        List.of(new RelyingPartyResource.PolicyDetails(
                            "PrivacyPolicy", "https://example.test/intended-privacy")),
                        List.of(new RelyingPartyResource.CredentialDetails(
                            "dc+sd-jwt",
                            Map.of("vct_values", List.of("urn:example:pid")),
                            List.of(Map.of("path", List.of("given_name")))))),
                    new RelyingPartyResource.IntendedUseDetails(
                        UUID.randomUUID(), "revoked-use", "2025-01-01T00:00:00Z",
                        "2026-01-01T00:00:00Z", List.of(), List.of(), List.of())));
            when(mockRelyingPartiesService.get(relyingPartyResource.id())).thenReturn(relyingPartyResource);

            var response = mockMvc.perform(get("/details/" + relyingPartyResource.id()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            assertAll(
                () -> assertTrue(response.contains("Party trade name")),
                () -> assertTrue(response.contains("EUID: NO-123")),
                () -> assertTrue(response.contains("Datatilsynet")),
                () -> assertTrue(response.contains("https://registry.example.test/wrp")),
                () -> assertTrue(response.contains("en: Service description")),
                () -> assertTrue(response.contains("pid-verification")),
                () -> assertTrue(response.contains("Verify identity")),
                () -> assertTrue(response.contains("<h2 class=\"ds-heading\">Føremål</h2>")
                    && response.indexOf("<h2 class=\"ds-heading\">Føremål</h2>")
                    < response.indexOf("<h2 class=\"ds-heading\">Roller og bevistypar</h2>")),
                () -> assertTrue(response.contains("dc+sd-jwt")),
                () -> assertTrue(response.contains("Aktiv")),
                () -> assertTrue(response.contains("Revokert")));
        }
    }
}
