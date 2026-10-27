package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.integrations.registerservice.config.RelyingPartiesServiceProperties;
import no.idporten.eudiw.rp.admin.integrations.registerservice.config.V2CertificateClientConfig;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
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
        // A V1 fallback would fail locally instead of reaching another service.
        service = new RelyingPartiesService(RestClient.builder().build(), null, client);
    }

    @AfterEach
    void tearDown() throws Exception {
        httpClient.close();
        server.shutdown();
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
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
    void shouldRevokeOnlyAfterTheRegisterConfirmsAnActiveOwnedCertificate(String type) throws Exception {
        server.enqueue(certificateResponse(-1));
        server.enqueue(new MockResponse().setResponseCode(204));

        revokeCertificate(type);

        assertRequest(server.takeRequest(1, TimeUnit.SECONDS), "GET", ownedCertificatePath(type));
        assertRequest(server.takeRequest(1, TimeUnit.SECONDS), "PATCH", ownedCertificatePath(type) + "/revoke");
        assertEquals(2, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldAcceptRegisterConfirmedRevocationWithoutAnotherCaOperation(String type) throws Exception {
        server.enqueue(certificateResponse(1));

        revokeCertificate(type);

        assertEquals(1, server.getRequestCount());
        assertRequest(server.takeRequest(1, TimeUnit.SECONDS), "GET", ownedCertificatePath(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldReportMissingOwnedCertificatesWithoutAttemptingRevocation(String type) throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404));

        var exception = assertThrows(CertificateNotFoundException.class, () -> revokeCertificate(type));

        assertEquals(instanceId.toString(), exception.getRpId());
        assertEquals(certificate.id().toString(), exception.getCertificateId());
        assertEquals(1, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldReportACertificateRemovedBetweenLookupAndRevocation(String type) throws Exception {
        server.enqueue(certificateResponse(-1));
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThrows(CertificateNotFoundException.class, () -> revokeCertificate(type));
        assertEquals(2, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldKeepValidationFailuresVisibleWithoutFallback(String type) {
        server.enqueue(errorResponse(400, "invalid_request"));

        assertThrows(BadRequestException.class, () -> requestCertificate(type));
        assertEquals(1, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldNotRetryIssuanceWhenTheRegisterReturnsServiceUnavailable(String type) {
        server.enqueue(errorResponse(503, "server_error").setHeader("Retry-After", "0"));
        server.enqueue(new MockResponse().setResponseCode(204));

        assertThrows(ErrorResponseException.class, () -> requestCertificate(type));
        assertEquals(1, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldNotRetryLookupWhenTheRegisterReturnsServiceUnavailable(String type) {
        server.enqueue(errorResponse(503, "server_error").setHeader("Retry-After", "0"));
        server.enqueue(new MockResponse().setResponseCode(204));

        assertThrows(ErrorResponseException.class, () -> getCertificate(type));
        assertEquals(1, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldKeepCaRevocationFailuresVisibleWithoutRetry(String type) throws Exception {
        server.enqueue(certificateResponse(-1));
        server.enqueue(errorResponse(503, "server_error").setHeader("Retry-After", "0"));
        server.enqueue(new MockResponse().setResponseCode(204));

        assertThrows(ErrorResponseException.class, () -> revokeCertificate(type));
        assertEquals(2, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldNotClaimRevocationWhenOnlyTheCaMayHaveRevokedTheCertificate(String type) throws Exception {
        server.enqueue(certificateResponse(-1));
        server.enqueue(errorResponse(400, "invalid_request"));

        assertThrows(BadRequestException.class, () -> revokeCertificate(type));
        assertEquals(2, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldReportUnknownIssuanceOutcomeAfterTimeoutWithoutRetry(String type) throws Exception {
        server.enqueue(certificateResponse(-1).setHeadersDelay(1, TimeUnit.SECONDS));

        var exception = assertThrows(AdminServiceException.class, () -> requestCertificate(type));

        assertInstanceOf(ResourceAccessException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("outcome is unknown"));
        assertEquals(1, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldReportUnknownRevocationOutcomeAfterTimeoutWithoutRetry(String type) throws Exception {
        server.enqueue(certificateResponse(-1));
        server.enqueue(new MockResponse().setResponseCode(204).setHeadersDelay(1, TimeUnit.SECONDS));

        var exception = assertThrows(AdminServiceException.class, () -> revokeCertificate(type));

        assertInstanceOf(ResourceAccessException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("outcome is unknown"));
        assertEquals(2, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldNotRetryLookupAfterALostConnection(String type) {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        server.enqueue(new MockResponse().setResponseCode(204));

        var exception = assertThrows(AdminServiceException.class, () -> getCertificate(type));

        assertInstanceOf(ResourceAccessException.class, exception.getCause());
        assertEquals(1, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 202})
    void shouldRejectUnconfirmedRevocation(int status) throws Exception {
        server.enqueue(certificateResponse(-1));
        server.enqueue(new MockResponse().setResponseCode(status));

        assertThrows(UnrecognizedErrorResponseException.class, () -> revokeCertificate("access"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{\"certificate\":\"not a certificate\"}"})
    void shouldRejectMissingOrUnreadableIssuanceResponses(String body) {
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body));

        assertThrows(UnrecognizedErrorResponseException.class, () -> requestCertificate("access"));
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 202, 204})
    void shouldRejectUnexpectedIssuanceSuccessStatus(int status) throws Exception {
        server.enqueue(certificateResponse(-1).setResponseCode(status));

        assertThrows(UnrecognizedErrorResponseException.class, () -> requestCertificate("access"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldRejectUnconfirmedCertificateIdentityOrStatus(String type) throws Exception {
        var response = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(certificate);
        response.put("id", UUID.randomUUID().toString());
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(response.toString()));
        assertThrows(UnrecognizedErrorResponseException.class, () -> getCertificate(type));

        response.put("id", certificate.id().toString());
        response.remove("revocation_status");
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(response.toString()));
        assertThrows(UnrecognizedErrorResponseException.class, () -> revokeCertificate(type));

        server.enqueue(certificateResponse(-2));
        assertThrows(UnrecognizedErrorResponseException.class, () -> revokeCertificate(type));
        assertEquals(3, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    void shouldRejectRedirectsWithoutFollowingThem(String type) {
        server.enqueue(new MockResponse().setResponseCode(307).setHeader("Location", server.url("/v1/fallback")));
        server.enqueue(new MockResponse().setResponseCode(204));

        assertThrows(UnrecognizedErrorResponseException.class, () -> requestCertificate(type));
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
