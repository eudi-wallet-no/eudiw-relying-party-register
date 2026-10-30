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
            null,
            null,
            List.of());
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
