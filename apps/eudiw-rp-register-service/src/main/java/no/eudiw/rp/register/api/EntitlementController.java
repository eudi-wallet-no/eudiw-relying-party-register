package no.eudiw.rp.register.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.service.EntitlementService;
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

    private static final String ENTITLEMENT_CREATED = "ENTITLEMENT-CREATED";
    private static final String ENTITLEMENT_UPDATED = "ENTITLEMENT-UPDATED";
    private static final String ENTITLEMENTS_RETRIEVED = "ENTITLEMENTS-RETRIEVED";

    private final EntitlementService entitlementService;

    @Operation(
        summary = "Register entitlement",
        description = "Register a new entitlement",
        tags = {"entitlement-api"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entitlement is created"),
        @ApiResponse(responseCode = "400", description = "Invalid resource")
    })
    @Audit(auditId = ENTITLEMENT_CREATED)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntitlementResource> registerEntitlement(
        @Valid @RequestBody CreateEntitlementResource request)
    {
        return ResponseEntity.ok(entitlementService.register(request));
    }

    @Operation(
        summary = "Edit entitlement",
        description = "Edit active status for entitlement",
        tags = {"entitlement-api"},
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "entitlement", required = true, description = "Unique entitlement")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entitlement active status is edited"),
        @ApiResponse(responseCode = "404", description = "Entitlement is not found")
    })
    @Audit(auditId = ENTITLEMENT_UPDATED)
    @PutMapping(path = "/{entitlement}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntitlementResource> editEntitlement(
        @PathVariable("entitlement") String entitlement,
        @Valid @RequestBody EditEntitlementResource request) 
    {
        return ResponseEntity.ofNullable(entitlementService.editEntitlement(entitlement, request.active()));
    }

    @Operation(
        summary = "Get all entitlements",
        description = "Retrieve information on all active entitlements",
        tags = {"entitlement-api"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entitlements retrieved")
    })
    @Audit(auditId = ENTITLEMENTS_RETRIEVED)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntitlementsResource> getAllEntitlements() {
        return ResponseEntity.ok(entitlementService.findAllEntitlements());
    }
}
