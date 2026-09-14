package no.eudiw.rp.register.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.RegisterServiceApiSwaggerExamples;
import no.eudiw.rp.register.api.resource.entitlements.EntitlementsResource;
import no.eudiw.rp.register.service.EntitlementService;
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
            description = "Error response", value = RegisterServiceApiSwaggerExamples.BAD_REQUEST_ERROR_EXAMPLE)
        )),
    @ApiResponse(responseCode = "404",
        description = "Not found",
        content = @Content(examples = @ExampleObject(
            description = "Error response", value = RegisterServiceApiSwaggerExamples.NOT_FOUND_ERROR_EXAMPLE)
        )),
    @ApiResponse(responseCode = "500",
        description = "Server error",
        content = @Content(examples = @ExampleObject(
            description = "Error response", value = RegisterServiceApiSwaggerExamples.SERVER_ERROR_EXAMPLE)
        ))
})
@Validated
@RestController
@RequestMapping("/v1/entitlement")
@RequiredArgsConstructor
public class EntitlementController {

    private static final String ENTITLEMENTS_RETRIEVED = "ENTITLEMENTS-RETRIEVED";

    private final EntitlementService entitlementService;

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
        return ResponseEntity.ok(entitlementService.findAllEntitlements(includeInactive));
    }
}
