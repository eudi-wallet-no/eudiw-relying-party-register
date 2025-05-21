package no.idporten.eudiw.rp.admin.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "relying-parties-api", description = "Relying Parties Service Api")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(examples = {
                @ExampleObject(description = "Error response", value = RelyingPartiesController.errorResponseExample)
        })),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content(examples = {
                @ExampleObject(description = "Error response", value = RelyingPartiesController.errorResponseExample)
        }))
})
@Validated
@RestController
public class RelyingPartiesController {

    public static final String errorResponseExample = "{\"error\": \"error code\", \"error_description\": \"description of the error\"}";

    private final RelyingPartiesService relyingPartiesService;

    @Autowired
    public RelyingPartiesController(RelyingPartiesService relyingPartiesService) {
        this.relyingPartiesService = relyingPartiesService;
    }

    @Operation(
            summary = "Create relying party",
            description = "Register a new relying party",
            tags = {"relying-parties-api"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relying party is created"),
            @ApiResponse(responseCode = "400", description = "Invalid resource")
    })
    @PostMapping(path = "v1/rp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyResource> createRelyingParty(@Valid @RequestBody CreateRelyingPartyResource request) {
        return ResponseEntity.ok(relyingPartiesService.create(request));
    }

    @Operation(
            summary = "Edit relying party",
            description = "Edit information for a given relying party",
            tags = {"relying-parties-api"},
            parameters = {
                    @Parameter(in = ParameterIn.PATH, name = "id", required = true, description = "Unique ID")
            })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relying party is edited"),
            @ApiResponse(responseCode = "404", description = "Relying party is not found")
    })
    @PutMapping(path = "v1/rp/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyResource> editRelyingParty(
            @PathVariable("id") UUID id, @Valid @RequestBody EditRelyingPartyResource request) {
        return ResponseEntity.ofNullable(relyingPartiesService.edit(id, request));
    }

    @Operation(
            summary = "Delete relying party",
            description = "Delete a relying party by its ID",
            parameters = {
                    @Parameter(in = ParameterIn.PATH, name = "id", required = true, description = "Unique ID")
            },
            tags = {"relying-parties-api"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Relying party is deleted"),
            @ApiResponse(responseCode = "404", description = "Relying party is not found")
    })
    @DeleteMapping(path = "v1/rp/{id}")
    public ResponseEntity<Void> deleteRelyingParty(@PathVariable("id") UUID id) {
        relyingPartiesService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get relying party",
            description = "Get information on relying party by its ID",
            tags = {"relying-parties-api"},
            parameters = {
                    @Parameter(in = ParameterIn.PATH, name = "id", required = true, description = "Unik id")
            })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relying party is returned"),
            @ApiResponse(responseCode = "404", description = "Relying party is not found")
    })
    @GetMapping(path = "v1/rp/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyResource> getRelyingParty(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(relyingPartiesService.get(id));
    }

    @Operation(
            summary = "Search for relying parties",
            description = "Free-text search for relying parties by organization number or name",
            tags = {"relying-parties-api"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Possibly empty relying parties search result is returned")
    })
    @PostMapping(path = "v1/rp/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartiesResource> searchRelyingParty(
            @Valid @RequestBody SearchRelyingPartyResource request) {
        return ResponseEntity.ok(relyingPartiesService.search(request));
    }

    @Operation(
            summary = "Get all relying parties",
            description = "Retrieve information on all active and inactive relying parties",
            tags = {"relying-parties-api"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relying parties are retrieved")
    })
    @GetMapping(path = "v1/rp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartiesResource> getAllRelyingParties() {
        return ResponseEntity.ok(relyingPartiesService.getAll());
    }
}
