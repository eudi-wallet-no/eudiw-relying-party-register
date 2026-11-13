package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.web.form.InstanceEaasForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
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
public class RelyingPartyEaasController {

    private static final String FORM_ATTR = "editFormAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @InitBinder(FORM_ATTR)
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.setAllowedFields("eaas[*].namespace", "eaas[*].intent");
    }

    @GetMapping("/eaas/edit/{id}")
    public ModelAndView edit(@PathVariable UUID id,
                             @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);

        return formView(registration, returnUrl).addObject(FORM_ATTR, InstanceEaasForm.from(registration));
    }

    @Audit(auditId = "RELYING-PARTY-EAAS-UPDATED", includeResult = false, includeParameters = false)
    @PostMapping("/eaas/edit/{id}")
    public ModelAndView update(@PathVariable UUID id,
                               @ModelAttribute(FORM_ATTR) @Valid InstanceEaasForm form,
                               @AuditIgnore BindingResult bindingResult,
                               @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);

        if (!bindingResult.hasErrors()) {
            try {
                relyingPartiesService.updateInstanceEaas(registration.id(), form.toRequest());
                return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(registration.id(), returnUrl));
            } catch (BadRequestException e) {
                bindingResult.reject("rejected", "Endringa kunne ikkje lagrast. Sjekk bevistypane og føremåla.");
            }
        }

        return formView(registration, returnUrl);
    }

    private RelyingPartyResource getAuthorizedRegistration(UUID id) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());

        return registration;
    }

    private ModelAndView formView(RelyingPartyResource registration, String returnUrl) {
        return new ModelAndView("eaas_form_view", Map.of(
            "registration", registration,
            "returnUrl", DetailedViewController.validSearchUrl(returnUrl),
            "detailsUrl", DetailedViewController.detailsUrl(registration.id(), returnUrl)));
    }
}
