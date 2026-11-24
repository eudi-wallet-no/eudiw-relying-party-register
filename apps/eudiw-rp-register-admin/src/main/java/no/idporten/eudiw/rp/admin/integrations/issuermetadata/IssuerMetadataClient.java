package no.idporten.eudiw.rp.admin.integrations.issuermetadata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;

@Component
public class IssuerMetadataClient {

    private static final String WELL_KNOWN_PATH = "/.well-known/openid-credential-issuer";
    private static final int MAX_BODY_BYTES = 512 * 1024;
    private final RestClient client;

    public IssuerMetadataClient(
        @Qualifier("issuerMetadataRestClient") RestClient client
    ) {
        this.client = client;
    }

    byte[] fetch(URI endpoint) throws IOException {
        try {
            return client.get()
                .uri(endpoint)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> readBody(response));
        } catch (ResourceAccessException e) {
            throw new IOException("Issuer metadata request failed", e);
        }
    }

    static String issuerFor(URI endpoint) {
        String path = endpoint.getRawPath();
        if (!isValidMetadataEndpoint(endpoint, path)) {
            throw new IllegalArgumentException("Expected an HTTPS credential issuer metadata URL");
        }

        return endpoint.getScheme() + "://" + endpoint.getRawAuthority()
            + path.substring(WELL_KNOWN_PATH.length());
    }

    private static boolean isValidMetadataEndpoint(URI endpoint, String path) {
        return isHttpsEndpoint(endpoint)
            && endpoint.getHost() != null
            && hasNoUserInfoQueryOrFragment(endpoint)
            && isWellKnownIssuerPath(path)
            && endpoint.normalize().equals(endpoint);
    }

    private static boolean isHttpsEndpoint(URI endpoint) {
        return "https".equalsIgnoreCase(endpoint.getScheme());
    }

    private static boolean hasNoUserInfoQueryOrFragment(URI endpoint) {
        return endpoint.getRawUserInfo() == null
            && endpoint.getRawQuery() == null
            && endpoint.getRawFragment() == null;
    }

    private static boolean isWellKnownIssuerPath(String path) {
        return path != null
            && (path.equals(WELL_KNOWN_PATH) || path.startsWith(WELL_KNOWN_PATH + "/"));
    }

    private static byte[] readBody(ClientHttpResponse response) throws IOException {
        var headers = response.getHeaders();
        if (response.getStatusCode().value() != 200 || headers.getContentLength() > MAX_BODY_BYTES
            || headers.containsHeader(HttpHeaders.CONTENT_ENCODING)) {
            throw new IOException("Invalid issuer metadata response");
        }

        // Bound the bytes parsed here; closing the response may still consume the remaining body.
        var body = response.getBody().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new IOException("Issuer metadata response is too large");
        }

        return body;
    }
}
