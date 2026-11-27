package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.logging.audit.Audit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RelyingPartyRevocationController {

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @Audit(auditId = "PARTY-REVOKED", includeResult = false, includeParameters = false)
    @PostMapping("/relying-party/revoke/{id}")
    public ModelAndView revoke(@PathVariable UUID id, @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());
        relyingPartiesService.revokeRelyingParty(registration.relyingPartyId());
        return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(id, returnUrl));
    }
}
