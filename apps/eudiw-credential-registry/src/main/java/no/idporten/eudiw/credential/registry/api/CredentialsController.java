package no.idporten.eudiw.credential.registry.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import no.idporten.eudiw.credential.registry.response.CredentialRegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import no.idporten.eudiw.credential.registry.response.model.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CredentialsController {

    private final CredentialRegisterService credentialRegisterService;


    @Autowired
    public CredentialsController(CredentialRegisterService credentialRegisterService) {
        this.credentialRegisterService = credentialRegisterService;
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
}
