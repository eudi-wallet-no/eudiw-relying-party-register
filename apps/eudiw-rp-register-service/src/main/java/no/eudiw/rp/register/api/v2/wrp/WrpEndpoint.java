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
import no.eudiw.rp.register.config.WrpApiProperties;
import no.eudiw.rp.register.exception.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Tag(name = "wrp-api", description = "Public Wallet Relying Party API")
@RestController
@RequestMapping("/wrp")
@RequiredArgsConstructor
public class WrpEndpoint {
    private static final String JWK_URL_HEADER = "x-jku-url";
    private static final Set<String> COLLECTION_PARAMETERS =
        Set.of(
            "limit", "cursor", "identifier", "legalName", "serviceIdentifier", "tradeName", "policy",
            "entitlement", "providedAttestation", "usesIntermediary"
        );

    private final WrpLookupService lookupService;
    private final WrpJwsSigner jwsSigner;
    private final WrpApiProperties properties;

    @Operation(
        summary = "Get a paginated list of Wallet Relying Parties",
        description = """
             Returns active relying parties matching the optional identifier, legalName,
             serviceIdentifier, tradeName, policy, entitlement, providedAttestation, and
             usesIntermediary filters.""",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Signed list of Wallet Relying Parties",
                headers = @Header(
                    name = JWK_URL_HEADER,
                    schema = @Schema(type = "string", format = "uri")
                ),
                content = @Content(
                    mediaType = "application/jwt",
                    schema = @Schema(type = "string", format = "JWS")
                )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid limit, cursor, filter, or query parameter")
        }
    )
    @GetMapping
    public ResponseEntity<String> getAll(
        @Parameter(
            name = "limit",
            in = ParameterIn.QUERY,
            description = "Maximum number of parties to return (1-100)",
            schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")
        )
        @RequestParam(defaultValue = "20") int limit,
        @Parameter(
            name = "cursor",
            in = ParameterIn.QUERY,
            description = "Opaque cursor returned in the previous page",
            schema = @Schema(type = "string")
        )
        @RequestParam(required = false) String cursor,
        @Parameter(
            name = "identifier",
            in = ParameterIn.QUERY,
            description = "Exact organization number or registered identifier",
            schema = @Schema(type = "string")
        )
        @RequestParam(required = false) String identifier,
        @Parameter(
            name = "legalName",
            in = ParameterIn.QUERY,
            description = "Exact registered legal name",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "legalName", required = false) String legalName,
        @Parameter(
            name = "serviceIdentifier",
            in = ParameterIn.QUERY,
            description = "Exact identifier of an active service",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "serviceIdentifier", required = false) String serviceIdentifier,
        @Parameter(
            name = "tradeName",
            in = ParameterIn.QUERY,
            description = "Exact trade name of the WRP or an active service",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "tradeName", required = false) String tradeName,
        @Parameter(
            name = "policy",
            in = ParameterIn.QUERY,
            description = "Exact URI of a WRP policy or intended-use privacy policy",
            schema = @Schema(type = "string", format = "uri")
        )
        @RequestParam(name = "policy", required = false) String policy,
        @Parameter(
            name = "entitlement",
            in = ParameterIn.QUERY,
            description = "Exact URI of an active service entitlement or sub-entitlement",
            schema = @Schema(type = "string", format = "uri")
        )
        @RequestParam(name = "entitlement", required = false) String entitlement,
        @Parameter(
            name = "providedAttestation",
            in = ParameterIn.QUERY,
            description = "Exact type of an attestation provided by an active service",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "providedAttestation", required = false) String providedAttestation,
        @Parameter(
            name = "usesIntermediary",
            in = ParameterIn.QUERY,
            description = "Exact serviceIdentifier of an intermediary referenced by an active service",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "usesIntermediary", required = false) String usesIntermediary,
        @Parameter(hidden = true)
        @RequestParam MultiValueMap<String, String> queryParameters
    ) {
        if (!COLLECTION_PARAMETERS.containsAll(queryParameters.keySet())) {
            throw new BadRequestException("Unsupported WRP query parameter");
        }
        if ((identifier != null && identifier.isBlank())
            || (legalName != null && legalName.isBlank())
            || (serviceIdentifier != null && serviceIdentifier.isBlank())
            || (tradeName != null && tradeName.isBlank())
            || (policy != null && policy.isBlank())
            || (entitlement != null && entitlement.isBlank())
            || (providedAttestation != null && providedAttestation.isBlank())
            || (usesIntermediary != null && usesIntermediary.isBlank())) {
            throw new BadRequestException("WRP filters must not be blank");
        }

        WrpCollectionFilters filters =
            new WrpCollectionFilters(
                identifier, legalName, serviceIdentifier, tradeName, policy, entitlement,
                providedAttestation, usesIntermediary);
        WrpLookupService.CollectionPage page = lookupService.findAll(limit, cursor, filters);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/jwt"))
            .header(JWK_URL_HEADER, properties.jwksUrl())
            .body(jwsSigner.signCollection(page.data(), page.nextCursor(), page.hasNextPage()));
    }

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
