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

import java.util.UUID;


@Tag(name = "relying-parties-certs-api",
    description = "Relying parties access certificates service API")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(examples = {
        @ExampleObject(description = "Error response", value = RelyingPartiesController.errorResponseExample)
    })),
    @ApiResponse(responseCode = "500", description = "Server error", content = @Content(examples = {
        @ExampleObject(description = "Error response", value = RelyingPartiesController.errorResponseExample)
    }))
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
        @ApiResponse(responseCode = "200", description = "CSR accepted and access certificate is returned"),
        @ApiResponse(responseCode = "404", description = "CSR registered for unknown RP"),
        @ApiResponse(responseCode = "400", description = "CSR is rejected")
    })
    @Audit(auditId = RELYING_PARTY_NEW_CERTIFICATE_REQUESTED)
    @PostMapping(path = "/{id}/certs/access/",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyAccessCertificateResource> requestAccessCertificate(
        @PathVariable("id") @Valid UUID id,
        @Valid @RequestBody RelyingPartyCsrResource csrResource) {
        return ResponseEntity.ok(certificatesService.requestCertificateForRelyingParty(id, csrResource));
    }

    @Operation(
        summary = "Get access certificates for relying party",
        description = "Get all access certificates for relying party by ID",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Access certificates returned"),
        @ApiResponse(responseCode = "404", description = "Relying party not found")
    })
    @Audit(auditId = RELYING_PARTY_CERTIFICATE_RETRIEVED)
    @GetMapping(path = "/{relyingPartyId}/certs",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyAccessCertificatesResource> getAccessCertificates(
        @PathVariable("relyingPartyId") @Valid UUID relyingPartyId) {
        return ResponseEntity.ok(
            certificatesService.getAccessCertificatesForRelyingParty(relyingPartyId));
    }

    @Operation(
        summary = "Get access certificate",
        description = "Get access certificate by its ID and ID of the holding relying party",
        tags = {"relying-parties-certs-api"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Access certificates returned"),
        @ApiResponse(responseCode = "404", description = "Access certificate not found")
    })
    @Audit(auditId = RELYING_PARTY_CERTIFICATE_RETRIEVED)
    @GetMapping(path = "/{relyingPartyId}/certs/{certificateId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyAccessCertificateResource> getAccessCertificate(
        @PathVariable("relyingPartyId") @Valid UUID relyingPartyId,
        @PathVariable("certificateId") @Valid UUID certificateId) {
        return ResponseEntity.ok(
            certificatesService.getAccessCertificate(
                certificateId, relyingPartyId));
    }
}
