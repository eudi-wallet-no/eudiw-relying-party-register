package no.eudiw.rp.register.api.v1.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v1.resource.CredentialIssuerUrlsResource;
import no.eudiw.rp.register.api.v1.V1ApiService;
import no.idporten.logging.audit.Audit;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "credential-issuers-api", description = "Credential issuer URLs API")
@Validated
@RestController
@RequestMapping("/v1/credential-issuers")
@RequiredArgsConstructor
public class CredentialIssuersEndpoint {

    private static final String CREDENTIAL_ISSUER_URLS_RETRIEVED = "CREDENTIAL-ISSUER-URLS-RETRIEVED";

    private final V1ApiService v1Api;

    @Operation(
        summary = "Get credential issuer URLS",
        description = "Get list of unique credential issuer URLs registered"
    )
    @ApiResponse(responseCode = "200", description = "Credential issuer URLs retrieved")
    @Audit(auditId = CREDENTIAL_ISSUER_URLS_RETRIEVED)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialIssuerUrlsResource> getCredentialIssuerUrls() {
        CredentialIssuerUrlsResource credentialIssuerUrls = v1Api.getRegisteredCredentialIssuerUrls();
        return ResponseEntity.ok(credentialIssuerUrls);
    }
}
