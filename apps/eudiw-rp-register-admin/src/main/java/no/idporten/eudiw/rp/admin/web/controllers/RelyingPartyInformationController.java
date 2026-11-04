package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyInformationForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RelyingPartyInformationController {

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @InitBinder("relyingPartyForm")
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("tradeName", "country", "postalAddress", "email", "phone", "informationUri");
    }

    @GetMapping("/relying-party/edit/{id}")
    public ModelAndView edit(@PathVariable UUID id,
                             @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        var relyingParty = getAuthorizedRelyingParty(registration.relyingPartyId());
        return formView(registration, relyingParty, returnUrl)
            .addObject("relyingPartyForm", RelyingPartyInformationForm.from(relyingParty));
    }

    @Audit(auditId = "RELYING-PARTY-INFORMATION-UPDATED", includeResult = false, includeParameters = false)
    @PostMapping("/relying-party/edit/{id}")
    public ModelAndView update(@PathVariable UUID id,
                               @ModelAttribute("relyingPartyForm") @Valid RelyingPartyInformationForm form,
                               @AuditIgnore BindingResult bindingResult,
                               @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        var relyingParty = getAuthorizedRelyingParty(registration.relyingPartyId());
        if (!form.hasValidInformationUris()) {
            bindingResult.rejectValue("informationUri", "invalid",
                "Skriv inn ei fullstendig adresse, til dømes https://digdir.no.");
        }
        if (bindingResult.hasErrors()) {
            return formView(registration, relyingParty, returnUrl);
        }

        relyingPartiesService.updateRelyingPartyInformation(relyingParty.id(), form.toRequest(relyingParty));
        return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(registration.id(), returnUrl));
    }

    private RelyingPartyResource getAuthorizedRegistration(UUID id) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());
        return registration;
    }

    private V2RegisterClient.RelyingParty getAuthorizedRelyingParty(UUID id) {
        var relyingParty = relyingPartiesService.getRelyingPartyInformation(id);
        userAuthorityService.assertUserHasAccessTo(relyingParty.orgno());
        return relyingParty;
    }

    private ModelAndView formView(RelyingPartyResource registration, V2RegisterClient.RelyingParty relyingParty,
                                  String returnUrl) {
        return new ModelAndView("relying_party_information_form_view", Map.of(
            "registration", registration,
            "relyingParty", relyingParty,
            "returnUrl", DetailedViewController.validSearchUrl(returnUrl),
            "detailsUrl", DetailedViewController.detailsUrl(registration.id(), returnUrl)));
    }
}
