package no.idporten.eudiw.rp.admin.integrations.issuermetadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IssuerMetadataServiceTest {

    private final IssuerMetadataClient client = mock(IssuerMetadataClient.class);
    private final JsonMapper jsonMapper = new JsonMapper();
    private final IssuerMetadataService service = new IssuerMetadataService(client, jsonMapper);
    private final URI endpoint = URI.create("https://issuer.example/.well-known/openid-credential-issuer/example");

    @Test
    @DisplayName("Should retain typed paths for SD-JWT and reject missing metadata")
    void shouldRetainTypedPathsForSdJwtAndRejectMissingMetadata() throws Exception {
        String response = """
            {"credential_issuer":"https://issuer.example/example",
             "credential_configurations_supported":{
              "good":{"format":"dc+sd-jwt","vct":"type","credential_metadata":{"claims":[
                {"path":["part.with.dot",0,null]}]}},
              "missing":{"format":"dc+sd-jwt","credential_metadata":{"claims":[{"path":["x"]}]}},
              "mdoc":{"format":"mso_mdoc","doctype":"doc"}
             }}
            """;
        when(client.fetch(endpoint)).thenReturn(response.getBytes(StandardCharsets.UTF_8));

        var definitions = service.load(endpoint);

        assertEquals("type", definitions.get(0).vct());
        assertEquals(Arrays.asList("part.with.dot", BigInteger.ZERO, null), definitions.get(0).claims().get(0).path());
        assertNotNull(definitions.get(1).error());
        assertNotNull(definitions.get(2).error());
    }

    @Test
    @DisplayName("Should abort when issuer metadata has an invalid configuration structure")
    void shouldAbortWhenIssuerMetadataHasAnInvalidConfigurationStructure() throws Exception {
        String response = """
            {"credential_issuer":"https://issuer.example/example",
             "credential_configurations_supported":[]}
            """;
        when(client.fetch(endpoint)).thenReturn(response.getBytes(StandardCharsets.UTF_8));

        assertThrows(IssuerMetadataException.class, () -> service.load(endpoint));
    }

    @Test
    @DisplayName("Should reject issuer metadata that identifies a different issuer")
    void shouldRejectIssuerMetadataThatIdentifiesADifferentIssuer() throws Exception {
        String response = """
            {"credential_issuer":"https://another.example/example",
             "credential_configurations_supported":{"credential":{"format":"dc+sd-jwt"}}}
            """;
        when(client.fetch(endpoint)).thenReturn(response.getBytes(StandardCharsets.UTF_8));

        assertThrows(IssuerMetadataException.class, () -> service.load(endpoint));
    }

    @Test
    @DisplayName("Should process metadata with many credentials, claims and path segments")
    void shouldProcessMetadataWithManyCredentialsClaimsAndPathSegments() throws Exception {
        List<String> path = Collections.nCopies(33, "segment");
        List<Map<String, Object>> claims = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            claims.add(Map.of("path", path));
        }

        Map<String, Object> supportedCredential = Map.of(
            "format", "dc+sd-jwt",
            "vct", "type",
            "credential_metadata", Map.of("claims", claims)
        );
        Map<String, Object> configurations = new LinkedHashMap<>();
        configurations.put("supported", supportedCredential);
        for (int i = 0; i < 100; i++) {
            configurations.put("unsupported-%d".formatted(i), Map.of("format", "mso_mdoc"));
        }

        String response = jsonMapper.writeValueAsString(Map.of(
            "credential_issuer", "https://issuer.example/example",
            "credential_configurations_supported", configurations
        ));
        when(client.fetch(endpoint)).thenReturn(response.getBytes(StandardCharsets.UTF_8));

        var definitions = service.load(endpoint);

        assertEquals(101, definitions.size());
        assertEquals(101, definitions.getFirst().claims().size());
        assertEquals(33, definitions.getFirst().claims().getFirst().path().size());
    }

    @Test
    @DisplayName("Should reject malformed issuer metadata")
    void shouldRejectMalformedIssuerMetadata() throws Exception {
        when(client.fetch(endpoint)).thenReturn("{".getBytes(StandardCharsets.UTF_8));

        assertThrows(IssuerMetadataException.class, () -> service.load(endpoint));
    }

    @Test
    @DisplayName("Should reject invalid metadata addresses")
    void shouldRejectInvalidMetadataAddresses() {
        for (String url : List.of("http://issuer.example/.well-known/openid-credential-issuer",
            "https://-issuer.example/.well-known/openid-credential-issuer",
            "https://user@issuer.example/.well-known/openid-credential-issuer")) {
            assertThrows(IllegalArgumentException.class, () -> IssuerMetadataClient.issuerFor(URI.create(url)));
        }
    }

    @Test
    @DisplayName("Should accept HTTPS metadata addresses with a host")
    void shouldAcceptHttpsMetadataAddressesWithAHost() {
        for (String host : List.of("localhost", "127.0.0.1", "[::1]", "issuer.example", "issuer.example:8443")) {
            URI address = URI.create("https://" + host + "/.well-known/openid-credential-issuer");

            assertEquals("https://" + host, IssuerMetadataClient.issuerFor(address));
        }
    }

}
