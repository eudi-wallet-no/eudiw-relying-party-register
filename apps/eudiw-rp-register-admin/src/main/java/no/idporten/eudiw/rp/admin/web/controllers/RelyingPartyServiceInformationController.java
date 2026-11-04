package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyServiceInformationForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.eudiw.rp.admin.web.security.exception.InsufficientAuthorityException;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
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
public class RelyingPartyServiceInformationController {

    static final String FORM_ATTR = "serviceForm";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @InitBinder(FORM_ATTR)
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.setAllowedFields("serviceTradeName", "serviceIdentifier", "descriptions[*].lang",
            "descriptions[*].content", "supportUri", "email", "phone");
    }

    @GetMapping("/service/edit/{id}")
    public ModelAndView edit(@PathVariable UUID id,
                             @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        return formView(registration, returnUrl)
            .addObject(FORM_ATTR, RelyingPartyServiceInformationForm.from(registration));
    }

    @Audit(auditId = "RELYING-PARTY-SERVICE-INFORMATION-UPDATED", includeResult = false, includeParameters = false)
    @PostMapping("/service/edit/{id}")
    public ModelAndView update(@PathVariable UUID id,
                               @ModelAttribute(FORM_ATTR) @Valid RelyingPartyServiceInformationForm form,
                               @AuditIgnore BindingResult bindingResult,
                               @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        validate(form, bindingResult);

        if (bindingResult.hasErrors()) {
            form.addDefaultDescriptionRow();
            return formView(registration, returnUrl);
        }

        relyingPartiesService.updateServiceInformation(registration.relyingPartyServiceId(), form.toRequest());
        return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(registration.id(), returnUrl));
    }

    private void validate(RelyingPartyServiceInformationForm form, BindingResult bindingResult) {
        if (!form.hasCompleteDescriptions()) {
            bindingResult.rejectValue("descriptions", "incomplete",
                "Fyll ut både språk og skildring på kvar linje.");
        } else if (!form.hasValidDescriptionLanguages()) {
            bindingResult.rejectValue("descriptions", "invalid",
                "Skriv språket som språkkode, til dømes nb, nn eller en.");
        } else if (!form.hasUniqueDescriptionLanguages()) {
            bindingResult.rejectValue("descriptions", "duplicate",
                "Kvart språk kan berre ha éi skildring.");
        } else if (!form.hasDescription()) {
            bindingResult.rejectValue("descriptions", "required", "Skriv inn tenesteskildring.");
        }

        if (!form.hasValidSupportUri()) {
            bindingResult.rejectValue("supportUri", "invalid",
                "Skriv inn ei fullstendig adresse, til dømes https://digdir.no/stotte.");
        }

        if (!form.hasContactInformation()) {
            bindingResult.rejectValue("supportUri", "required",
                "Skriv inn støtteadresse, e-post eller telefon.");
        }
    }

    private RelyingPartyResource getAuthorizedRegistration(UUID id) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());

        if (!registration.active() && !userAuthorityService.userHasAdminAuthority()) {
            throw new InsufficientAuthorityException("Only administrators can edit inactive registrations");
        }

        return registration;
    }

    private ModelAndView formView(RelyingPartyResource registration, String returnUrl) {
        return new ModelAndView("service_information_form_view", Map.of(
            "registration", registration,
            "returnUrl", DetailedViewController.validSearchUrl(returnUrl),
            "detailsUrl", DetailedViewController.detailsUrl(registration.id(), returnUrl)));
    }
}
