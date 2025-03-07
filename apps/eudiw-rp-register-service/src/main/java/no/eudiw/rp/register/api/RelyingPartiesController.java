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
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.service.RelyingPartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
                @ApiResponse(responseCode = "200", description = "Relying party is created")
        })
        @PostMapping(path = "v1/rp/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartyResponse> createRelyingParty(@Valid @RequestBody CreateRelyingPartyResource request) {
                RelyingPartyResponse response = new RelyingPartyResponse(
                        relyingPartyService.createRelyingParty(request)
                );

                return ResponseEntity.ok(response);
        }

        @Operation(
                summary = "Edit Relying Party",
                description = "Edit Relying Party",
                tags = {"relying-parties-api"})
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Relying party is edited"),
                @ApiResponse(responseCode = "404", description = "Relying party is not found")
        })
        @PatchMapping(path = "v1/rp/edit/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartyResponse> editRelyingParty(@PathVariable("id") UUID id, @Valid @RequestBody EditRelyingPartyResource request) {
                RelyingPartyResponse response = new RelyingPartyResponse(
                        relyingPartyService.updateRelyingParty(
                                id,
                                request
                        )
                );
                return ResponseEntity.ok(response);
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
        @GetMapping(path = "v1/rp/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartyResponse> getRelyingParty(@PathVariable("id") UUID id) {
                RelyingPartyResponse response = new RelyingPartyResponse(relyingPartyService.findRelyingParty(id));
                return ResponseEntity.ok(response);
        }

        @Operation(
                summary = "Get all Relying Parties",
                description = "Get all Relying Parties",
                tags = {"relying-parties-api"})
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Relying parties is retrieved")
        })
        @GetMapping(path = "v1/rp/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<RelyingPartiesResponse> getAllRelyingParties() {
                List<RelyingParty> relyingParties = relyingPartyService.findAllRelyingParties();
                List<RelyingPartyResponse> relyingPartiesResponse = relyingParties
                        .stream()
                        .map(RelyingPartyResponse::new)
                        .toList();
                return ResponseEntity.ok(new RelyingPartiesResponse(relyingPartiesResponse));
        }
}
