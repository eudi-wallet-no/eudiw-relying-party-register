package no.eudiw.rp.register.api.v1.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v1.openapi.V1ApiExamples;
import no.eudiw.rp.register.api.v1.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCertificatesResource;
import no.eudiw.rp.register.api.v1.V1ApiService;
import no.idporten.logging.audit.Audit;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static no.eudiw.rp.register.api.v1.openapi.V1ApiExamples.*;

import java.util.UUID;


@Tag(name = "relying-parties-certs-api",
    description = "Relying parties certificates service API")
@ApiResponses({
    @ApiResponse(responseCode = "400",
        description = "Invalid request",
        content = @Content(examples = @ExampleObject(
            description = "Error response", value = V1ApiExamples.BAD_REQUEST_ERROR_EXAMPLE)
        )),
    @ApiResponse(responseCode = "404",
        description = "Not found",
        content = @Content(examples = @ExampleObject(
            description = "Error response", value = V1ApiExamples.NOT_FOUND_ERROR_EXAMPLE)
        )),
    @ApiResponse(responseCode = "500",
        description = "Server error",
        content = @Content(examples = @ExampleObject(
            description = "Error response", value = V1ApiExamples.SERVER_ERROR_EXAMPLE)
        ))
})
@Validated
@RestController
@RequestMapping("/v1/rp")
@RequiredArgsConstructor
public class RelyingPartyCertificateEndpoint {

    private static final String RELYING_PARTY_NEW_ACCESS_CERTIFICATE_REQUESTED =
        "RELYING-PARTY-NEW-CERTIFICATE-REQUESTED";
    private static final String ISSUER_NEW_CERTIFICATE_REQUESTED =
            "ISSUER-NEW-CERTIFICATE-REQUESTED";
    private static final String RELYING_PARTY_ACCESS_CERTIFICATE_RETRIEVED =
        "RELYING-PARTY-ACCESS-CERTIFICATE-RETRIEVED";
    private static final String ISSUER_CERTIFICATE_RETRIEVED =
        "ISSUER-CERTIFICATE-RETRIEVED";
    private static final String RELYING_PARTY_ACCESS_CERTIFICATE_REVOKED =
            "RELYING-PARTY-ACCESS-CERTIFICATE-REVOKED";
    private static final String ISSUER_CERTIFICATE_REVOKED =
            "ISSUER-CERTIFICATE-REVOKED";

    private final V1ApiService v1Api;

