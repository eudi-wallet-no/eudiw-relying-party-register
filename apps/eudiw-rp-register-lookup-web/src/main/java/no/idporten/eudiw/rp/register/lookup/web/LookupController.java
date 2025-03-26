package no.idporten.eudiw.rp.register.lookup.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.idporten.eudiw.rp.register.lookup.web.resource.TempObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "lookup-relying-parties-api", description = "Lookup Relying Parties Service Api")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(examples = {
                @ExampleObject(description = "Error response", value = LookupController.errorResponseExample)
        })),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content(examples = {
                @ExampleObject(description = "Error response", value = LookupController.errorResponseExample)
        }))
})
@RestController
public class LookupController {

    public static final String errorResponseExample = "{\"error\": \"error code\", \"error_description\": \"description of the error\"}";

    @Operation(
            summary = "Lookup Relying Parties",
            description = "Lookup Relying Parties",
            tags = {"relying-parties-api"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Looks up all relying parties")
    })
    @GetMapping(path = "v1/rp/lookup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TempObject>> lookupRelyingParties() {
        return ResponseEntity.ok(
                List.of(
                        new TempObject("1234", "1234", "1234"),
                        new TempObject("5678", "5678", "5678"),
                        new TempObject("789", "789", "789")
                )
        );
    }


}
