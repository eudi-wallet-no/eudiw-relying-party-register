package no.idporten.eudiw.rp.ca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.ca.config.CertificateAuthorities;
import no.idporten.eudiw.rp.ca.service.CertificateAuthorityService;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.cert.X509Certificate;

import static no.idporten.eudiw.rp.ca.api.CertificateAuthorityApiController.API_TAG;
import static no.idporten.eudiw.rp.ca.api.CertificateAuthorityApiController.errorResponseExample;

@Tag(name = API_TAG, description = "Relying Parties Certificate Authority API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(examples = {
                @ExampleObject(description = "Error response", value = errorResponseExample)
        })),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content(examples = {
                @ExampleObject(description = "Error response", value = errorResponseExample)
        }))
})
@RequiredArgsConstructor
@RestController
public class CertificateAuthorityApiController {

    private final CertificateAuthorityService certificateAuthorityService;
    private final CertificateAuthorities certificateAuthorities;

    public final static String APPLICATION_X_PEM_FILE_VALUE = "application/x-pem-file";
    public final static String errorResponseExample = "{\"error\": \"error_code\", \"error_description\": \"Description of the error\"}";
    public final static String API_TAG = "rp-ca-api-v1";

    @Operation(
            summary = "Download root CA certificate",
            description = "Download root CA certificate",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PEM-encoded certificate", content = @Content(mediaType = APPLICATION_X_PEM_FILE_VALUE))
    })
    @GetMapping(path = "/v1/certs/root.crt", produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> getRootCertificate() throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(certificateAuthorities.getRoot().getCertificate()));
    }

    @Operation(
            summary = "Download root CA CRL",
            description = "Download root CA certificate revocation list",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PEM-encoded CRL", content = @Content(mediaType = APPLICATION_X_PEM_FILE_VALUE))
    })
    @GetMapping(path = "/v1/certs/root.crl", produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> getRootCrl() throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(certificateAuthorityService.createCRL(certificateAuthorities.getRoot())));
    }

    @Operation(
            summary = "Download intermediate CA certificate",
            description = "Download intermediate CA certificate",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PEM-encoded certificate", content = @Content(mediaType = APPLICATION_X_PEM_FILE_VALUE))
    })
    @GetMapping(path = "/v1/certs/intermediates/access.crt", produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> getIntermediateCertificates() throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(certificateAuthorities.findIntermediate("access").getCertificate()));
    }

    @Operation(
            summary = "Download intermediate CA CRL",
            description = "Download intermediate CA certificate revocation list",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PEM-encoded CRL", content = @Content(mediaType = APPLICATION_X_PEM_FILE_VALUE))
    })
    @GetMapping(path = "/v1/certs/intermediates/access.crl", produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> getIntermediateCrl() throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(certificateAuthorityService.createCRL(certificateAuthorities.findIntermediate("access"))));
    }

    @Operation(
            summary = "Create RP access certificate",
            description = "Sign CSR with intermediate CA for RP access",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PEM-encoded certificate", content = @Content(mediaType = APPLICATION_X_PEM_FILE_VALUE))
    })
    @PostMapping(path = "/v1/certs/access", consumes = APPLICATION_X_PEM_FILE_VALUE, produces = {APPLICATION_X_PEM_FILE_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> signCertificate(@Valid @NotEmpty(message = "CSR cannot be null") @RequestBody String csr) throws Exception {
        PKCS10CertificationRequest pkcs10CertificationRequest = certificateAuthorityService.decodeCsr(csr);
        X509Certificate signedCertificate =
                certificateAuthorityService.signCertificate(
                        certificateAuthorities.findIntermediate("access"),
                        pkcs10CertificationRequest);
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(signedCertificate));
    }

}
