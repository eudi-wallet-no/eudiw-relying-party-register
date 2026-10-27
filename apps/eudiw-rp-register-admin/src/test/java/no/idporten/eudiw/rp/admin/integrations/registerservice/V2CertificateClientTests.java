package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.integrations.registerservice.config.RelyingPartiesServiceProperties;
import no.idporten.eudiw.rp.admin.integrations.registerservice.config.V2CertificateClientConfig;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class V2CertificateClientTests {

    private final UUID instanceId = UUID.randomUUID();
    private final RelyingPartyCertificateResource certificate = ResourceGenerator.generateCertificateResource();
    private final RelyingPartyCsrResource csr = ResourceGenerator.generateCsrResource();
    private final JsonMapper mapper = new JsonMapper();
    private MockWebServer server;
    private CloseableHttpClient httpClient;
    private RelyingPartiesService service;

    V2CertificateClientTests() throws Exception { }

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        var properties = new RelyingPartiesServiceProperties(
            new RelyingPartiesServiceProperties.RegisterServiceApi(server.url("/").toString().replaceAll("/$", ""),
                "X-API-KEY", "certificate-test-key"),
            new RelyingPartiesServiceProperties.RestClient(1000, 200),
            new RelyingPartiesServiceProperties.RelyingPartyCertificateConfig(30));
        var config = new V2CertificateClientConfig(properties);
        httpClient = config.v2CertificateHttpClient();
        var client = new V2CertificateClient(config.v2CertificateRestClient(RestClient.builder(), httpClient));
        service = new RelyingPartiesService(RestClient.builder().build(), null, client);
    }

    @AfterEach
    void tearDown() throws Exception {
        httpClient.close();
        server.shutdown();
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    @DisplayName("Should issue an access or issuer certificate for the requested instance")
    void shouldIssueCertificatesForTheInstanceWithTheOriginalCsr(String type) throws Exception {
        server.enqueue(certificateResponse(-1));

        RelyingPartyCertificateResource result = requestCertificate(type);

        assertEquals(certificate.certificate(), result.certificate());
        assertEquals(-1, result.revocationStatus());
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertRequest(request, "POST", certificatePath(type));
        var body = mapper.readTree(request.getBody().readUtf8());
        assertEquals(mapper.valueToTree(csr).get("csr"), body.get("csr"));
        if (type.equals("issuer")) {
            assertEquals("pid_provider", body.get("entitlement").asText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    @DisplayName("Should retrieve the requested access or issuer certificate with its revocation status")
    void shouldRetrieveTheOwnedCertificateWithItsRevocationStatus(String type) throws Exception {
        server.enqueue(certificateResponse(1));

        RelyingPartyCertificateResource result = getCertificate(type);

        assertEquals(certificate.certificate(), result.certificate());
        assertEquals(certificate.id(), result.id());
        assertEquals(1, result.revocationStatus());
        assertRequest(server.takeRequest(1, TimeUnit.SECONDS), "GET", ownedCertificatePath(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    @DisplayName("Should revoke an active access or issuer certificate owned by the instance")
    void shouldRevokeAnActiveOwnedCertificate(String type) throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        revokeCertificate(type);

        assertRequest(server.takeRequest(1, TimeUnit.SECONDS), "PATCH", ownedCertificatePath(type) + "/revoke");
        assertEquals(1, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    @DisplayName("Should report a missing certificate when revocation is rejected")
    void shouldReportMissingCertificatesWhenRevocationIsRejected(String type) throws Exception {
        server.enqueue(errorResponse(404, "not_found"));

        var exception = assertThrows(CertificateNotFoundException.class, () -> revokeCertificate(type));

        assertEquals(instanceId.toString(), exception.getRpId());
        assertEquals(certificate.id().toString(), exception.getCertificateId());
        assertEquals(1, server.getRequestCount());
        assertRequest(server.takeRequest(1, TimeUnit.SECONDS), "PATCH", ownedCertificatePath(type) + "/revoke");
    }

    @Test
    @DisplayName("Should report a rejected certificate request without fallback")
    void shouldKeepValidationFailuresVisibleWithoutFallback() {
        server.enqueue(errorResponse(400, "invalid_request"));

        assertThrows(BadRequestException.class, () -> requestCertificate("access"));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    @DisplayName("Should report a register server error without fallback")
    void shouldReportRegisterServerErrorWithoutFallback() {
        server.enqueue(errorResponse(500, "server_error"));

        var exception = assertThrows(ErrorResponseException.class, () -> requestCertificate("access"));

        assertEquals("server_error", exception.getError());
        assertEquals(1, server.getRequestCount());
    }

    @Test
    @DisplayName("Should report a rejected revocation without fallback")
    void shouldReportRejectedRevocationWithoutFallback() {
        server.enqueue(errorResponse(400, "invalid_request"));

        assertThrows(BadRequestException.class, () -> revokeCertificate("access"));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    @DisplayName("Should propagate an issuance timeout without retry")
    void shouldPropagateIssuanceTimeoutWithoutRetry() throws Exception {
        server.enqueue(certificateResponse(-1).setHeadersDelay(1, TimeUnit.SECONDS));

        assertThrows(ResourceAccessException.class, () -> requestCertificate("access"));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    @DisplayName("Should propagate a revocation timeout without retry")
    void shouldPropagateRevocationTimeoutWithoutRetry() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204).setHeadersDelay(1, TimeUnit.SECONDS));

        assertThrows(ResourceAccessException.class, () -> revokeCertificate("access"));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    @DisplayName("Should report a lost certificate lookup without retry")
    void shouldNotRetryLookupAfterALostConnection() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        server.enqueue(new MockResponse().setResponseCode(204));

        assertThrows(ResourceAccessException.class, () -> getCertificate("access"));
        assertEquals(1, server.getRequestCount());
    }

    private MockResponse certificateResponse(int revocationStatus) throws Exception {
        var resource = new RelyingPartyCertificateResource(certificate.certificate(), certificate.id(),
            certificate.entitlement(), revocationStatus);
        return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(resource));
    }

    private MockResponse errorResponse(int status, String error) {
        return new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"" + error + "\",\"error_description\":\"Register rejected operation\"}");
    }

    private RelyingPartyCertificateResource requestCertificate(String type) {
        if (type.equals("issuer")) {
            return service.requestIssuerCertificateForEntitlement(instanceId, new IssuerCsrResource(csr.csr(), "pid_provider"));
        }
        return service.requestCertificateForRelyingParty(instanceId, csr);
    }

    private RelyingPartyCertificateResource getCertificate(String type) {
        if (type.equals("issuer")) {
            return service.getIssuerCertificate(instanceId, certificate.id());
        }
        return service.getAccessCertificate(instanceId, certificate.id());
    }

    private void revokeCertificate(String type) {
        if (type.equals("issuer")) {
            service.revokeIssuerCertificate(instanceId, certificate.id());
            return;
        }
        service.revokeAccessCertificate(instanceId, certificate.id());
    }

    private String certificatePath(String type) {
        return "/v2/instances/" + instanceId + "/certificates/" + type;
    }

    private String ownedCertificatePath(String type) {
        return certificatePath(type) + "/" + certificate.id();
    }

    private void assertRequest(RecordedRequest request, String method, String path) {
        assertNotNull(request);
        assertEquals(method, request.getMethod());
        assertEquals(path, request.getPath());
        assertEquals("certificate-test-key", request.getHeader("X-API-KEY"));
    }
}
