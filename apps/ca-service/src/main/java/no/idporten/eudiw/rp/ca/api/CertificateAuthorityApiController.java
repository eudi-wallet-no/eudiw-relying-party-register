package no.idporten.eudiw.rp.ca.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.ca.config.CertificateAuthorities;
import no.idporten.eudiw.rp.ca.config.CertificateAuthority;
import no.idporten.eudiw.rp.ca.service.CertificateAuthorityService;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.cert.X509Certificate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
public class CertificateAuthorityApiController {

    private final CertificateAuthorityService certificateAuthorityService;
    private final CertificateAuthorities certificateAuthorities;

    private final static String APPLICATION_X_PEM_FILE_VALUE = "application/x-pem-file";


    @GetMapping(path = "/v1/certs/root", produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> getRootCertificate() throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(certificateAuthorities.getRoot().getCertificate()));
    }


    @GetMapping(path = "/v1/certs/intermediates", produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> getIntermediateCertificates() throws Exception {
        return ResponseEntity.ok(certificateAuthorities.getIntermediates()
                .stream()
                .map(CertificateAuthority::getCertificate)
                .map(certificateAuthorityService::encodeToPem)
                .collect(Collectors.joining(System.lineSeparator())));
    }

    @GetMapping(path = "/v1/crl/root", produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> getRootCrl() throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(certificateAuthorityService.createCRL(certificateAuthorities.getRoot())));
    }

    @GetMapping(path = "/v1/crl/intermediates/access", produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> getIntermediateCrl() throws Exception {
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(certificateAuthorityService.createCRL(certificateAuthorities.findIntermediate("access"))));
    }

    @PostMapping(path = "/v1/access/certificate", consumes = APPLICATION_X_PEM_FILE_VALUE, produces = APPLICATION_X_PEM_FILE_VALUE)
    public ResponseEntity<String> processCSR(@Valid @RequestBody String csr) throws Exception {
        PKCS10CertificationRequest pkcs10CertificationRequest = certificateAuthorityService.decodeCsr(csr);
        X509Certificate signedCertificate =
                certificateAuthorityService.signCertificate(
                        certificateAuthorities.findIntermediate("access"),
                        pkcs10CertificationRequest);
        return ResponseEntity.ok(certificateAuthorityService.encodeToPem(signedCertificate));
    }

}
