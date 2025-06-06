package no.idporten.eudiw.ca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.ca.config.CertificateAuthorities;
import no.idporten.eudiw.ca.service.CertificateAuthorityService;
import no.idporten.validators.orgnr.Orgnr;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.cert.X509Certificate;

import static no.idporten.eudiw.ca.api.CertificateAuthorityApiController.API_TAG;
import static no.idporten.eudiw.ca.api.CertificateAuthorityApiController.errorResponseExample;

@Tag(name = API_TAG, description = "eIDAS 2.0 NO Sandbox Certificate Authority API")
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(
                        examples = {@ExampleObject(description = "Error response", value = errorResponseExample)},
                        mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
                responseCode = "500",
                description = "Server error",
                content = @Content(
                        examples = {@ExampleObject(description = "Error response", value = errorResponseExample)},
                mediaType = MediaType.APPLICATION_JSON_VALUE))
})
@Validated
@RequiredArgsConstructor
@RestController
public class CertificateAuthorityApiController {

    private final CertificateAuthorityService certificateAuthorityService;
    private final CertificateAuthorities certificateAuthorities;

    public final static String APPLICATION_X_PEM_FILE_VALUE = "application/x-pem-file";
    public final static String APPLICATION_X_PKIX_CERT_VALUE = "application/pkix-cert";
    public final static String APPLICATION_X_PKIX_CRL_VALUE = "application/pkix-crl";
    public final static String errorResponseExample = "{\"error\": \"error_code\", \"error_description\": \"Description of the error\"}";
    public final static String certificateSigningRequestExample = """
            -----BEGIN NEW CERTIFICATE REQUEST-----
            MII...
            -----END NEW CERTIFICATE REQUEST-----
            """;
    public final static String API_TAG = "ca-api-v1";

    @Operation(
            summary = "Download root CA certificate",
            description = "Download root CA certificate",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DER-encoded certificate", content = @Content(mediaType = APPLICATION_X_PKIX_CERT_VALUE))
    })
    @GetMapping(path = {"/v1/certs/root.crt", "/v1/certs/root.cer"}, produces = APPLICATION_X_PKIX_CERT_VALUE)
    public ResponseEntity<byte[]> getRootCertificate() throws Exception {
        return ResponseEntity.ok(certificateAuthorities.getRoot().getCertificate().getEncoded());
    }

    @Operation(
            summary = "Download root CA CRL",
            description = "Download root CA certificate revocation list",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DER-encoded CRL", content = @Content(mediaType = APPLICATION_X_PKIX_CRL_VALUE))
    })
    @GetMapping(path = "/v1/certs/root.crl", produces = APPLICATION_X_PKIX_CRL_VALUE)
    public ResponseEntity<byte[]> getRootCrl() throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.createCRL(certificateAuthorities.getRoot()).getEncoded());
    }

    @Operation(
            summary = "Download intermediate CA certificate",
            description = "Download intermediate CA certificate",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DER-encoded certificate", content = @Content(mediaType = APPLICATION_X_PKIX_CERT_VALUE))
    })
    @GetMapping(path = {"/v1/certs/intermediates/{intermediate}.crt", "/v1/certs/intermediates/{intermediate}.cer"}, produces = APPLICATION_X_PKIX_CERT_VALUE)
    public ResponseEntity<byte[]> getIntermediateCertificates(
            @Parameter(
                    description = "Intermediate CA name",
                    examples = {
                            @ExampleObject(name = "access", value = "access", description = "RP access CA"),
                            @ExampleObject(name = "issuer", value = "issuer", description = "Issuer CA")},
                    required = true)
            @PathVariable("intermediate") String intermediate) throws Exception {
        return ResponseEntity.ok(certificateAuthorities.findIntermediate(intermediate).getCertificate().getEncoded());
    }

    @Operation(
            summary = "Download intermediate CA CRL",
            description = "Download intermediate CA certificate revocation list",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DER-encoded CRL", content = @Content(mediaType = APPLICATION_X_PKIX_CRL_VALUE))
    })
    @GetMapping(path = "/v1/certs/intermediates/{intermediate}.crl", produces = APPLICATION_X_PKIX_CRL_VALUE)
    public ResponseEntity<byte[]> getIntermediateCrl(
            @Parameter(
                    description = "Intermediate CA name",
                    examples = {
                            @ExampleObject(name = "access", value = "access", description = "RP access CA"),
                            @ExampleObject(name = "issuer", value = "issuer", description = "Issuer CA")},
                    required = true)
            @PathVariable("intermediate") String intermediate) throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.createCRL(certificateAuthorities.findIntermediate(intermediate)).getEncoded());
    }

    @Operation(
            summary = "Issue certificate",
            description = "Sign CSR with intermediate CA",
            tags = {API_TAG})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PEM-encoded certificate", content = @Content(mediaType = APPLICATION_X_PEM_FILE_VALUE))
    })
    @PostMapping(path = "/v1/certs/{intermediate}/{orgno}", consumes = APPLICATION_X_PEM_FILE_VALUE, produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> signLeafCertificate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "PEM-encoded Certificate Signing Request with SAN extension",
                    content = {
                            @Content(mediaType = APPLICATION_X_PEM_FILE_VALUE,
                                    examples = @ExampleObject(value = certificateSigningRequestExample))},
                    required = true)
            @Valid @NotEmpty(message = "CSR cannot be null") @RequestBody String csr,
            @Parameter(
                    description = "Intermediate CA name",
                    examples = {
                            @ExampleObject(name = "access", value = "access", description = "RP access CA"),
                            @ExampleObject(name = "issuer", value = "issuer", description = "Issuer CA")},
                    required = true)
            @PathVariable("intermediate") String intermediate,
            @Parameter(
                    description = "Organization number",
                    required = true)
            @Valid @Orgnr(message = "Invalid organization number") @NotEmpty @PathVariable String orgno) throws Exception {
        PKCS10CertificationRequest pkcs10CertificationRequest = certificateAuthorityService.decodeCsr(csr);
        X509Certificate signedCertificate =
                certificateAuthorityService.signCertificate(
                        certificateAuthorities.findIntermediate(intermediate),
                        pkcs10CertificationRequest,
                        orgno);
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(signedCertificate));
    }

}
