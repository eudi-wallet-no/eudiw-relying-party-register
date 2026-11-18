package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.entitlements.Entitlements;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.form.ServiceCredentialIssuerUrlForm;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RelyingPartyServiceCredentialIssuerUrlsController {

    static final String FORM_ATTR = "credentialIssuerUrlsForm";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @InitBinder(FORM_ATTR)
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("credentialIssuerUrls[*].entitlement", "credentialIssuerUrls[*].selected",
            "credentialIssuerUrls[*].credentialIssuerUrl");
    }

    @GetMapping("/service/credential-issuer-urls/edit/{id}")
    public ModelAndView edit(@PathVariable UUID id,
                             @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        var serviceEntitlements = relyingPartiesService.getServiceEntitlements(registration.relyingPartyServiceId());
        var credentialIssuerUrls = relyingPartiesService.getServiceCredentialIssuerUrls(
            registration.relyingPartyServiceId());

        return formView(registration, createForm(serviceEntitlements, credentialIssuerUrls), returnUrl);
    }

    @Audit(auditId = "RELYING-PARTY-SERVICE-CREDENTIAL-ISSUER-URL-UPDATED", includeResult = false, includeParameters = false)
    @PostMapping("/service/credential-issuer-urls/edit/{id}")
    public ModelAndView update(@PathVariable UUID id,
                               @ModelAttribute(FORM_ATTR) ServiceCredentialIssuerUrlsForm form,
                               @AuditIgnore BindingResult bindingResult,
                               @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        var serviceEntitlements = relyingPartiesService.getServiceEntitlements(registration.relyingPartyServiceId());
        var credentialIssuerUrls = relyingPartiesService.getServiceCredentialIssuerUrls(registration.relyingPartyServiceId());
        var expectedForm = createForm(serviceEntitlements, credentialIssuerUrls);
        addAvailableRoles(form, expectedForm);
        Set<String> selectedEntitlements = new LinkedHashSet<>(form.selectedEntitlements());
        Set<String> currentEntitlements = new HashSet<>(serviceEntitlements);

        for (int i = 0; i < form.getCredentialIssuerUrls().size(); i++) {
            var field = form.getCredentialIssuerUrls().get(i);
            if (field.isSelected() && field.isHasCredentialIssuerUrl() && field.hasChangedCredentialIssuerUrl()
                && !field.hasValidCredentialIssuerUrl()) {
                bindingResult.rejectValue("credentialIssuerUrls[" + i + "].credentialIssuerUrl", "invalid",
                    "Skriv ein fullstendig HTTPS-URL, til dømes https://utsteder.no.");
            }
        }

        if (!userAuthorityService.userHasAdminAuthority() && !currentEntitlements.equals(selectedEntitlements)) {
            throw new InsufficientAuthorityException("Only administrators can change the service role set");
        }

        if (!bindingResult.hasErrors()) {
            try {
                var changedUrls = form.changedCredentialIssuerUrls();

                if (!currentEntitlements.equals(selectedEntitlements) || !changedUrls.isEmpty()) {
                    if (userAuthorityService.userHasAdminAuthority()) {
                        relyingPartiesService.updateServiceEntitlements(
                            registration.relyingPartyServiceId(),
                            new V2RegisterClient.ServiceEntitlementsRequest(
                                List.copyOf(selectedEntitlements), changedUrls));
                    } else {
                        relyingPartiesService.updateServiceCredentialIssuerUrls(
                            registration.relyingPartyServiceId(), changedUrls);
                    }
                }

                return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(registration.id(), returnUrl));
            } catch (BadRequestException e) {
                bindingResult.reject("rejected",
                    "Rollene eller URL-ane kunne ikkje lagrast. Kontroller verdiane og prøv igjen.");
            }
        }

        return formView(registration, form, returnUrl);
    }

    private RelyingPartyResource getAuthorizedRegistration(UUID id) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());

        return registration;
    }

    private ServiceCredentialIssuerUrlsForm createForm(
        List<String> serviceEntitlements,
        List<V2RegisterClient.ServiceCredentialIssuerUrl> credentialIssuerUrls
    ) {
        boolean administrator = userAuthorityService.userHasAdminAuthority();
        Set<String> currentEntitlements = new HashSet<>(serviceEntitlements);
        Set<String> selectableEntitlements = new LinkedHashSet<>(serviceEntitlements);
        Map<String, String> displayNames = new HashMap<>(relyingPartiesService.getEntitlementDisplayNames());

        if (administrator) {
            List<RelyingPartyEntitlementFormField> activeEntitlementOptions =
                relyingPartiesService.getValidEntitlementOptions();
            activeEntitlementOptions.forEach(option -> {
                selectableEntitlements.add(option.getEntitlement());
                displayNames.put(option.getEntitlement(), option.getDisplayName());
            });
        }

        Map<String, String> urlByEntitlement = new HashMap<>();
        credentialIssuerUrls.forEach(url -> urlByEntitlement.put(url.entitlement(), url.credentialIssuerUrl()));

        var form = new ServiceCredentialIssuerUrlsForm();
        for (String entitlement : selectableEntitlements) {
            var field = new ServiceCredentialIssuerUrlForm();
            String issuerUrl = urlByEntitlement.get(entitlement);
            field.setEntitlement(entitlement);
            field.setDisplayName(displayNames.getOrDefault(entitlement, entitlement));
            field.setServiceRole(currentEntitlements.contains(entitlement));
            field.setSelected(currentEntitlements.contains(entitlement));
            field.setCredentialIssuerUrl(issuerUrl);
            field.setOriginalCredentialIssuerUrl(issuerUrl);
            field.setHasCredentialIssuerUrl(Entitlements.isIssuerEntitlement(entitlement)
                || issuerUrl != null);
            form.getCredentialIssuerUrls().add(field);
        }

        return form;
    }

    private void addAvailableRoles(ServiceCredentialIssuerUrlsForm form, ServiceCredentialIssuerUrlsForm expectedForm) {
        var submittedRoles = form.getCredentialIssuerUrls();
        var availableRoles = expectedForm.getCredentialIssuerUrls();

        if (submittedRoles == null || submittedRoles.size() != availableRoles.size()) {
            throw new InsufficientAuthorityException("Expected one submitted row per available service role");
        }

        for (int i = 0; i < availableRoles.size(); i++) {
            var field = submittedRoles.get(i);
            var expected = availableRoles.get(i);

            if (field == null || !Objects.equals(field.getEntitlement(), expected.getEntitlement())) {
                throw new InsufficientAuthorityException("The submitted row does not match the expected service role");
            }

            field.setDisplayName(expected.getDisplayName());
            field.setServiceRole(expected.isServiceRole());
            field.setHasCredentialIssuerUrl(expected.isHasCredentialIssuerUrl());
            field.setOriginalCredentialIssuerUrl(expected.getOriginalCredentialIssuerUrl());
        }
    }

    private ModelAndView formView(RelyingPartyResource registration,
                                  ServiceCredentialIssuerUrlsForm form,
                                  String returnUrl) {
        return new ModelAndView("service_credential_issuer_urls_form_view", Map.of(
            "registration", registration,
            "isAdministrator", userAuthorityService.userHasAdminAuthority(),
            FORM_ATTR, form,
            "returnUrl", DetailedViewController.validSearchUrl(returnUrl),
            "detailsUrl", DetailedViewController.detailsUrl(registration.id(), returnUrl)));
    }
}
