package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.web.form.IntendedUseForm;
import no.idporten.eudiw.rp.admin.web.form.IntendedUseMapper;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class IntendedUseController {

    static final String FORM_ATTR = "intendedUseForm";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @InitBinder(FORM_ATTR)
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.setAutoGrowCollectionLimit(1000);
        binder.setAllowedFields("purpose[*].lang", "purpose[*].content", "privacyPolicyUri",
            "credentials[*].name", "credentials[*].format", "credentials[*].vct",
            "credentials[*].doctype", "credentials[*].claimPaths");
    }

    @GetMapping("/service/intended-use/create/{id}")
    public ModelAndView createForm(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "/search") String returnUrl
    ) {
        // TODO: Legg inn kontroll mot revokert/inaktiv status før oppretting av føremål.
        var registration = getAuthorizedRegistration(id);
        var form = new IntendedUseForm();
        form.getCredentials().add(new IntendedUseForm.Credential());
        var bindingResult = new BeanPropertyBindingResult(form, FORM_ATTR);

        return formView(registration, form, bindingResult, returnUrl);
    }

    @Audit(auditId = "RELYING-PARTY-INTENDED-USE-CREATED", includeResult = false, includeParameters = false)
    @PostMapping("/service/intended-use/create/{id}")
    public ModelAndView create(
        @PathVariable UUID id,
        @ModelAttribute(FORM_ATTR) IntendedUseForm form,
        @AuditIgnore BindingResult bindingResult,
        @RequestParam(defaultValue = "/search") String returnUrl
    ) {
        // TODO: Legg inn kontroll mot revokert/inaktiv status før oppretting av føremål.
        var registration = getAuthorizedRegistration(id);
        form.addDefaultPurposeRow();
        validateForm(form, bindingResult);

        if (bindingResult.hasErrors()) {
            return formView(registration, form, bindingResult, returnUrl);
        }

        IntendedUseMapper.MappingResult mappingResult = IntendedUseMapper.toCreateRequest(form);
        if (mappingResult.hasError()) {
            bindingResult.rejectValue(mappingResult.errorField(), mappingResult.errorCode(), mappingResult.errorMessage());
            return formView(registration, form, bindingResult, returnUrl);
        }

        try {
            relyingPartiesService.createIntendedUse(registration.relyingPartyServiceId(), mappingResult.request());
        } catch (NotFoundException exception) {
            bindingResult.reject("inactive",
                "Tenesta eller brukarstaden er ikkje lenger tilgjengeleg. Kontroller statusen og prøv igjen.");
            return formView(registration, form, bindingResult, returnUrl);
        } catch (BadRequestException exception) {
            bindingResult.reject("rejected",
                "Verdiane kunne ikkje lagrast. Kontroller føremålet, adressa til personvernreglane og attributtstiane.");
            return formView(registration, form, bindingResult, returnUrl);
        }

        return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(registration.id(), returnUrl));
    }

    private void validateForm(IntendedUseForm form, BindingResult bindingResult) {
        if (!form.hasCompletePurposes()) {
            bindingResult.rejectValue("purpose", "incomplete",
                "Fyll ut både språk og føremål på kvar linje.");
        } else if (!form.hasValidPurposeLanguages()) {
            bindingResult.rejectValue("purpose", "invalid",
                "Skriv språket som språkkode, til dømes nb, nn eller en.");
        } else if (!form.hasUniquePurposeLanguages()) {
            bindingResult.rejectValue("purpose", "duplicate",
                "Kvart språk kan berre ha eitt føremål.");
        }

        if (!form.hasValidPrivacyPolicyUri()) {
            bindingResult.rejectValue("privacyPolicyUri", "invalid",
                "Skriv inn ei fullstendig adresse til personvernreglane.");
        }
    }

    private ModelAndView formView(
        RelyingPartyResource registration,
        IntendedUseForm form,
        BindingResult bindingResult,
        String returnUrl
    ) {
        form.addDefaultPurposeRow();
        ModelAndView modelAndView = new ModelAndView("intended_use_form_view");
        modelAndView.addAllObjects(bindingResult.getModel());
        modelAndView.addObject("registration", registration);
        modelAndView.addObject("emptyCredential", new IntendedUseForm.Credential());
        modelAndView.addObject("returnUrl", DetailedViewController.validSearchUrl(returnUrl));
        modelAndView.addObject("detailsUrl", DetailedViewController.detailsUrl(registration.id(), returnUrl));
        return modelAndView;
    }

    private RelyingPartyResource getAuthorizedRegistration(UUID id) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());
        return registration;
    }
}
