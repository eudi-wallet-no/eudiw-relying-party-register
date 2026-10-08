package no.idporten.eudiw.credential.registry.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import no.idporten.eudiw.credential.registry.integration.CredentialIssuerMetadataRetriever;
import no.idporten.eudiw.credential.registry.response.CredentialRegisterService;
import no.idporten.eudiw.credential.registry.response.model.CredentialValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import no.idporten.eudiw.credential.registry.response.model.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
public class CredentialsController {

    private final CredentialRegisterService credentialRegisterService;
    private final CredentialIssuerMetadataRetriever retriever;
    private final ConfigProperties config;


    @Autowired
    public CredentialsController(CredentialRegisterService credentialRegisterService,
                                 CredentialIssuerMetadataRetriever retriever, ConfigProperties config) {
        this.credentialRegisterService = credentialRegisterService;
        this.retriever = retriever;
        this.config = config;
    }

    @Operation(
            summary = "Hente alle typer bevis som er registrert i sandkassen",
            description = "Hent alle bevisene som finnes på .well-known/openid-credential-issuer endepunktene til registrerte utstedere")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alle bevis hentes"),
            @ApiResponse(responseCode = "500", description = "Intern feil",
            content = @Content(examples= @ExampleObject(description = "error response", value = CredentialRegisterServiceAPISwaggerExample.SERVER_ERROR_EXAMPLE)))
    })


    @GetMapping(value= "/v1/credentials", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Credentials> credentials() {
        if (credentialRegisterService.getCredentials() != null) {
            return ResponseEntity.ok(credentialRegisterService.getCredentials());
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public record CredentialErrorsRequest(@NotNull URI issuer) { }

    @PostMapping("/v1/credential-errors")
    public ResponseEntity<List<CredentialValidationError>> errors(@RequestBody @Valid CredentialErrorsRequest request,
                                                                 HttpServletRequest http) {
        if (config.apiKeyValue() == null || config.apiKeyValue().isBlank()
                || !config.apiKeyValue().equals(http.getHeader(config.apiKeyHeaderId()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(retriever.getCredentialErrors(request.issuer()));
    }
}
