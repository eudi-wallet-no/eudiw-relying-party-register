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
import no.eudiw.rp.register.exception.NotFoundException;
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
    private static final Set<String> CHECK_INTENDED_USE_PARAMETERS = Set.of(
        "identifier", "serviceIdentifier", "intendedUseIdentifier",
        "credentialFormat", "claimPath", "credentialMeta", "policy"
    );
    private static final Set<String> SUPPORTED_CHECK_INTENDED_USE_PARAMETERS = Set.of(
        "identifier", "serviceIdentifier", "intendedUseIdentifier",
        "credentialFormat", "claimPath", "credentialMeta", "policy"
    );
    private static final Set<String> COLLECTION_PARAMETERS =
        Set.of(
            "limit", "cursor", "identifier", "legalName", "serviceIdentifier", "tradeName", "policy",
            "intendedUseIdentifier", "claimPath", "credentialMeta", "credentialFormat", "entitlement",
            "providedAttestation", "usesIntermediary",
            "isIntermediary"
        );

    private final WrpLookupService lookupService;
    private final WrpJwsSigner jwsSigner;
    private final WrpApiProperties properties;

    @Operation(
        summary = "Get a paginated list of Wallet Relying Parties",
        description = """
             Returns relying parties matching the optional identifier, legalName,
             serviceIdentifier, tradeName, policy, intendedUseIdentifier, claimPath, credentialMeta,
             credentialFormat, entitlement,
             providedAttestation, usesIntermediary, and isIntermediary filters. The signed JWT payload
             contains a data array and a pagination object with next_cursor and has_next_page.
             next_cursor is omitted when there is no next page.""",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Signed JWT containing matching parties in data and pagination metadata",
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
        @RequestParam(name = "limit", defaultValue = "20") int limit,
        @Parameter(
            name = "cursor",
            in = ParameterIn.QUERY,
            description = "Opaque cursor returned in the previous page",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "cursor", required = false) String cursor,
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
            description = "Exact, case-sensitive identifier of a service; only that service is included in the response",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "serviceIdentifier", required = false) String serviceIdentifier,
        @Parameter(
            name = "tradeName",
            in = ParameterIn.QUERY,
            description = "Exact trade name of the WRP or a service",
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
            name = "intendedUseIdentifier",
            in = ParameterIn.QUERY,
            description = "Exact identifier of an intended use registered by a service",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "intendedUseIdentifier", required = false) String intendedUseIdentifier,
        @Parameter(
            name = "claimPath",
            in = ParameterIn.QUERY,
            description = "Case-sensitive exact match against any string element of a registered credential claim path",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "claimPath", required = false) String claimPath,
        @Parameter(
            name = "credentialMeta",
            in = ParameterIn.QUERY,
            description = "Exact, case-sensitive match against a value in credential meta (vct_values or doctype_value)",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "credentialMeta", required = false) String credentialMeta,
        @Parameter(
            name = "credentialFormat",
            in = ParameterIn.QUERY,
            description = """
                Case-sensitive exact match against a registered credential format.
                Percent-encode '+' as '%2B'; an unencoded '+' is decoded as a space.""",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "credentialFormat", required = false) String credentialFormat,
        @Parameter(
            name = "entitlement",
            in = ParameterIn.QUERY,
            description = "Exact URI of a service entitlement or sub-entitlement",
            schema = @Schema(type = "string", format = "uri")
        )
        @RequestParam(name = "entitlement", required = false) String entitlement,
        @Parameter(
            name = "providedAttestation",
            in = ParameterIn.QUERY,
            description = "Exact type of an attestation provided by a service",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "providedAttestation", required = false) String providedAttestation,
        @Parameter(
            name = "usesIntermediary",
            in = ParameterIn.QUERY,
            description = "Exact serviceIdentifier of an intermediary referenced by a service",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "usesIntermediary", required = false) String usesIntermediary,
        @Parameter(
            name = "isIntermediary",
            in = ParameterIn.QUERY,
            description = "Whether a service is registered as an intermediary",
            schema = @Schema(type = "boolean")
        )
        @RequestParam(name = "isIntermediary", required = false) String isIntermediaryString,
        @Parameter(hidden = true)
        @RequestParam MultiValueMap<String, String> queryParameters
    ) {
        if (!COLLECTION_PARAMETERS.containsAll(queryParameters.keySet())) {
            throw new BadRequestException("Unsupported WRP query parameter");
        }
        for (var entry : queryParameters.entrySet()) {
            if (entry.getValue().size() != 1) {
                throw new BadRequestException(entry.getKey() + " must be specified at most once");
            }
        }
        if (isIntermediaryString != null
            && !isIntermediaryString.equals("true")
            && !isIntermediaryString.equals("false")) {
            throw new BadRequestException("isIntermediary must be true or false");
        }
        Boolean isIntermediary = isIntermediaryString == null ? null : Boolean.parseBoolean(isIntermediaryString);

        if ((identifier != null && identifier.isBlank())
            || (legalName != null && legalName.isBlank())
            || (serviceIdentifier != null && serviceIdentifier.isBlank())
            || (tradeName != null && tradeName.isBlank())
            || (policy != null && policy.isBlank())
            || (intendedUseIdentifier != null && intendedUseIdentifier.isBlank())
            || (claimPath != null && claimPath.isBlank())
            || (credentialMeta != null && credentialMeta.isBlank())
            || (credentialFormat != null && credentialFormat.isBlank())
            || (entitlement != null && entitlement.isBlank())
            || (providedAttestation != null && providedAttestation.isBlank())
            || (usesIntermediary != null && usesIntermediary.isBlank())) {
            throw new BadRequestException("WRP filters must not be blank");
        }

        WrpCollectionFilters filters = new WrpCollectionFilters(
                identifier, legalName, serviceIdentifier, tradeName, policy, intendedUseIdentifier, claimPath,
                credentialMeta, credentialFormat, entitlement, providedAttestation, usesIntermediary, isIntermediary);
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
            @ApiResponse(responseCode = "404", description = "The Wallet Relying Party does not exist")
        }
    )
    @GetMapping("/{identifier}")
    public ResponseEntity<String> getByIdentifier(
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
        @PathVariable("identifier") String identifier
    ) {
        String signedResult = lookupService.findByIdentifier(identifier)
            .map(jwsSigner::sign)
            .orElseThrow(() -> new NotFoundException("Wallet Relying Party not found"));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/jwt"))
            .header(JWK_URL_HEADER, properties.jwksUrl())
            .body(signedResult);
    }

    @Operation(
        summary = "Get a Wallet Relying Party service by identifiers",
        description = """
            Returns the parent Wallet Relying Party with its services array isolated to the exact
            serviceIdentifier.""",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Signed isolated Wallet Relying Party",
                headers = @Header(
                    name = JWK_URL_HEADER,
                    schema = @Schema(type = "string", format = "uri")
                ),
                content = @Content(
                    mediaType = "application/jwt",
                    schema = @Schema(type = "string", format = "JWS")
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "The Wallet Relying Party or service does not exist"
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Registry contains duplicate service identifiers within the WRP"
            )
        }
    )
    @GetMapping("/{identifier}/services/{serviceIdentifier}")
    public ResponseEntity<String> getByIdentifierAndServiceIdentifier(
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
        @PathVariable("identifier") String identifier,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
        @PathVariable("serviceIdentifier") String serviceIdentifier
    ) {
        String signedResult = lookupService.findByIdentifierAndServiceIdentifier(identifier, serviceIdentifier)
            .map(jwsSigner::sign)
            .orElseThrow(() -> new NotFoundException("Wallet Relying Party service not found"));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/jwt"))
            .header(JWK_URL_HEADER, properties.jwksUrl())
            .body(signedResult);
    }

    @Operation(
        summary = "Check whether an intended use is currently valid",
        description = """
            Returns a signed boolean in the data.isRegistered claim. The credentialFormat, claimPath,
            credentialMeta, and policy filters apply to the same IntendedUse whose validity is checked.
            policy matches only the IntendedUse privacy-policy list, not the parent WRP policy list.
            """,
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Signed result; no matching IntendedUse returns data.isRegistered=false, while a matching currently valid IntendedUse returns true",
                headers = @Header(
                    name = JWK_URL_HEADER,
                    schema = @Schema(type = "string", format = "uri")
                ),
                content = @Content(
                    mediaType = "application/jwt",
                    schema = @Schema(type = "string", format = "JWS")
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Missing, invalid, repeated, or unsupported query parameter"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "The supplied WRP identifier does not exist"
            )
        }
    )
    @GetMapping("/check-intended-use")
    public ResponseEntity<String> checkIntendedUse(
        @Parameter(
            name = "identifier",
            in = ParameterIn.QUERY,
            description = "Exact WRP identifier matching orgno or an additional Identifier.identifier",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "identifier", required = false) String identifier,
        @Parameter(
            name = "serviceIdentifier",
            in = ParameterIn.QUERY,
            description = "Exact, case-sensitive service identifier",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "serviceIdentifier", required = false) String serviceIdentifier,
        @Parameter(
            name = "intendedUseIdentifier",
            in = ParameterIn.QUERY,
            description = "Exact, case-sensitive identifier of a registered intended use",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "intendedUseIdentifier", required = false) String intendedUseIdentifier,
        @Parameter(
            name = "credentialFormat",
            in = ParameterIn.QUERY,
            description = "Exact, case-sensitive credential format registered for the IntendedUse",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "credentialFormat", required = false) String credentialFormat,
        @Parameter(
            name = "claimPath",
            in = ParameterIn.QUERY,
            description = "Exact, case-sensitive match against any string element of a credential claim path for the IntendedUse",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "claimPath", required = false) String claimPath,
        @Parameter(
            name = "credentialMeta",
            in = ParameterIn.QUERY,
            description = "Exact, case-sensitive match against a vct_values or doctype value in credential metadata for the IntendedUse",
            schema = @Schema(type = "string")
        )
        @RequestParam(name = "credentialMeta", required = false) String credentialMeta,
        @Parameter(
            name = "policy",
            in = ParameterIn.QUERY,
            description = "Exact URI in the privacy-policy list of the IntendedUse; parent WRP policies do not match",
            schema = @Schema(type = "string", format = "uri")
        )
        @RequestParam(name = "policy", required = false) String policy,
        @Parameter(hidden = true)
        @RequestParam MultiValueMap<String, String> queryParameters
    ) {
        if (!CHECK_INTENDED_USE_PARAMETERS.containsAll(queryParameters.keySet())) {
            throw new BadRequestException("Unsupported WRP check query parameter");
        }
        for (var entry : queryParameters.entrySet()) {
            if (entry.getValue().size() != 1) {
                throw new BadRequestException(entry.getKey() + " must be specified at most once");
            }
        }
        if (queryParameters.isEmpty()) {
            throw new BadRequestException("At least one WRP check filter must be provided");
        }
        if (!SUPPORTED_CHECK_INTENDED_USE_PARAMETERS.containsAll(queryParameters.keySet())) {
            throw new BadRequestException("This WRP check filter is not supported yet");
        }
        if (identifier != null && identifier.isBlank()) {
            throw new BadRequestException("identifier must not be blank");
        }
        if (serviceIdentifier != null && serviceIdentifier.isBlank()) {
            throw new BadRequestException("serviceIdentifier must not be blank");
        }
        if (intendedUseIdentifier != null && intendedUseIdentifier.isBlank()) {
            throw new BadRequestException("intendedUseIdentifier must not be blank");
        }
        if (credentialFormat != null && credentialFormat.isBlank()) {
            throw new BadRequestException("credentialFormat must not be blank");
        }
        if (claimPath != null && claimPath.isBlank()) {
            throw new BadRequestException("claimPath must not be blank");
        }
        if (credentialMeta != null && credentialMeta.isBlank()) {
            throw new BadRequestException("credentialMeta must not be blank");
        }
        if (policy != null && policy.isBlank()) {
            throw new BadRequestException("policy must not be blank");
        }

        String signedResult = jwsSigner.signIntendedUseCheck(
            lookupService.isIntendedUseValid(
                identifier,
                serviceIdentifier,
                intendedUseIdentifier,
                credentialFormat,
                claimPath,
                credentialMeta,
                policy
            ));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/jwt"))
            .header(JWK_URL_HEADER, properties.jwksUrl())
            .body(signedResult);
    }
}
