package no.eudiw.rp.register.api.v2.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v2.endpoints.entities.CredentialIssuerUrlsResponse;
import no.eudiw.rp.register.service.CredentialIssuersService;
import no.idporten.logging.audit.Audit;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "credential-issuers-v2-api", description = "Internal credential issuer URLs API")
@RestController
@RequestMapping("/v2/credential-issuers")
@RequiredArgsConstructor
public class V2CredentialIssuersEndpoint {

    private final CredentialIssuersService credentialIssuersService;

    @Operation(
        summary = "Get credential issuer URLs",
        description = "Returns credential_issuer_urls with unique, non-null registered URLs "
            + "from active parties, services and instances. A successful register read with no active issuer URLs "
            + "returns 200 with an empty list. Register read failures return an error, not an empty list. "
            + "Requires X-API-KEY."
    )
    @ApiResponse(responseCode = "200", description = "Credential issuer URLs retrieved")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @Audit(auditId = "V2-CREDENTIAL-ISSUER-URLS-RETRIEVED")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CredentialIssuerUrlsResponse getCredentialIssuerUrls() {
        return new CredentialIssuerUrlsResponse(
            credentialIssuersService.getRegisteredCredentialIssuerUrls());
    }
}