    @Operation(
        summary = "Register access certificate",
        description = "Register access certificate for relying party by ID",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "CSR accepted and access certificate is returned",
                     content = @Content(examples = @ExampleObject(value = CERTIFICATE_RESOURCE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Certificate requested for unknown RP"),
        @ApiResponse(responseCode = "400", description = "CSR is rejected")
    })
    @Audit(auditId = RELYING_PARTY_NEW_ACCESS_CERTIFICATE_REQUESTED)
    @PostMapping(path = "/{relying-party-id}/certs/access",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyCertificateResource> requestAccessCertificate(
        @PathVariable("relying-party-id") @Valid UUID id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Relying Party CSR resource",
            content = @Content(examples = @ExampleObject(value = CSR_RESOURCE_EXAMPLE)))
        @Valid @RequestBody RelyingPartyCsrResource csrResource) {
        return ResponseEntity.ok(v1Api.requestAccessCertificateForRelyingParty(id, csrResource));
    }

    @Operation(
        summary = "Register issuer certificate",
        description = "Register issuer certificate for relying party by ID",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "CSR accepted and issuer certificate is returned",
            content = @Content(examples = @ExampleObject(value = CERTIFICATE_RESOURCE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Certificate requested for unknown RP"),
        @ApiResponse(responseCode = "400", description = "CSR is rejected")
    })
    @Audit(auditId = ISSUER_NEW_CERTIFICATE_REQUESTED)
    @PostMapping(path = "/{relying-party-id}/certs/issuer",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyCertificateResource> requestIssuerCertificate(
        @PathVariable("relying-party-id") @Valid UUID relyingPartyId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Issuer CSR resource",
            content = @Content(examples = @ExampleObject(value = CSR_RESOURCE_EXAMPLE)))
        @Valid @RequestBody IssuerCsrResource csrResource
    ) {
        return ResponseEntity.ok(v1Api.requestIssuerCertificate(relyingPartyId, csrResource));
    }

    @Operation(
        summary = "Get issuer certificates for relying party",
        description = "Get all issuer certificates for relying party by ID",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "Certificates returned",
            content = @Content(examples = @ExampleObject(value = ISSUER_CERTS_ENTITLEMENTS_RESOURCE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Relying party not found")
    })
    @Audit(auditId = ISSUER_CERTIFICATE_RETRIEVED)
    @GetMapping(path = "/{relying-party-id}/certs/issuer",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyCertificatesResource> getIssuerCertificates(
        @PathVariable("relying-party-id") @Valid UUID relyingPartyId) {
        return ResponseEntity.ok(
            v1Api.getAllIssuerCertificatesFromRelyingParty(relyingPartyId));
    }

    @Operation(
        summary = "Get access-certificates for relying party",
        description = "Get all access-certificates for relying party by ID",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Certificates returned",
                     content = @Content(examples = @ExampleObject(value = CERTIFICATES_RESOURCE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Relying party not found")
    })
    @Audit(auditId = RELYING_PARTY_ACCESS_CERTIFICATE_RETRIEVED)
    @GetMapping(path = "/{relying-party-id}/certs/access",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyCertificatesResource> getAccessCertificates(
        @PathVariable("relying-party-id") @Valid UUID relyingPartyId) {
        return ResponseEntity.ok(
            v1Api.getCertificatesForRelyingParty(relyingPartyId));
    }

    @Operation(
        summary = "Get specific access-certificate",
        description = "Get access-certificate by its ID and ID of the holding relying party",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Certificates returned",
                     content = @Content(examples = @ExampleObject(value = CERTIFICATE_RESOURCE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    @Audit(auditId = RELYING_PARTY_ACCESS_CERTIFICATE_RETRIEVED)
    @GetMapping(path = "/{relying-party-id}/certs/access/{certificate-id}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyCertificateResource> getAccessCertificate(
        @PathVariable("relying-party-id") @Valid UUID relyingPartyId,
        @PathVariable("certificate-id") @Valid UUID certificateId) {
        return ResponseEntity.ok(
            v1Api.getCertificate(
                certificateId, relyingPartyId));
    }

    @Operation(
        summary = "Get specific issuer-certificate",
        description = "Get issuer-certificate by its ID",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "Certificates returned",
            content = @Content(examples = @ExampleObject(value = CERTIFICATE_RESOURCE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    @Audit(auditId = ISSUER_CERTIFICATE_RETRIEVED)
    @GetMapping(path = "/{relying-party-id}/certs/issuer/{certificate-id}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyCertificateResource> getIssuerCertificate(
        @PathVariable("relying-party-id") @Valid UUID relyingPartyId,
        @PathVariable("certificate-id") @Valid UUID certificateId) {
        return ResponseEntity.ok(v1Api.getIssuerCertificate(certificateId, relyingPartyId));
    }

    @Operation(
            summary = "Revoke specific access-certificate",
            description = "Revoke access-certificate by its relying party id and its certificate id",
            tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "Certificate revoked"),
            @ApiResponse(responseCode = "404", description = "Certificate not found")
    })

    @Audit(auditId = RELYING_PARTY_ACCESS_CERTIFICATE_REVOKED)
    @PatchMapping(path = "/{relying-party-id}/certs/access/{certificate-id}/revoke")
    public ResponseEntity<Void> revokeAccessCertificate(
            @PathVariable("relying-party-id") @Valid UUID relyingPartyId,
            @PathVariable("certificate-id") @Valid UUID certificateId) {
        v1Api.revokeAccessCertificate(certificateId, relyingPartyId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Revoke specific issuer-certificate",
            description = "Revoke issuer-certificate by its certificate ID",
            tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "Certificate revoked"),
            @ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    @Audit(auditId = ISSUER_CERTIFICATE_REVOKED)
    @PatchMapping(path = "/{relying-party-id}/certs/issuer/{certificate-id}/revoke")
    public ResponseEntity<Void> revokeIssuerCertificate(
            @PathVariable("relying-party-id") @Valid UUID relyingPartyId,
            @PathVariable("certificate-id") @Valid UUID certificateId) {
        v1Api.revokeIssuerCertificate(certificateId, relyingPartyId);
        return ResponseEntity.noContent().build();
    }

}
