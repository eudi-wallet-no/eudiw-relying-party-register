package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.form.BaseEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EditController {

    public static final String DETAILED_VIEW_DATA_ATTR = SearchController.DETAILED_VIEW_DATA_ATTR;
    public static final String EDIT_FORM_ATTR = "editFormAttr";

    private static final String LOMMEBOK_12_EDIT_RP_REQUEST = "LOMMEBOK-12-EDIT-RP-REQUEST";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @GetMapping("/edit/{id}")
    public ModelAndView editGet(@PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(relyingPartyResource.orgno());

        AdminEditRelyingPartyForm editForm =
            AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(relyingPartyResource);

        return new ModelAndView("edit_form_view", Map.of(
            EDIT_FORM_ATTR, editForm,
            DETAILED_VIEW_DATA_ATTR, relyingPartyResource));
    }

    @Audit(auditId = LOMMEBOK_12_EDIT_RP_REQUEST, includeResult = false, includeParameters = false)
    @PostMapping("/edit/{id}")
    public ModelAndView editPost(
        @PathVariable("id") UUID id,
        @ModelAttribute(EDIT_FORM_ATTR) @Valid BaseEditRelyingPartyForm editForm,
        @AuditIgnore BindingResult editFormBindingResult) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(relyingPartyResource.orgno());

        if (editFormBindingResult.hasErrors()) {
            return new ModelAndView("edit_form_view",
                DETAILED_VIEW_DATA_ATTR, relyingPartyResource);
        }

        EditRelyingPartyResource editResource =
            editForm.toResource()
                    .withRelyingPartyEntitlements(relyingPartyResource.relyingPartyEntitlements())
                    .withActive(relyingPartyResource.active());
        relyingPartiesService.edit(relyingPartyResource.id(), editResource);
        return new ModelAndView("redirect:/details/" + relyingPartyResource.id());
    }

    @Audit(auditId = LOMMEBOK_12_EDIT_RP_REQUEST, includeResult = false, includeParameters = false)
    @PostMapping("/admin/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView adminEditPost(
        @PathVariable("id") UUID id,
        @ModelAttribute(EDIT_FORM_ATTR) @Valid AdminEditRelyingPartyForm editForm,
        @AuditIgnore BindingResult editFormBindingResult) {

        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);

        if (editFormBindingResult.hasErrors()) {
            return new ModelAndView("edit_form_view",
                DETAILED_VIEW_DATA_ATTR, relyingPartyResource);
        }
        EditRelyingPartyResource editResource = editForm.toResource();
        relyingPartiesService.edit(relyingPartyResource.id(), editResource);
        return new ModelAndView("redirect:/details/" + relyingPartyResource.id());
    }
}
