package no.idporten.eudiw.credential.registry.integration;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.idporten.eudiw.credential.registry.integration.model.Claims;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import no.idporten.eudiw.credential.registry.response.CredentialRegisterService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrregIssuerMetadataTest {
    private static final String ISSUER = "https://agent.paradym.id/oid4vci/a3614c5c-aad7-4c72-b1be-2837ffd644af";
    private static final String METADATA_URL = "https://agent.paradym.id/.well-known/openid-credential-issuer/oid4vci/a3614c5c-aad7-4c72-b1be-2837ffd644af";

    // Relevant subset of public issuer metadata fetched on 2026-10-08.
    private String metadata() throws Exception {
        try (var stream = getClass().getResourceAsStream("/brreg-issuer-metadata.json")) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void brregMetadataIncludesEbwoidAndEuccInCatalogue() throws Exception {
        RestClient.Builder rp = RestClient.builder().baseUrl("https://rp.example/credential-issuers");
        var rpServer = MockRestServiceServer.bindTo(rp).build();
        rpServer.expect(requestTo("https://rp.example/credential-issuers"))
                .andRespond(withSuccess("{\"credential_issuer_urls\":[\"" + ISSUER + "\"]}", MediaType.APPLICATION_JSON));
        RestClient.Builder external = RestClient.builder();
        var issuerServer = MockRestServiceServer.bindTo(external).build();
        issuerServer.expect(requestTo(METADATA_URL))
                .andRespond(withSuccess(metadata(), MediaType.APPLICATION_JSON));

        var meters = new SimpleMeterRegistry();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var retriever = new CredentialIssuerMetadataRetriever(factory.getValidator(), rp.build(), external.build(),
                    meters.counter("external.errors"), meters.counter("internal.errors"));
            var service = new CredentialRegisterService(retriever);
            service.updateCredentialMetadataRetriever();
            var credentials = service.getCredentials().credentials();
            assertEquals(2, credentials.size(), "EUCC claim paths must not remove the issuer and EBWOID");
            var names = credentials.stream()
                    .map(c -> c.credentialMetadata().display().getFirst().name()).toList();
            assertTrue(names.contains("EBWOID"));
            assertTrue(names.contains("EUCC"));
            var eucc = credentials.stream().filter(c -> c.credentialType().equals("uri:eu:eudi:eucc:1"))
                    .findFirst().orElseThrow();
            assertEquals(1, eucc.credentialMetadata().claims().stream()
                    .filter(c -> c.path().contains(null)).count());
            var json = JsonMapper.builder().build().valueToTree(service.getCredentials());
            assertEquals(2, json.get("credentials").size());
            rpServer.verify();
            issuerServer.verify();
        } finally {
            meters.close();
        }
    }

    @Test
    void invalidEuccConfigurationDoesNotRemoveEbwoid() throws Exception {
        var issuer = JsonMapper.builder().build().readValue(metadata(), CredentialIssuer.class);
        issuer.getCredentialConfiguration().get("cmtty9uoi000302s64nstreth")
                .getCredentialMetadata().getClaims().getFirst().setPath(List.of(true));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var retriever = new CredentialIssuerMetadataRetriever(factory.getValidator(), null, null, null, null);
            assertNotNull(retriever.validateCredentialIssuer(issuer, java.net.URI.create(ISSUER)));
            assertEquals(1, issuer.getCredentialConfiguration().size());
            assertTrue(issuer.getCredentialConfiguration().containsKey("cmmz1pojj009d01s6mk4uu4h5"));
            assertFalse(issuer.getCredentialConfiguration().containsKey("cmtty9uoi000302s64nstreth"));
        }
    }

    @Test
    void invalidSharedIssuerMetadataIsRejected() throws Exception {
        var issuer = JsonMapper.builder().build().readValue(metadata(), CredentialIssuer.class);
        issuer.setCredentialIssuer("");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var retriever = new CredentialIssuerMetadataRetriever(factory.getValidator(), null, null, null, null);
            assertNull(retriever.validateCredentialIssuer(issuer, java.net.URI.create(ISSUER)));
        }
    }

    @Test
    void euccParseErrorDoesNotRemoveEbwoidFromCatalogue() throws Exception {
        var mapper = JsonMapper.builder().build();
        var metadata = mapper.readTree(metadata());
        ((ObjectNode) metadata.get("credential_configurations_supported").get("cmtty9uoi000302s64nstreth")
                .get("credential_metadata").get("claims").get(0)).put("path", "not an array");
        var rp = RestClient.builder().baseUrl("https://rp.example/credential-issuers");
        var rpServer = MockRestServiceServer.bindTo(rp).build();
        rpServer.expect(requestTo("https://rp.example/credential-issuers"))
                .andRespond(withSuccess("{\"credential_issuer_urls\":[\"" + ISSUER + "\"]}", MediaType.APPLICATION_JSON));
        var external = RestClient.builder();
        var issuerServer = MockRestServiceServer.bindTo(external).build();
        issuerServer.expect(requestTo(METADATA_URL))
                .andRespond(withSuccess(mapper.writeValueAsString(metadata), MediaType.APPLICATION_JSON));
        var meters = new SimpleMeterRegistry();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var retriever = new CredentialIssuerMetadataRetriever(factory.getValidator(), rp.build(), external.build(),
                    meters.counter("external.errors"), meters.counter("internal.errors"));
            var service = new CredentialRegisterService(retriever);
            service.updateCredentialMetadataRetriever();
            assertEquals(1, service.getCredentials().credentials().size());
            assertEquals("EBWOID", service.getCredentials().credentials().getFirst()
                    .credentialMetadata().display().getFirst().name());
            rpServer.verify();
            issuerServer.verify();
        } finally {
            meters.close();
        }
    }

    @Test
    void claimsPathsPreserveJsonTypesAndRejectInvalidComponents() {
        var mapper = JsonMapper.builder().build();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String path : List.of("[\"name\"]", "[\"representatives\",null,\"name\"]",
                    "[\"representatives\",0,\"name\"]", "[\"representatives\",2147483648]", "[\"\"]")) {
                var claim = mapper.readValue("{\"path\":" + path + "}", Claims.class);
                assertTrue(factory.getValidator().validate(claim).isEmpty(), path);
                var serialized = mapper.valueToTree(claim);
                assertEquals(mapper.readTree(path), serialized.get("path"));
                assertFalse(serialized.has("validPath"));
            }
            for (String path : List.of("null", "[]", "[true]", "[1.5]", "[-1]", "[{}]", "[[]]")) {
                var claim = mapper.readValue("{\"path\":" + path + "}", Claims.class);
                assertFalse(factory.getValidator().validate(claim).isEmpty(), path);
            }
        }
    }
}
