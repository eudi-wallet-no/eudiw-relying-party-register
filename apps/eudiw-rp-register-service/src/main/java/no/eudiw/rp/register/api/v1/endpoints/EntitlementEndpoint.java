package no.eudiw.rp.register.api.v1.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v1.openapi.V1ApiExamples;
import no.eudiw.rp.register.api.v1.resource.entitlements.EntitlementsResource;
import no.eudiw.rp.register.api.v1.V1ApiService;
import no.idporten.logging.audit.Audit;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "entitlement-api", description = "Entitlement Api")
@ApiResponses({
    @ApiResponse(responseCode = "400",
        description = "Invalid request",
        content = @Content(examples = @ExampleObject(
            description = "Error response", value = V1ApiExamples.BAD_REQUEST_ERROR_EXAMPLE)
        )),
    @ApiResponse(responseCode = "404",
        description = "Not found",
        content = @Content(examples = @ExampleObject(
            description = "Error response", value = V1ApiExamples.NOT_FOUND_ERROR_EXAMPLE)
        )),
    @ApiResponse(responseCode = "500",
        description = "Server error",
        content = @Content(examples = @ExampleObject(
            description = "Error response", value = V1ApiExamples.SERVER_ERROR_EXAMPLE)
        ))
})
@Validated
@RestController
@RequestMapping("/v1/entitlement")
@RequiredArgsConstructor
public class EntitlementEndpoint {

    private static final String ENTITLEMENTS_RETRIEVED = "ENTITLEMENTS-RETRIEVED";

    private final V1ApiService v1Api;

    @Operation(
        summary = "Get all entitlements",
        description = "Retrieve information on all active entitlements",
        tags = {"entitlement-api"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entitlements retrieved")
    })
    @Audit(auditId = ENTITLEMENTS_RETRIEVED)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntitlementsResource> getAllEntitlements(
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(v1Api.findAllEntitlements(includeInactive));
    }
}
