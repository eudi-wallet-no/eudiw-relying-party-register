package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RevokeCertificateController {

    private final RelyingPartiesService relyingPartiesService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/revoke-certificate/{rp-id}/access/{cert-id}")
    public ModelAndView revokeAccessCertificate(
            @PathVariable("rp-id") UUID relyingPartyId,
            @PathVariable("cert-id") UUID certificateId
    ) {
        relyingPartiesService.revokeAccessCertificate(relyingPartyId, certificateId);
        log.info("Access certificate revoked: {}", certificateId);
        return new ModelAndView("redirect:/details/{rp-id}#access-certificates");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/revoke-certificate/{rp-id}/issuer/{cert-id}")
    public ModelAndView revokeIssuerCertificate(
            @PathVariable("rp-id") UUID relyingPartyId,
            @PathVariable("cert-id") UUID certificateId
    ) {
        relyingPartiesService.revokeIssuerCertificate(relyingPartyId, certificateId);
        log.info("Issuer certificate revoked: {}", certificateId);
        return new ModelAndView("redirect:/details/{rp-id}#issuer-certificates");
    }
}
