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
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.service.RelyingPartyService;
import no.idporten.logging.audit.Audit;
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
        private static final String RELYING_PARTY_CREATED = "RELYING-PARTY-CREATED";
        private static final String RELYING_PARTY_UPDATED = "RELYING-PARTY-UPDATED";
        private static final String RELYING_PARTY_DELETED = "RELYING-PARTY-DELETED";
        private static final String RELYING_PARTY_RETRIEVED = "RELYING-PARTY-RETRIEVED";
        private static final String RELYING_PARTIES_SEARCHED = "RELYING-PARTIES-SEARCHED";
        private static final String RELYING_PARTIES_RETRIEVED = "RELYING-PARTIES-RETRIEVED";

        private final RelyingPartyService relyingPartyService;

        @Autowired
        public RelyingPartiesController(RelyingPartyService relyingPartyService) {
                this.relyingPartyService = relyingPartyService;
        }

        @Operation(
                summary = "Create Relying Party",
                description = "Create Relying Party",
                tags = {"relying-parties-api"})
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Relying party is created"),
                @ApiResponse(responseCode = "400", description = "Invalid resource")
        })
        @Audit(auditId = RELYING_PARTY_CREATED)
        @PostMapping(path = "v1/rp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartyResource> createRelyingParty(@Valid @RequestBody CreateRelyingPartyResource request) {
                return ResponseEntity.ok(relyingPartyService.createRelyingParty(request));
        }

        @Operation(
                summary = "Edit Relying Party",
                description = "Edit Relying Party",
                tags = {"relying-parties-api"})
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Relying party is edited"),
                @ApiResponse(responseCode = "404", description = "Relying party is not found")
        })
        @Audit(auditId = RELYING_PARTY_UPDATED)
        @PutMapping(path = "v1/rp/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartyResource> editRelyingParty(
            @PathVariable("id") UUID id, @Valid @RequestBody EditRelyingPartyResource request) {
                return ResponseEntity.ofNullable(relyingPartyService.updateRelyingParty(id, request));
        }

        @Operation(
                summary = "Delete Relying Party",
                description = "Delete Relying Party",
                tags = {"relying-parties-api"})
        @ApiResponses(value = {
                @ApiResponse(responseCode = "204", description = "Relying party is deleted"),
                @ApiResponse(responseCode = "404", description = "Relying party is not found")
        })
        @Audit(auditId = RELYING_PARTY_DELETED)
        @DeleteMapping(path = "v1/rp/{id}")
        public ResponseEntity<Void> deleteRelyingParty(@PathVariable("id") UUID id) {
                relyingPartyService.deleteRelyingParty(id);
                return ResponseEntity.noContent().build();
        }

        @Operation(
                summary = "Get Relying Party",
                description = "Get Relying Party",
                tags = {"relying-parties-api"},
                parameters = {
                        @Parameter(in = ParameterIn.PATH, name = "id", required = true, description = "Unik id")
                })
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Relying party is returned"),
                @ApiResponse(responseCode = "404", description = "Relying party is not found")
        })
        @Audit(auditId = RELYING_PARTY_RETRIEVED)
        @GetMapping(path = "v1/rp/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartyResource> getRelyingParty(@PathVariable("id") UUID id) {
                return ResponseEntity.ok(relyingPartyService.findRelyingParty(id));
        }

        @Operation(
                summary = "Search",
                description = "Search relying parties by org_nr and public_sector",
                tags = {"relying-parties-api"},
                parameters = {
                        @Parameter(in = ParameterIn.PATH,
                                   name = "organisajonsnummer",
                                   description = "Unikt organisasjonsnummer",
                                   required = true)
                })
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200",
                             description = "Possibly empty relying parties search result is returned")
        })
        @Audit(auditId = RELYING_PARTIES_SEARCHED)
        @PostMapping(path = "v1/rp/search", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartiesResource> searchRelyingParty(
            @Valid @RequestBody SearchRelyingPartyResource request) {
                return ResponseEntity.ok(relyingPartyService.searchRelyingParties(request));
        }

        @Operation(
                summary = "Get all Relying Parties",
                description = "Get all Relying Parties",
                tags = {"relying-parties-api"})
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Relying parties are retrieved")
        })
        @Audit(auditId = RELYING_PARTIES_RETRIEVED)
        @GetMapping(path = "v1/rp", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartiesResource> getAllRelyingParties() {
                return ResponseEntity.ok(relyingPartyService.findAllRelyingParties());
        }
}
