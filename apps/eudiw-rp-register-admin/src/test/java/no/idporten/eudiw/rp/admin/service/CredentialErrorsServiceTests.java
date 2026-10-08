package no.idporten.eudiw.rp.admin.service;

import no.idporten.eudiw.rp.admin.service.credentialsservice.CredentialsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CredentialErrorsServiceTests {
    @Test
    void retrievesOnlyErrorsForSavedIssuer() {
        var builder = RestClient.builder().baseUrl("https://registry.example");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://registry.example/v1/credential-errors"))
                .andExpect(content().json("{\"issuer\":\"https://issuer.example\"}"))
                .andRespond(withSuccess("[{\"name\":\"EUCC\",\"errors\":[\"path: invalid index\"]}]", MediaType.APPLICATION_JSON));
        var errors = new CredentialsService(builder.build()).getCredentialErrors("https://issuer.example");
        assertThat(errors).containsExactly(new CredentialsService.CredentialValidationError("EUCC", java.util.List.of("path: invalid index")));
        server.verify();
    }

    @Test
    void unavailableRegistryDoesNotPreventDetailsPageFromLoading() {
        var builder = RestClient.builder().baseUrl("https://registry.example");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://registry.example/v1/credential-errors")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(new CredentialsService(builder.build()).getCredentialErrors("https://issuer.example"))
                .containsExactly(new CredentialsService.CredentialValidationError("Utstedarmetadata", java.util.List.of("Kunne ikkje hente valideringsfeil frå bevisregisteret.")));
        server.verify();
    }
}
