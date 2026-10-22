package no.eudiw.rp.register.api.v2.wrp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "wrp-api", description = "Public Wallet Relying Party API")
@RestController
@RequestMapping("/wrp")
@RequiredArgsConstructor
public class WrpEndpoint {
    private static final String JWK_URL_HEADER = "x-jku-url";

    private final WrpLookupService lookupService;
    private final WrpJwsSigner jwsSigner;
    private final WrpApiProperties properties;

    @Operation(
        summary = "Get a Wallet Relying Party by identifier",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Signed Wallet Relying Party",
                headers = @Header(
                    name = JWK_URL_HEADER,
                    schema = @Schema(type = "string", format = "uri")
                ),
                content = @Content(
                    mediaType = "application/jwt",
                    schema = @Schema(type = "string", format = "JWS")
                )
            ),
            @ApiResponse(responseCode = "404", description = "Wallet Relying Party not found")
        }
    )
    @GetMapping("/{identifier}")
    public ResponseEntity<String> getByIdentifier(
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
        @PathVariable("identifier") String identifier
    ) {
        return lookupService.findByIdentifier(identifier)
            .map(data -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/jwt"))
                .header(JWK_URL_HEADER, properties.jwksUrl())
                .body(jwsSigner.sign(data)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
