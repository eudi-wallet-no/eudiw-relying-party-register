package no.idporten.eudiw.rp.register.lookup.service.credentialsservice;

import jakarta.validation.Validation;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrregCatalogueTest {
    @Test
    void frontendAcceptsAllBrregCredentialsIncludingEuccArrayClaims() throws Exception {
        String response;
        try (var stream = getClass().getResourceAsStream("/brreg-catalogue-response.json")) {
            assertNotNull(stream);
            response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        var builder = RestClient.builder().baseUrl("https://registry.example");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://registry.example/credentials"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new CredentialsService(builder.build(), factory.getValidator());
            var credentials = service.getAvailableCredentials().credentials();
            assertEquals(2, credentials.size());
            assertTrue(credentials.stream().anyMatch(c -> c.getCredentialTypeDisplayName("no").equals("EBWOID")));
            var eucc = credentials.stream().filter(c -> c.getCredentialType().equals("uri:eu:eudi:eucc:1"))
                    .findFirst().orElseThrow();
            assertEquals(1, eucc.getMetadata().getClaims().stream()
                    .filter(c -> c.getPaths().contains(null)).count());
            var mapper = JsonMapper.builder().build();
            for (var claim : eucc.getMetadata().getClaims()) {
                assertEquals(mapper.valueToTree(claim.getPaths()), mapper.readTree(claim.getDcqlFormattedPaths()));
            }
            server.verify();
        }
    }

    @Test
    void dcqlPathsPreserveNullIntegersAndEscapedPropertyNames() {
        var mapper = JsonMapper.builder().build();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String path : List.of("[\"representatives\",null,\"name\"]", "[\"representatives\",0]",
                    "[\"representatives\",2147483648]", "[\"a\\\"b\"]", "[\"\"]")) {
                var claim = mapper.readValue("{\"path\":" + path + "}", CredentialMetadata.Claims.class);
                assertTrue(factory.getValidator().validate(claim).isEmpty(), path);
                assertEquals(mapper.readTree(path), mapper.readTree(claim.getDcqlFormattedPaths()));
            }
            for (String path : List.of("null", "[]", "[true]", "[1.5]", "[-1]", "[{}]", "[[]]")) {
                var claim = mapper.readValue("{\"path\":" + path + "}", CredentialMetadata.Claims.class);
                assertFalse(factory.getValidator().validate(claim).isEmpty(), path);
            }
        }
    }
}
