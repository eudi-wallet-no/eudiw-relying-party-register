package no.idporten.eudiw.credential.registry.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import no.idporten.eudiw.credential.registry.integration.CredentialIssuerMetadataRetriever;
import no.idporten.eudiw.credential.registry.response.model.CredentialValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
public class CredentialErrorsController {
    private final CredentialIssuerMetadataRetriever retriever;
    private final ConfigProperties config;

    @Autowired
    public CredentialErrorsController(CredentialIssuerMetadataRetriever retriever, ConfigProperties config) {
        this.retriever = retriever;
        this.config = config;
    }

    public record Request(@NotNull URI issuer) { }

    @PostMapping("/v1/credential-errors")
    public ResponseEntity<List<CredentialValidationError>> errors(@RequestBody @Valid Request request,
                                                                 HttpServletRequest http) {
        if (config.apiKeyValue() == null || config.apiKeyValue().isBlank()
                || !config.apiKeyValue().equals(http.getHeader(config.apiKeyHeaderId()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(retriever.getCredentialErrors(request.issuer()));
    }
}
