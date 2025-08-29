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
import no.eudiw.rp.register.data.service.RelyingPartyService;
import no.idporten.logging.audit.Audit;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "relying-parties-api", description = "Relying Parties Service Api")
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
@RequestMapping("/v1/rp")
@RequiredArgsConstructor
public class RelyingPartiesController {

    private static final String RELYING_PARTY_CREATED = "RELYING-PARTY-CREATED";
    private static final String RELYING_PARTY_UPDATED = "RELYING-PARTY-UPDATED";
    private static final String RELYING_PARTY_DELETED = "RELYING-PARTY-DELETED";
    private static final String RELYING_PARTY_RETRIEVED = "RELYING-PARTY-RETRIEVED";
    private static final String RELYING_PARTIES_SEARCHED = "RELYING-PARTIES-SEARCHED";
    private static final String RELYING_PARTIES_RETRIEVED = "RELYING-PARTIES-RETRIEVED";

    private final RelyingPartyService relyingPartyService;

    @Operation(
        summary = "Create relying party",
        description = "Register a new relying party",
        tags = {"relying-parties-api"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relying party is created"),
        @ApiResponse(responseCode = "400", description = "Invalid resource")
    })
    @Audit(auditId = RELYING_PARTY_CREATED)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyResource> createRelyingParty(
        @Valid @RequestBody CreateRelyingPartyResource request) {
        return ResponseEntity.ok(relyingPartyService.createRelyingParty(request));
    }

    @Operation(
        summary = "Edit relying party",
        description = "Edit information for a given relying party",
        tags = {"relying-parties-api"},
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", required = true, description = "Unique ID")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relying party is edited"),
        @ApiResponse(responseCode = "404", description = "Relying party is not found")
    })
    @Audit(auditId = RELYING_PARTY_UPDATED)
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyResource> editRelyingParty(
        @PathVariable("id") UUID id,
        @Valid @RequestBody EditRelyingPartyResource request) {
        return ResponseEntity.ofNullable(relyingPartyService.updateRelyingParty(id, request));
    }

    @Operation(
        summary = "Delete relying party",
        description = "Delete a relying party by its ID",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", required = true, description = "Unique ID")
        },
        tags = {"relying-parties-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Relying party is deleted"),
        @ApiResponse(responseCode = "404", description = "Relying party is not found")
    })
    @Audit(auditId = RELYING_PARTY_DELETED)
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteRelyingParty(@PathVariable("id") UUID id) {
        relyingPartyService.deleteRelyingParty(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Get relying party",
        description = "Get information on relying party by its ID",
        tags = {"relying-parties-api"},
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", required = true, description = "Unik id")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relying party is returned"),
        @ApiResponse(responseCode = "404", description = "Relying party is not found")
    })
    @Audit(auditId = RELYING_PARTY_RETRIEVED)
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyResource> getRelyingParty(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(relyingPartyService.findRelyingParty(id));
    }

    @Operation(
        summary = "Search for relying parties",
        description = "Free-text search for relying parties by organization number or name",
        tags = {"relying-parties-api"})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Possibly empty relying parties search result is returned")
    })
    @Audit(auditId = RELYING_PARTIES_SEARCHED)
    @PostMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<RelyingPartyResource>> searchRelyingParty(
        @Valid @RequestBody SearchRelyingPartyResource request) {
        return ResponseEntity.ok(relyingPartyService.searchRelyingParties(request));
    }
}
