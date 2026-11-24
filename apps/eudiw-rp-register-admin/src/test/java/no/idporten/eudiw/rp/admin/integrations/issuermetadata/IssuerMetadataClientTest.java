package no.idporten.eudiw.rp.admin.integrations.issuermetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IssuerMetadataClientTest {

    private static final URI ENDPOINT = URI.create("https://issuer.example/.well-known/openid-credential-issuer");
    private MockRestServiceServer server;
    private IssuerMetadataClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new IssuerMetadataClient(builder.build());
    }

    @Test
    @DisplayName("Should read issuer metadata from a successful response")
    void shouldReadIssuerMetadataFromASuccessfulResponse() throws IOException {
        server.expect(requestTo(ENDPOINT))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Accept", "application/json"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), client.fetch(ENDPOINT));
        server.verify();
    }

    @Test
    @DisplayName("Should reject an unsuccessful issuer response")
    void shouldRejectAnUnsuccessfulIssuerResponse() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThrows(IOException.class, () -> client.fetch(ENDPOINT));
        server.verify();
    }

    @Test
    @DisplayName("Should reject issuer metadata exceeding the size limit")
    void shouldRejectIssuerMetadataExceedingTheSizeLimit() {
        server.expect(requestTo(ENDPOINT))
            .andRespond(withSuccess(new byte[512 * 1024 + 1], MediaType.APPLICATION_JSON));

        assertThrows(IOException.class, () -> client.fetch(ENDPOINT));
        server.verify();
    }
}
