package no.eudiw.rp.register.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyAccessCertificatesResource;
import no.eudiw.rp.register.data.service.accesscertificates.RelyingPartyCertificatesService;
import no.idporten.logging.audit.Audit;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static no.eudiw.rp.register.api.RegisterServiceApiSwaggerExamples.*;

import java.util.UUID;


@Tag(name = "relying-parties-certs-api",
    description = "Relying parties access certificates service API")
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
public class RelyingPartyAccessCertificatesController {

    private static final String RELYING_PARTY_NEW_CERTIFICATE_REQUESTED =
        "RELYING-PARTY-NEW-CERTIFICATE-REQUESTED";
    private static final String RELYING_PARTY_CERTIFICATE_RETRIEVED =
        "RELYING-PARTY-CERTIFICATE-RETRIEVED";

    private final RelyingPartyCertificatesService certificatesService;

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
    @Audit(auditId = RELYING_PARTY_NEW_CERTIFICATE_REQUESTED)
    @PostMapping(path = "/{relying-party-id}/certs/access",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyAccessCertificateResource> requestAccessCertificate(
        @PathVariable("relying-party-id") @Valid UUID id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Relying Party CSR resource",
            content = @Content(examples = @ExampleObject(value = CSR_RESOURCE_EXAMPLE)))
        @Valid @RequestBody RelyingPartyCsrResource csrResource) {
        return ResponseEntity.ok(certificatesService.requestCertificateForRelyingParty(id, csrResource));
    }

    @Operation(
        summary = "Get access certificates for relying party",
        description = "Get all access certificates for relying party by ID",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Access certificates returned",
                     content = @Content(examples = @ExampleObject(value = CERTIFICATES_RESOURCE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Relying party not found")
    })
    @Audit(auditId = RELYING_PARTY_CERTIFICATE_RETRIEVED)
    @GetMapping(path = "/{relying-party-id}/certs",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyAccessCertificatesResource> getAccessCertificates(
        @PathVariable("relying-party-id") @Valid UUID relyingPartyId) {
        return ResponseEntity.ok(
            certificatesService.getAccessCertificatesForRelyingParty(relyingPartyId));
    }

    @Operation(
        summary = "Get specific access certificate",
        description = "Get access certificate by its ID and ID of the holding relying party",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Access certificates returned",
                     content = @Content(examples = @ExampleObject(value = CERTIFICATE_RESOURCE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Access certificate not found")
    })
    @Audit(auditId = RELYING_PARTY_CERTIFICATE_RETRIEVED)
    @GetMapping(path = "/{relying-party-id}/certs/{certificate-id}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyAccessCertificateResource> getAccessCertificate(
        @PathVariable("relying-party-id") @Valid UUID relyingPartyId,
        @PathVariable("certificate-id") @Valid UUID certificateId) {
        return ResponseEntity.ok(
            certificatesService.getAccessCertificate(
                certificateId, relyingPartyId));
    }
}
