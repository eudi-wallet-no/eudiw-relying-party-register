package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.accesscertificates.X509CertificateConverter;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.security.cert.X509Certificate;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DownloadCertificateController {

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    private static final String APPLICATION_X_PEM_FILE_VALUE = "application/x-pem-file";

    @GetMapping("/get-certificate/{rp-id}/{cert-id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<byte[]> downloadCertificate(
        @PathVariable("rp-id") UUID relyingPartyId,
        @PathVariable("cert-id") UUID certificateId) {
        RelyingPartyResource relyingParty = relyingPartiesService.get(relyingPartyId);
        userAuthorityService.assertUserHasAccessTo(relyingParty.orgno());

        return doDownloadCertificate(
            relyingPartiesService.getAccessCertificate(relyingPartyId, certificateId));
    }

    @GetMapping("/get-certificate/{rp-id}/issuer/{cert-id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<byte[]> downloadIssuerCertificate(
        @PathVariable("rp-id") UUID relyingPartyId,
        @PathVariable("cert-id") UUID certificateId) {
        RelyingPartyResource relyingParty = relyingPartiesService.get(relyingPartyId);
        userAuthorityService.assertUserHasAccessTo(relyingParty.orgno());

        return doDownloadCertificate(
            relyingPartiesService.getIssuerCertificate(relyingPartyId, certificateId));
    }

    private ResponseEntity<byte[]> doDownloadCertificate(
        RelyingPartyCertificateResource certResource) {

        X509Certificate certificate = certResource.certificate();
        byte[] content = X509CertificateConverter.toPem(certificate).getBytes();
        String filename = "%s.pem".formatted(certificate.getSerialNumber());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, APPLICATION_X_PEM_FILE_VALUE);
        headers.setContentDisposition(
            ContentDisposition.attachment()
                              .filename(filename)
                              .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
