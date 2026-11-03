package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.RegistrationStatusForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RegistrationStatusController {

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @Audit(auditId = "REGISTRATION-STATUS-UPDATED", includeResult = false, includeParameters = false)
    @PostMapping("/admin/status/{id}")
    public ModelAndView updateStatus(
        @PathVariable UUID id,
        @ModelAttribute @Valid RegistrationStatusForm form,
        @AuditIgnore BindingResult bindingResult,
        @RequestParam(defaultValue = "/search") String returnUrl
    ) {
        userAuthorityService.assertUserHasAdminAuthority();
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());

        if (!bindingResult.hasErrors() && statusChangeNeeded(registration, form.getActive())) {
            relyingPartiesService.updateServiceAndInstanceStatus(registration, form.getActive());
        }
        String detailsUrl = UriComponentsBuilder.fromPath("/details/{id}")
            .queryParam("returnUrl", "{returnUrl}")
            .encode()
            .buildAndExpand(registration.id(), DetailedViewController.validSearchUrl(returnUrl))
            .toUriString();
        return new ModelAndView("redirect:" + detailsUrl);
    }

    private static boolean statusChangeNeeded(RelyingPartyResource registration, boolean activate) {
        boolean activationNeeded = activate && !registration.serviceAndInstanceActive();
        boolean deactivationNeeded = !activate && registration.serviceOrInstanceActive();
        return activationNeeded || deactivationNeeded;
    }
}
