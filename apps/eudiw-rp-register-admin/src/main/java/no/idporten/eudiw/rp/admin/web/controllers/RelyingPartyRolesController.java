package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.web.form.InstanceRoleOption;
import no.idporten.eudiw.rp.admin.web.form.InstanceRolesForm;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
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

import java.util.ArrayList;
import java.util.HashSet;
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
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.setAllowedFields("entitlements[*].entitlement", "entitlements[*].credentialIssuerUrl",
            "eaas[*].namespace", "eaas[*].intent");
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

        if (!bindingResult.hasErrors()) {
            try {
                relyingPartiesService.updateInstanceRoles(registration.id(), form.toRequest());
                return new ModelAndView("redirect:" + DetailedViewController.detailsUrl(registration.id(), returnUrl));
            } catch (BadRequestException e) {
                bindingResult.reject("rejected",
                    "Endringa kunne ikkje lagrast. Sjekk at rollene er gyldige, og at roller med utstedarsertifikat ikkje er fjerna eller fått ny URL.");
            }
        }
        return formView(registration, options, returnUrl);
    }

    private RelyingPartyResource getAuthorizedRegistration(UUID id) {
        var registration = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(registration.orgno());
        if (!registration.active() && !userAuthorityService.userHasAdminAuthority()) {
            throw new InsufficientAuthorityException("Only administrators can edit inactive registrations");
        }
        return registration;
    }

    private List<InstanceRoleOption> roleOptions(RelyingPartyResource registration) {
        Set<String> locked = registration.issuerCertificates().stream()
            .map(RelyingPartyCertificateResource::entitlement)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<InstanceRoleOption> options = new ArrayList<>();
        Set<String> existing = new HashSet<>();
        for (RelyingPartyEntitlementResource role : registration.relyingPartyEntitlements()) {
            existing.add(role.entitlement());
            options.add(new InstanceRoleOption(role.entitlement(), role.displayName(), locked.contains(role.entitlement())));
        }
        if (userAuthorityService.userHasAdminAuthority()) {
            relyingPartiesService.getValidEntitlementOptions().stream()
                .filter(option -> !existing.contains(option.getEntitlement()))
                .forEach(option -> options.add(
                    new InstanceRoleOption(option.getEntitlement(), option.getDisplayName(), false)));
        }
        return options;
    }

    private void assertPostedRolesAllowed(RelyingPartyResource registration, List<InstanceRoleOption> options,
                                          InstanceRolesForm form) {
        Set<String> selected = form.selectedEntitlements().stream()
            .map(RelyingPartyEntitlementFormField::getEntitlement)
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

    private ModelAndView formView(RelyingPartyResource registration, List<InstanceRoleOption> options,
                                  String returnUrl) {
        return new ModelAndView("roles_form_view", Map.of(
            "registration", registration,
            "roleOptions", options,
            "returnUrl", DetailedViewController.validSearchUrl(returnUrl),
            "detailsUrl", DetailedViewController.detailsUrl(registration.id(), returnUrl)));
    }
}
