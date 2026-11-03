package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.integrations.registerservice.CertificateType;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.MockWebServerConfiguration;
import no.idporten.eudiw.rp.admin.service.accesscertificates.PKCS10CertificationRequestConverter;
import no.idporten.eudiw.rp.admin.testdata.CertificatesGenerator;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.PagedResponse;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.utils.WebTestUtils;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@AutoConfigureMockMvc
@Import(MockWebServerConfiguration.class)
class RegistrationWorkflowTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MockWebServer server;

    private final JsonMapper jsonMapper = new JsonMapper();
    private final UUID relyingPartyServiceId = new UUID(0, 2);
    private final UUID relyingPartyId = new UUID(0, 3);
    private final List<String> expectedRequests = new ArrayList<>();
    private RelyingPartyResource relyingPartyResource;
    private String expectedUpdatedServiceName;
    private int initialRequestCount;
    private String expectedRelyingPartyAlias;
    private String expectedRolesRequest;

    @BeforeEach
    void setUp() {
        var certificate = ResourceGenerator.generateCertificateResource();
        var accessCertificate = new RelyingPartyCertificateResource(certificate.certificate(), new UUID(0, 4), null, 0);
        var issuerCertificate = new RelyingPartyCertificateResource(
            certificate.certificate(),
            new UUID(0, 5),
            "https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
            0);
        relyingPartyResource = new RelyingPartyResource(
            new UUID(0, 1),
            "123456785",
            "Service name",
            "Legal name",
            true,
            List.of(new RelyingPartyEntitlementResource(issuerCertificate.entitlement(), "Issuer", "https://issuer.test")),
            List.of(),
            List.of(accessCertificate),
            List.of(issuerCertificate),
            1700000000000L,
            1710000000000L,
            true,
            new RelyingPartyResource.RelyingPartyDetails("Original alias", "NO", "WalletRelyingParty",
                new RelyingPartyResource.LegalPersonDetails(List.of("Legal name"), List.of()),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null, null),
            null,
            List.of(),
            relyingPartyId,
            relyingPartyServiceId);
        initialRequestCount = server.getRequestCount();
    }

    @AfterEach
    void verifyBackendRequests() throws Exception {
        for (String expected : expectedRequests) {
            var request = server.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request, "Missing backend request: " + expected);
            assertEquals(expected, request.getMethod() + " " + request.getPath());
            if (expectedUpdatedServiceName != null && "PUT".equals(request.getMethod())) {
                assertEquals(
                    expectedUpdatedServiceName,
                    jsonMapper.readTree(request.getBody().readUtf8())
                        .get("name")
                        .asString());
            }
            if (expectedRolesRequest != null && "PUT".equals(request.getMethod())) {
                assertEquals(jsonMapper.readTree(expectedRolesRequest),
                    jsonMapper.readTree(request.getBody().readUtf8()));
            }
            if (expectedRelyingPartyAlias != null && "PATCH".equals(request.getMethod())) {
                assertEquals(jsonMapper.readTree(expectedRelyingPartyAlias),
                    jsonMapper.readTree(request.getBody().readUtf8()));
            }
        }
        assertEquals(initialRequestCount + expectedRequests.size(), server.getRequestCount(),
            "Unexpected backend request, e.g. V1 lookup, write or download after access denial");
    }

    @Test
    @DisplayName("Should preserve combined search filters and sorted pages when returning from registration details")
    void shouldPreserveCombinedSearchFiltersAndSortedPagesWhenReturningFromDetails() throws Exception {
        UUID secondInstanceId = relyingPartyResource.id();
        URI originalSearch = URI.create("/search?searchTerm=Test&includeInactive=true"
            + "&requiredEntitlements=urn%3Afirst&requiredEntitlements=urn%3Asecond&page=2&sort=name");
        enqueueSearchPage(1, "name", secondInstanceId, "Second service");
        String secondPageHtml = mockMvc.perform(get(originalSearch).with(login(true)))
            .andExpectAll(status().isOk(), view().name("search_view"),
                content().string(allOf(
                    containsString("<td>123456785</td>"), containsString("<td>Legal name</td>"),
                    containsString("href=\"/details/" + secondInstanceId + "?returnUrl="),
                    containsString("<div>Second service</div>"),
                    matchesPattern("(?s).*<td>1</td>\\s*<td>1</td>.*"))))
            .andReturn().getResponse().getContentAsString();

        URI detailsLink = navigationLink(secondPageHtml, "/details/" + secondInstanceId, "returnUrl=");
        enqueueLookup();
        String detailsHtml = mockMvc.perform(get(detailsLink).with(login(true)))
            .andExpectAll(status().isOk(), view().name("details_view"))
            .andReturn().getResponse().getContentAsString();

        URI returnLink = navigationLink(detailsHtml, "/search", "page=2");
        assertEquals(originalSearch, returnLink);

        enqueueLookup();
        expectedRequests.add("PATCH /v2/instances/" + secondInstanceId + "/deactivate");
        server.enqueue(new MockResponse().setResponseCode(204));
        String redirect = mockMvc.perform(post("/admin/status/" + secondInstanceId).with(login(true)).with(csrf())
                .param("active", "false").param("returnUrl", originalSearch.toString()))
            .andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        relyingPartyResource = relyingPartyResource.withActive(false);
        enqueueLookup();
        detailsHtml = mockMvc.perform(get(URI.create(redirect)).with(login(true)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertEquals(originalSearch, navigationLink(detailsHtml, "/search", "page=2"));
        enqueueSearchPage(1, "name", secondInstanceId, "Second service");
        mockMvc.perform(get(returnLink).with(login(true)))
            .andExpectAll(status().isOk(), content().string(containsString("<div>Second service</div>")));
    }

    @Test
    @DisplayName("Should preserve combined search filters when changing pages and sorting")
    void shouldPreserveCombinedSearchFiltersWhenChangingPagesAndSorting() throws Exception {
        enqueueSearchPage(1, "name", relyingPartyResource.id(), "Second service");
        String secondPageHtml = mockMvc.perform(get("/search").with(login(true))
                .param("searchTerm", "Test").param("includeInactive", "true")
                .param("requiredEntitlements", "urn:first", "urn:second")
                .param("page", "2").param("sort", "name"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        enqueueSearchPage(0, "name", relyingPartyResource.id(), "First service");
        String firstPageHtml = mockMvc.perform(get(navigationLink(secondPageHtml, "/search", "page=1")).with(login(true)))
            .andExpectAll(status().isOk(), content().string(containsString("<div>First service</div>")))
            .andReturn().getResponse().getContentAsString();

        enqueueSearchPage(0, "createdMs", relyingPartyResource.id(), "First service");
        mockMvc.perform(get(navigationLink(firstPageHtml, "/search", "sort=createdMs")).with(login(true)))
            .andExpectAll(status().isOk(), content().string(containsString("<div>First service</div>")));
    }

    @Test
    @DisplayName("Should search only authorized registrations and keep links to matching services")
    void shouldSearchOnlyAuthorizedRegistrationsAndKeepLinksToMatchingServices() throws Exception {
        var matching = new V2RegisterClient.Registration(relyingPartyResource.id(), "Kontrollteneste",
            List.of(new V2RegisterClient.Role("Service Provider")), 1700000000000L, 1710000000000L);
        var other = new V2RegisterClient.Registration(new UUID(0, 9), "Annan teneste", List.of(), null, null);
        enqueue("GET", "/v2/registrations?orgno=" + relyingPartyResource.orgno(), List.of(matching, other));

        mockMvc.perform(get("/registrations").with(login(false))
                .param("searchTerm", "  KONTROLL  ").param("orgno", "987654321"))
            .andExpectAll(status().isOk(), content().string(allOf(
                containsString("href=\"/details/" + matching.id() + "\""),
                containsString("Kontrollteneste"), not(containsString("Annan teneste")))));
    }

    @Test
    @DisplayName("Should display register service failures instead of empty registration search results")
    void shouldDisplayRegisterServiceFailuresInsteadOfEmptyRegistrationSearchResults() throws Exception {
        expectedRequests.add("GET /v2/registrations?orgno=" + relyingPartyResource.orgno());
        server.enqueue(new MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"service_unavailable\",\"error_description\":\"Register service unavailable\"}"));

        mockMvc.perform(get("/registrations").with(login(false)).param("searchTerm", "Test"))
            .andExpectAll(status().isInternalServerError(), view().name("error/5xx"));
    }

    @ParameterizedTest
    @CsvSource({
        "search, 500, '{\"error\":\"service_unavailable\",\"error_description\":\"Register service unavailable\"}'",
        "catalog, 500, '{\"error\":\"service_unavailable\",\"error_description\":\"Register service unavailable\"}'",
        "search, 200, not json",
        "catalog, 200, not json"})
    @DisplayName("Should show V2 search and catalog failures without falling back to V1 or displaying empty results")
    void shouldShowV2SearchAndCatalogFailuresWithoutFallback(String failedOperation, int status, String body) throws Exception {
        String failedPath = "/v2/instances?searchTerm=&includeInactive=false&page=0&pageSize=25&sort=createdMs";
        if (failedOperation.equals("catalog")) {
            enqueue("GET", failedPath, new PagedResponse<>(List.of(), new PagedResponse.PageMetadata(25, 0, 0, 0)));
            failedPath = "/v2/entitlements?includeInactive=false";
        }
        expectedRequests.add("GET " + failedPath);
        server.enqueue(new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
            .setBody(body));

        mockMvc.perform(get("/search").with(login(true)))
            .andExpectAll(status().isInternalServerError(), view().name("error/5xx"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "edit"})
    @DisplayName("Should offer active V2 roles in administrator create and edit forms")
    void shouldOfferActiveV2RolesInAdministratorCreateAndEditForms(String form) throws Exception {
        if (form.equals("edit")) {
            enqueueLookup();
        }
        enqueue("GET", "/v2/entitlements?includeInactive=false", new V2RegisterClient.Entitlements(List.of(
            new V2RegisterClient.Entitlement(new UUID(0, 7), "urn:additional", true, "Additional role"))));

        mockMvc.perform(get(form.equals("create") ? "/create" : "/edit/" + relyingPartyResource.id()).with(login(true)))
            .andExpectAll(status().isOk(), view().name(form + "_form_view"),
                content().string(containsString("Additional role")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Should submit a service name change and return to registration details for the owning organization and an administrator")
    void shouldSubmitServiceNameChangeAndReturnToRegistrationDetails(boolean administrator) throws Exception {
        enqueueLookup();
        var editRelyingPartyForm = AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(relyingPartyResource);
        expectedUpdatedServiceName = "Updated service name";
        editRelyingPartyForm.setTradeName(expectedUpdatedServiceName);
        enqueue("PUT", "/v1/rp/" + relyingPartyResource.id(), relyingPartyResource);

        var request = post((administrator ? "/admin/edit/" : "/edit/") + relyingPartyResource.id())
            .with(csrf())
            .with(login(administrator));

        mockMvc.perform(WebTestUtils.withEditForm(request, editRelyingPartyForm))
            .andExpectAll(status().is3xxRedirection(), redirectedUrl("/details/" + relyingPartyResource.id()));
    }

    @ParameterizedTest
    @EnumSource(CertificateType.class)
    @DisplayName("Should submit an access or issuer certificate order for the owning organization")
    void shouldSubmitCertificateOrderForOwningOrganization(CertificateType certificateType) throws Exception {
        enqueueLookup();
        var certificationRequest = CertificatesGenerator.generatePKCS10Csr();
        var certificate = activeCertificate(certificateType);
        String path = "/v2/instances/" + relyingPartyResource.id() + "/certificates/" + certificateType.pathSegment();
        enqueue("POST", path, certificate);

        mockMvc.perform(post("/csr/" + certificateType.pathSegment() + "/" + relyingPartyResource.id())
                .with(csrf())
                .with(login(false))
                .formField("csrField", PKCS10CertificationRequestConverter.toString(certificationRequest))
                .formField("entitlement", relyingPartyResource.issuerCertificates().getFirst().entitlement()))
            .andExpectAll(status().isOk(), view().name(certificateType == CertificateType.ISSUER
                ? "csr_issuer_submit_success_view" : "csr_submit_success_view"));
    }

    @ParameterizedTest
    @EnumSource(CertificateType.class)
    @DisplayName("Should download an existing access or issuer certificate for the owning organization")
    void shouldDownloadExistingCertificatesForOwningOrganization(CertificateType certificateType) throws Exception {
        enqueueLookup();
        var certificate = certificate(certificateType);
        enqueue(
            "GET",
            "/v2/instances/" + relyingPartyResource.id() + "/certificates/" + certificateType.pathSegment() + "/" + certificate.id(),
            certificate);
        String downloadPath = "/get-certificate/" + relyingPartyResource.id() + (certificateType == CertificateType.ISSUER ? "/issuer" : "")
            + "/" + certificate.id();

        mockMvc.perform(get(downloadPath).with(login(false)))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(CertificateType.class)
    @DisplayName("Should submit an access or issuer certificate revocation for the owning organization")
    void shouldSubmitCertificateRevocationForOwningOrganization(CertificateType certificateType) throws Exception {
        enqueueLookup();
        var certificate = activeCertificate(certificateType);
        String certificatePath = "/v2/instances/" + relyingPartyResource.id() + "/certificates/"
            + certificateType.pathSegment() + "/" + certificate.id();
        expectedRequests.add("PATCH " + certificatePath + "/revoke");
        server.enqueue(new MockResponse().setResponseCode(204));

        mockMvc.perform(post("/revoke-certificate/" + relyingPartyResource.id() + "/" + certificateType.pathSegment() + "/" + certificate.id())
                .with(csrf())
                .with(login(false)))
            .andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET /details/{id}", "GET /edit/{id}", "GET /csr/access/{id}",
        "GET /csr/issuer/{id}", "GET /get-certificate/{id}/{cert}",
        "GET /get-certificate/{id}/issuer/{cert}", "POST /edit/{id}", "POST /csr/access/{id}",
        "POST /csr/issuer/{id}", "POST /revoke-certificate/{id}/access/{cert}",
        "POST /revoke-certificate/{id}/issuer/{cert}"})
    @DisplayName("Should prevent another organization from viewing or editing the registration, ordering or downloading certificates, or submitting revocations")
    void shouldPreventOtherOrganizationsFromAccessingOrChangingRegistration(String operation) throws Exception {
        enqueueLookup();
        String path = operation.substring(operation.indexOf(' ') + 1)
            .replace("{id}", relyingPartyResource.id().toString())
            .replace("{cert}", certificate(CertificateType.ACCESS).id().toString());
        var request = operation.startsWith("GET") ? get(path) : post(path).with(csrf());

        mockMvc.perform(request.with(SecurityTestUtils.oidcLoginForOrgno("987654325")))
            .andExpectAll(status().isNotFound(), view().name("error/404"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Should change registration status from the status section and show the new status badge")
    void shouldChangeRegistrationStatusFromStatusSectionAndShowNewStatusBadge(boolean active) throws Exception {
        relyingPartyResource = relyingPartyResource.withActive(!active);
        enqueueLookup();
        mockMvc.perform(get("/details/" + relyingPartyResource.id()).with(login(true)))
            .andExpectAll(status().isOk(), content().string(allOf(
                containsString(active ? ">Inaktiv</span>" : ">Aktiv</span>"),
                containsString("Registreringsstatus</h2>"),
                containsString("name=\"active\" value=\"" + active + "\""),
                containsString(active ? ">Aktiver registrering</button>" : ">Deaktiver registrering</button>"))));

        enqueueLookup();
        expectedRequests.add("PATCH /v2/instances/" + relyingPartyResource.id() + "/" + (active ? "activate" : "deactivate"));
        server.enqueue(new MockResponse().setResponseCode(204));
        mockMvc.perform(post("/admin/status/" + relyingPartyResource.id()).with(login(true)).with(csrf())
                .param("active", Boolean.toString(active)))
            .andExpectAll(status().is3xxRedirection(), redirectedUrl("/details/" + relyingPartyResource.id() + "?returnUrl=%2Fsearch"));

        relyingPartyResource = relyingPartyResource.withActive(active);
        enqueueLookup();
        mockMvc.perform(get("/details/" + relyingPartyResource.id()).with(login(true)))
            .andExpectAll(status().isOk(), content().string(allOf(
                containsString(active ? ">Aktiv</span>" : ">Inaktiv</span>"),
                containsString(active ? ">Deaktiver registrering</button>" : ">Aktiver registrering</button>"))));
    }

    @Test
    @DisplayName("Should not offer activation when the service is inactive")
    void shouldNotOfferActivationWhenServiceIsInactive() throws Exception {
        relyingPartyResource = relyingPartyResource.withActive(false).withRelyingPartyService(
            new RelyingPartyResource.RelyingPartyServiceDetails(null, List.of(), null, null, null, List.of(), List.of(),
                false, List.of(), List.of(), false));
        enqueueLookup();
        mockMvc.perform(get("/details/" + relyingPartyResource.id()).with(login(true)))
            .andExpectAll(status().isOk(), content().string(allOf(
                containsString("Aktiver tenesta før du aktiverer registreringa."),
                containsString("disabled=\"disabled\""))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Should keep the current registration status without an unnecessary status update")
    void shouldKeepCurrentRegistrationStatusWithoutUnnecessaryUpdate(boolean active) throws Exception {
        relyingPartyResource = relyingPartyResource.withActive(active);
        enqueueLookup();
        mockMvc.perform(post("/admin/status/" + relyingPartyResource.id()).with(login(true)).with(csrf())
                .param("active", Boolean.toString(active)))
            .andExpectAll(status().is3xxRedirection(), redirectedUrl("/details/" + relyingPartyResource.id() + "?returnUrl=%2Fsearch"));
    }

    @Test
    @DisplayName("Should not change status when no status is selected")
    void shouldNotChangeStatusWhenNoStatusIsSelected() throws Exception {
        enqueueLookup();
        mockMvc.perform(post("/admin/status/" + relyingPartyResource.id()).with(login(true)).with(csrf())
                .param("returnUrl", "https://example.com/search"))
            .andExpectAll(status().is3xxRedirection(), redirectedUrl("/details/" + relyingPartyResource.id() + "?returnUrl=%2Fsearch"));
    }

    @Test
    @DisplayName("Should show backend errors rather than confirm a failed status change")
    void shouldShowBackendErrorsRatherThanConfirmFailedStatusChange() throws Exception {
        enqueueLookup();
        expectedRequests.add("PATCH /v2/instances/" + relyingPartyResource.id() + "/deactivate");
        server.enqueue(new MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"service_unavailable\",\"error_description\":\"Register service unavailable\"}"));

        mockMvc.perform(post("/admin/status/" + relyingPartyResource.id()).with(login(true)).with(csrf())
                .param("active", "false"))
            .andExpectAll(status().isInternalServerError(), view().name("error/5xx"));
    }

    private URI navigationLink(String html, String path, String parameter) {
        var links = Pattern.compile("href=\"(" + Pattern.quote(path) + "\\?[^\"]*)\"").matcher(html);
        while (links.find()) {
            String link = HtmlUtils.htmlUnescape(links.group(1));
            if (link.contains(parameter)) {
                return URI.create(link);
            }
        }
        throw new AssertionError("Missing " + path + " link with " + parameter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Should save trade name and country and keep lists with several values unchanged")
    void shouldSaveTradeNameAndCountryAndKeepListsWithSeveralValuesUnchanged(boolean administrator) throws Exception {
        relyingPartyResource = relyingPartyResource.withActive(false);
        enqueueLookup();
        String html = mockMvc.perform(get("/details/" + relyingPartyResource.id()).with(login(administrator)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        URI editLink = navigationLink(html, "/relying-party/edit/" + relyingPartyResource.id(), "returnUrl=");
        enqueueLookup();
        enqueueRelyingParty("Original alias");
        mockMvc.perform(get(editLink).with(login(administrator)))
            .andExpectAll(status().isOk(), view().name("relying_party_information_form_view"),
                content().string(allOf(containsString("Original alias"), containsString("Legal name"),
                    containsString("alle tenestene"), containsString("Postboks 1"), containsString("0100 Oslo"),
                    not(containsString("name=\"postalAddress\"")), containsString("name=\"email\""),
                    containsString("post@example.test"), not(containsString("name=\"legalName\"")),
                    not(containsString("name=\"isPSB\"")))));
        enqueueLookup();
        enqueueRelyingParty("Original alias");
        expectedRelyingPartyAlias = """
            {"tradeName":"Digdir","country":"SE","postalAddress":null,"email":[],
             "phone":["+4711111111"],"infoURI":["https://digdir.no"]}
            """;
        enqueue("PATCH", "/v2/relying-parties/" + relyingPartyId, "{}");
        String redirect = mockMvc.perform(post(editLink).with(login(administrator)).with(csrf())
                .param("tradeName", " Digdir ").param("country", "SE")
                .param("postalAddress", " Postboks 2 ").param("email", "")
                .param("phone", "+4711111111").param("informationUri", "https://digdir.no")
                .param("isPSB", "true").param("legalName", "Changed").param("orgno", "987654321"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        relyingPartyResource = relyingPartyResource.withRelyingParty(
            new RelyingPartyResource.RelyingPartyDetails("Digdir", "SE", "WalletRelyingParty",
                new RelyingPartyResource.LegalPersonDetails(List.of("Legal name"), List.of()),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null, null));
        enqueueLookup();
        mockMvc.perform(get(URI.create(redirect)).with(login(administrator)))
            .andExpectAll(status().isOk(), content().string(allOf(
                containsString("Digdir"), containsString("Legal name"), containsString("Service name"))));
    }

    @Test
    @DisplayName("Should keep entered values and show an error when a required field is missing")
    void shouldKeepEnteredValuesAndShowErrorWhenCountryIsMissing() throws Exception {
        enqueueLookup();
        enqueueRelyingParty("Original alias");
        mockMvc.perform(post("/relying-party/edit/" + relyingPartyResource.id()).with(login(false)).with(csrf())
                .param("tradeName", "New alias").param("country", " "))
            .andExpectAll(status().isOk(), view().name("relying_party_information_form_view"),
                content().string(allOf(containsString("New alias"), containsString("Skriv inn land."),
                    containsString("aria-invalid=\"true\""), containsString("aria-describedby=\"country-error\""),
                    containsString("href=\"#country\""), containsString("autofocus"))));
    }

    @Test
    @DisplayName("Should show a field error when the country code is invalid")
    void shouldShowFieldErrorWhenCountryCodeIsInvalid() throws Exception {
        enqueueLookup();
        enqueueRelyingParty("Original alias");
        mockMvc.perform(post("/relying-party/edit/" + relyingPartyResource.id()).with(login(false)).with(csrf())
                .param("tradeName", "New alias").param("country", "Noreg"))
            .andExpectAll(status().isOk(), view().name("relying_party_information_form_view"),
                content().string(allOf(containsString("New alias"),
                    containsString("Skriv inn ein landskode med to store bokstavar."),
                    containsString("aria-invalid=\"true\""), containsString("aria-describedby=\"country-error\""))));
    }


    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    @DisplayName("Should deny editing a relying party that belongs to another organization")
    void shouldDenyEditingRelyingPartyForAnotherOrganization(String method) throws Exception {
        enqueueLookup();
        var request = method.equals("GET") ? get("/relying-party/edit/" + relyingPartyResource.id())
            : post("/relying-party/edit/" + relyingPartyResource.id()).with(csrf()).param("country", "NO");
        mockMvc.perform(request.with(SecurityTestUtils.oidcLoginForOrgno("987654321")))
            .andExpectAll(status().isNotFound(), view().name("error/404"));
    }


    private static final String QEAA = "https://uri.etsi.org/19475/Entitlement/QEAA_Provider";

    @Test
    @DisplayName("Should show locked roles and catalog roles to an administrator who follows the edit link")
    void shouldShowLockedRolesAndCatalogRolesToAdministrator() throws Exception {
        enqueueLookup();
        String html = mockMvc.perform(get("/details/" + relyingPartyResource.id()).with(login(true)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        URI editLink = navigationLink(html, "/roles/edit/" + relyingPartyResource.id(), "returnUrl=");

        enqueueLookup();
        enqueueRoleCatalog();
        mockMvc.perform(get(editLink).with(login(true)))
            .andExpectAll(status().isOk(), view().name("roles_form_view"),
                content().string(allOf(containsString("Second role"), containsString("https://issuer.test"),
                    containsString("Rollen er koblet til utstedarsertifikat"), containsString("name=\"entitlements[1].entitlement\""),
                    containsString("Avbryt"), containsString("Tilbake til registreringa"))));
    }

    @Test
    @DisplayName("Should save added role, issuer URL and EAA for an administrator without changing other data")
    void shouldSaveRolesIssuerUrlAndEaaForAdministrator() throws Exception {
        enqueueLookup();
        enqueueRoleCatalog();
        expectedRolesRequest = """
            {"relyingPartyEntitlements":[
               {"entitlement":"%s","credentialIssuerUrl":"https://issuer.test"},
               {"entitlement":"urn:second","credentialIssuerUrl":"https://second.test"}],
             "relyingPartyEaas":[{"namespace":"eu.europa.ec.eudi.pid","intent":"login"}]}
            """.formatted(QEAA);
        enqueue("PUT", "/v2/instances/" + relyingPartyResource.id(), "{}");

        String redirect = mockMvc.perform(post("/roles/edit/" + relyingPartyResource.id()).with(login(true)).with(csrf())
                .param("returnUrl", "/search?searchTerm=abc")
                .param("entitlements[0].entitlement", QEAA)
                .param("entitlements[0].credentialIssuerUrl", "https://issuer.test")
                .param("entitlements[1].entitlement", "urn:second")
                .param("entitlements[1].credentialIssuerUrl", " https://second.test ")
                .param("eaas[0].namespace", "eu.europa.ec.eudi.pid").param("eaas[0].intent", "login")
                .param("name", "Changed").param("active", "false"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();

        assertTrue(redirect.startsWith("/details/" + relyingPartyResource.id() + "?returnUrl="));
        assertTrue(redirect.contains("searchTerm"));
    }

    @Test
    @DisplayName("Should save issuer URL and EAA for self-service while the role set stays unchanged")
    void shouldSaveEaaForSelfServiceAndKeepRoles() throws Exception {
        enqueueLookup();
        String form = mockMvc.perform(get("/roles/edit/" + relyingPartyResource.id()).with(login(false)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertFalse(form.contains("type=\"checkbox\""), "Self-service must not get role checkboxes");
        assertFalse(form.contains("for=\"role-0\""), "Self-service role names must not label a missing checkbox");
        assertTrue(form.contains("Berre administrator kan leggje til eller fjerne roller"));

        enqueueLookup();
        expectedRolesRequest = """
            {"relyingPartyEntitlements":[{"entitlement":"%s","credentialIssuerUrl":"https://issuer.test"}],
             "relyingPartyEaas":[{"namespace":"ns","intent":"purpose"}]}
            """.formatted(QEAA);
        enqueue("PUT", "/v2/instances/" + relyingPartyResource.id(), "{}");
        mockMvc.perform(post("/roles/edit/" + relyingPartyResource.id()).with(login(false)).with(csrf())
                .param("entitlements[0].entitlement", QEAA)
                .param("entitlements[0].credentialIssuerUrl", "https://issuer.test")
                .param("eaas[0].namespace", "ns").param("eaas[0].intent", "purpose"))
            .andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"removed", "added", "other"})
    @DisplayName("Should deny self-service a changed role set without sending anything to the register")
    void shouldDenySelfServiceChangedRoleSet(String manipulation) throws Exception {
        enqueueLookup();
        var request = post("/roles/edit/" + relyingPartyResource.id()).with(login(false)).with(csrf())
            .param("eaas[0].namespace", "ns").param("eaas[0].intent", "purpose");
        switch (manipulation) {
            case "removed" -> { }
            case "added" -> request.param("entitlements[0].entitlement", QEAA)
                .param("entitlements[1].entitlement", "urn:second");
            default -> request.param("entitlements[0].entitlement", "urn:other");
        }
        mockMvc.perform(request).andExpectAll(status().isNotFound(), view().name("error/404"));
    }

    @Test
    @DisplayName("Should keep entered values and show field errors when an issuer URL is not https")
    void shouldShowFieldErrorForInvalidIssuerUrl() throws Exception {
        enqueueLookup();
        enqueueRoleCatalog();
        mockMvc.perform(post("/roles/edit/" + relyingPartyResource.id()).with(login(true)).with(csrf())
                .param("entitlements[0].entitlement", QEAA)
                .param("entitlements[0].credentialIssuerUrl", "http://insecure.test")
                .param("eaas[0].namespace", "ns").param("eaas[0].intent", "purpose"))
            .andExpectAll(status().isOk(), view().name("roles_form_view"),
                content().string(allOf(containsString("http://insecure.test"), containsString("Rett feila før du lagrar"),
                    containsString("aria-invalid=\"true\""), containsString("ikkje ein gyldig HTTPS-URL"),
                    containsString("data-intent=\"purpose\""), containsString("autofocus"))));
    }

    @Test
    @DisplayName("Should show the form with an error and keep input when the register rejects the change")
    void shouldShowFormErrorWhenRegisterRejectsRoles() throws Exception {
        enqueueLookup();
        enqueueRoleCatalog();
        expectedRequests.add("PUT /v2/instances/" + relyingPartyResource.id());
        server.enqueue(new MockResponse().setResponseCode(400).setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"bad_request\",\"error_description\":\"Cannot remove entitlement with issuer certificates\"}"));
        mockMvc.perform(post("/roles/edit/" + relyingPartyResource.id()).with(login(true)).with(csrf())
                .param("entitlements[1].entitlement", "urn:second")
                .param("entitlements[1].credentialIssuerUrl", "https://second.test"))
            .andExpectAll(status().isOk(), view().name("roles_form_view"),
                content().string(allOf(containsString("Endringa kunne ikkje lagrast"),
                    containsString("https://second.test"), containsString("Rett feila før du lagrar"))));
    }

    @Test
    @DisplayName("Should let only administrators edit roles of an inactive registration")
    void shouldLetOnlyAdministratorsEditRolesOfInactiveRegistration() throws Exception {
        relyingPartyResource = relyingPartyResource.withActive(false);
        enqueueLookup();
        mockMvc.perform(get("/roles/edit/" + relyingPartyResource.id()).with(login(false)))
            .andExpectAll(status().isNotFound(), view().name("error/404"));

        enqueueLookup();
        mockMvc.perform(post("/roles/edit/" + relyingPartyResource.id()).with(login(false)).with(csrf())
                .param("entitlements[0].entitlement", QEAA))
            .andExpectAll(status().isNotFound(), view().name("error/404"));

        enqueueLookup();
        enqueueRoleCatalog();
        mockMvc.perform(get("/roles/edit/" + relyingPartyResource.id()).with(login(true)))
            .andExpect(status().isOk());

        enqueueLookup();
        String details = mockMvc.perform(get("/details/" + relyingPartyResource.id()).with(login(false)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(details.contains("/roles/edit/"), "Self-service gets no edit link for inactive registrations");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    @DisplayName("Should deny editing roles for a registration that belongs to another organization")
    void shouldDenyEditingRolesForAnotherOrganization(String method) throws Exception {
        enqueueLookup();
        var request = method.equals("GET") ? get("/roles/edit/" + relyingPartyResource.id())
            : post("/roles/edit/" + relyingPartyResource.id()).with(csrf());
        mockMvc.perform(request.with(SecurityTestUtils.oidcLoginForOrgno("987654321")))
            .andExpectAll(status().isNotFound(), view().name("error/404"));
    }

    private void enqueueRoleCatalog() {
        enqueue("GET", "/v2/entitlements?includeInactive=false", new V2RegisterClient.Entitlements(List.of(
            new V2RegisterClient.Entitlement(new UUID(0, 7), QEAA, true, "Issuer"),
            new V2RegisterClient.Entitlement(new UUID(0, 8), "urn:second", true, "Second role"))));
    }

    private void enqueueRelyingParty(String alias) {
        enqueue("GET", "/v2/relying-parties/" + relyingPartyId, new V2RegisterClient.RelyingParty(
            relyingPartyId, relyingPartyResource.orgno(), alias, "NO", false,
            new RelyingPartyResource.LegalPersonDetails(List.of("Legal name"), List.of()),
            List.of("Postboks 1", "0100 Oslo"), List.of("post@example.test"), List.of(), List.of("https://example.test")));
    }

    private RequestPostProcessor login(boolean administrator) {
        return administrator ? user("admin").roles("ADMIN") : SecurityTestUtils.oidcLoginForOrgno(relyingPartyResource.orgno());
    }

    private RelyingPartyCertificateResource certificate(CertificateType certificateType) {
        return (certificateType == CertificateType.ISSUER ? relyingPartyResource.issuerCertificates() : relyingPartyResource.accessCertificates()).getFirst();
    }

    private RelyingPartyCertificateResource activeCertificate(CertificateType certificateType) {
        var certificate = certificate(certificateType);
        return new RelyingPartyCertificateResource(certificate.certificate(), certificate.id(), certificate.entitlement(), -1);
    }

    private void enqueueSearchPage(int page, String sort, UUID instanceId, String serviceName) {
        var entitlements = new V2RegisterClient.Entitlements(List.of(
            new V2RegisterClient.Entitlement(new UUID(0, 7), "urn:first", true, "First role"),
            new V2RegisterClient.Entitlement(new UUID(0, 8), "urn:second", true, "Second role")));
        var instance = new V2RegisterClient.Instance(instanceId, relyingPartyResource.orgno(), relyingPartyResource.orgName(),
            serviceName, List.of(new V2RegisterClient.RelyingPartyEntitlement("urn:first", null, null),
                new V2RegisterClient.RelyingPartyEntitlement("urn:second", null, null)),
            relyingPartyResource.createdMs(), relyingPartyResource.lastUpdatedMs(),
            List.of(certificate(CertificateType.ACCESS).id()), List.of(certificate(CertificateType.ISSUER).id()));
        enqueue("GET", "/v2/instances?searchTerm=Test&includeInactive=true&page=" + page + "&pageSize=25&sort=" + sort
                + "&requiredEntitlements=urn%3Afirst&requiredEntitlements=urn%3Asecond",
            new PagedResponse<>(List.of(instance), new PagedResponse.PageMetadata(25, page, 26, 2)));
        enqueue("GET", "/v2/entitlements?includeInactive=true", entitlements);
        enqueue("GET", "/v2/entitlements?includeInactive=false", entitlements);
    }

    private void enqueueLookup() {
        enqueue(
            "GET",
            "/v2/registrations/" + relyingPartyResource.id(),
            new V2RegisterClient.RegistrationDetails(
                relyingPartyResource.id(),
                relyingPartyServiceId,
                relyingPartyId,
                relyingPartyResource.orgno(),
                relyingPartyResource.orgName(),
                relyingPartyResource.tradeName(),
                relyingPartyResource.publicSector(),
                relyingPartyResource.relyingPartyEntitlements()
                    .stream()
                    .map(relyingPartyEntitlement -> new V2RegisterClient.RelyingPartyEntitlement(
                        relyingPartyEntitlement.entitlement(),
                        relyingPartyEntitlement.displayName(),
                        relyingPartyEntitlement.credentialIssuerUrl()))
                    .toList(),
                relyingPartyResource.relyingPartyEaas(),
                relyingPartyResource.accessCertificates(),
                relyingPartyResource.issuerCertificates(),
                relyingPartyResource.createdMs(),
                relyingPartyResource.lastUpdatedMs(),
                relyingPartyResource.active(),
                relyingPartyResource.relyingParty(),
                relyingPartyResource.relyingPartyService()));
        enqueue("GET", "/v2/services/" + relyingPartyServiceId + "/intended-uses", List.of());
    }

    private void enqueue(String method, String path, Object response) {
        expectedRequests.add(method + " " + path);
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(response instanceof String json ? json : jsonMapper.writeValueAsString(response)));
    }
}
