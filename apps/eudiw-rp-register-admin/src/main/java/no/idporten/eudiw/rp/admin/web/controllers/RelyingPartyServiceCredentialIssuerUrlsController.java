package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.web.form.ServiceCredentialIssuerUrlsForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.eudiw.rp.admin.web.security.exception.InsufficientAuthorityException;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RelyingPartyServiceCredentialIssuerUrlsController {

    static final String FORM_ATTR = "credentialIssuerUrlsForm";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @InitBinder(FORM_ATTR)
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("credentialIssuerUrls[*].entitlement", "credentialIssuerUrls[*].credentialIssuerUrl",
            "credentialIssuerUrls[*].originalCredentialIssuerUrl");
    }

    @GetMapping("/service/credential-issuer-urls/edit/{id}")
    public ModelAndView edit(@PathVariable UUID id,
                             @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        var credentialIssuerUrls = relyingPartiesService.getServiceCredentialIssuerUrls(registration.relyingPartyServiceId());

        return formView(registration, credentialIssuerUrls, returnUrl)
            .addObject(FORM_ATTR, ServiceCredentialIssuerUrlsForm.from(credentialIssuerUrls));
    }

    @Audit(auditId = "RELYING-PARTY-SERVICE-CREDENTIAL-ISSUER-URL-UPDATED", includeResult = false, includeParameters = false)
    @PostMapping("/service/credential-issuer-urls/edit/{id}")
    public ModelAndView update(@PathVariable UUID id,
                               @ModelAttribute(FORM_ATTR) ServiceCredentialIssuerUrlsForm form,
                               @AuditIgnore BindingResult bindingResult,
                               @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        var credentialIssuerUrls = relyingPartiesService.getServiceCredentialIssuerUrls(registration.relyingPartyServiceId());

        for (int i = 0; i < form.getCredentialIssuerUrls().size(); i++) {
            var field = form.getCredentialIssuerUrls().get(i);

            if (field == null || credentialIssuerUrls.stream()
                .noneMatch(url -> url.entitlement().equals(field.getEntitlement()))) {
                throw new InsufficientAuthorityException("The role is not in the service URL list");
            }

            if (field.hasChangedCredentialIssuerUrl() && !field.hasValidCredentialIssuerUrl()) {
                bindingResult.rejectValue("credentialIssuerUrls[" + i + "].credentialIssuerUrl", "invalid",
                    "Skriv ein fullstendig HTTPS-URL, til dømes https://utsteder.no.");
            }
        }

        if (!bindingResult.hasErrors()) {
            try {
                var changedUrls = form.toRequest();

                if (!changedUrls.isEmpty()) {
                    relyingPartiesService.updateServiceCredentialIssuerUrls(registration.relyingPartyServiceId(), changedUrls);
                }

                return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(registration.id(), returnUrl));
            } catch (BadRequestException e) {
                bindingResult.reject("rejected", "URL-ane kunne ikkje lagrast. Kontroller adressene og prøv igjen.");
            }
        }

        return formView(registration, credentialIssuerUrls, returnUrl);
    }

    private RelyingPartyResource getAuthorizedRegistration(UUID id) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());

        return registration;
    }

    private ModelAndView formView(RelyingPartyResource registration,
                                  List<V2RegisterClient.ServiceCredentialIssuerUrl> credentialIssuerUrls,
                                  String returnUrl) {
        return new ModelAndView("service_credential_issuer_urls_form_view", Map.of(
            "registration", registration,
            "credentialIssuerUrls", credentialIssuerUrls,
            "entitlementDisplayNames", relyingPartiesService.getEntitlementDisplayNames(),
            "returnUrl", DetailedViewController.validSearchUrl(returnUrl),
            "detailsUrl", DetailedViewController.detailsUrl(registration.id(), returnUrl)));
    }
}
