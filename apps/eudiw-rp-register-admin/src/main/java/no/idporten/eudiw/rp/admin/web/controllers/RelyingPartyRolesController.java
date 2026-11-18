package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.web.form.InstanceRoleOption;
import no.idporten.eudiw.rp.admin.web.form.InstanceRolesForm;
import no.idporten.eudiw.rp.admin.web.form.InstanceRoleFormField;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class RelyingPartyRolesController {

    static final String FORM_ATTR = "editFormAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @InitBinder(FORM_ATTR)
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("entitlements[*].entitlement");
    }

    @GetMapping("/roles/edit/{id}")
    public ModelAndView edit(@PathVariable UUID id,
                             @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        var options = roleOptions(registration);

        return formView(registration, options, returnUrl)
            .addObject(FORM_ATTR, InstanceRolesForm.from(registration, options));
    }

    @Audit(auditId = "RELYING-PARTY-ROLES-UPDATED", includeResult = false, includeParameters = false)
    @PostMapping("/roles/edit/{id}")
    public ModelAndView update(@PathVariable UUID id,
                               @ModelAttribute(FORM_ATTR) @Valid InstanceRolesForm form,
                               @AuditIgnore BindingResult bindingResult,
                               @RequestParam(defaultValue = "/search") String returnUrl) {
        var registration = getAuthorizedRegistration(id);
        var options = roleOptions(registration);

        assertPostedRolesAllowed(registration, options, form);
        form.addMissingRoleRows(options);
        validateAdministratorSelectedRole(form, bindingResult);

        if (!bindingResult.hasErrors()) {
            try {
                relyingPartiesService.updateInstanceRoles(registration.id(), form.toRequest());

                return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(registration.id(), returnUrl));
            } catch (BadRequestException e) {
                bindingResult.reject("rejected",
                    "Endringen kunne ikke lagres. Kontroller rollene. Roller med utstedersertifikat kan ikke fjernes.");
            }
        }

        return formView(registration, options, returnUrl);
    }

    private RelyingPartyResource getAuthorizedRegistration(UUID id) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());
        return registration;
    }

    private List<InstanceRoleOption> roleOptions(RelyingPartyResource registration) {
        Set<String> locked = registration.issuerCertificates().stream()
            .map(RelyingPartyCertificateResource::entitlement)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<String> existing = registration.relyingPartyEntitlements().stream()
            .map(RelyingPartyEntitlementResource::entitlement)
            .collect(Collectors.toSet());
        Map<String, String> displayNames = relyingPartiesService.getEntitlementDisplayNames();
        Map<String, String> credentialIssuerUrls = new HashMap<>();
        relyingPartiesService.getServiceCredentialIssuerUrls(registration.relyingPartyServiceId())
            .forEach(url -> credentialIssuerUrls.put(url.entitlement(), url.credentialIssuerUrl()));
        List<String> serviceEntitlements = relyingPartiesService.getServiceEntitlements(
            registration.relyingPartyServiceId());

        return serviceEntitlements.stream()
            .filter(entitlement -> userAuthorityService.userHasAdminAuthority() || existing.contains(entitlement))
            .map(entitlement -> new InstanceRoleOption(
                entitlement,
                displayNames.getOrDefault(entitlement, entitlement),
                credentialIssuerUrls.get(entitlement),
                locked.contains(entitlement)))
            .toList();
    }

    private void assertPostedRolesAllowed(RelyingPartyResource registration, List<InstanceRoleOption> options,
                                          InstanceRolesForm form) {
        Set<String> selected = form.selectedEntitlements().stream()
            .map(InstanceRoleFormField::getEntitlement)
            .collect(Collectors.toSet());
        Set<String> allowed = options.stream().map(InstanceRoleOption::entitlement).collect(Collectors.toSet());

        if (!allowed.containsAll(selected)) {
            throw new InsufficientAuthorityException("Posted roles are not among the allowed roles");
        }

        if (!userAuthorityService.userHasAdminAuthority()) {
            Set<String> existing = registration.relyingPartyEntitlements().stream()
                .map(RelyingPartyEntitlementResource::entitlement)
                .collect(Collectors.toSet());

            if (!existing.equals(selected)) {
                throw new InsufficientAuthorityException("Only administrators can change the role set");
            }
        }
    }

    private void validateAdministratorSelectedRole(InstanceRolesForm form, BindingResult bindingResult) {
        if (userAuthorityService.userHasAdminAuthority() && form.selectedEntitlements().isEmpty()) {
            bindingResult.reject("noRoleSelected", "Brukerstedet må ha minst ei rolle");
        }
    }

    private ModelAndView formView(RelyingPartyResource registration, List<InstanceRoleOption> options,
                                  String returnUrl) {
        return new ModelAndView("roles_form_view", Map.of(
            "registration", registration,
            "roleOptions", options,
            "returnUrl", DetailedViewController.validSearchUrl(returnUrl),
            "detailsUrl", DetailedViewController.detailsUrl(registration.id(), returnUrl)));
    }
}
