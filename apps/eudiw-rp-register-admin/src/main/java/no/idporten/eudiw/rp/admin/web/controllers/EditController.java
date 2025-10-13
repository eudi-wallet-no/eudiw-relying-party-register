package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.SearchSession;
import no.idporten.eudiw.rp.admin.web.form.selfservice.SelfServiceEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminEditRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
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
import java.util.function.Supplier;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EditController {

    public static final String detailedViewDataAttrId = SearchController.detailedViewDataAttrId;
    public static final String editFormAttrId = "editFormAttr";
    private static final String LOMMEBOK_12_EDIT_RP_REQUEST = "LOMMEBOK-12-EDIT-RP-REQUEST";

    private final RelyingPartiesService relyingPartiesService;
    private final SearchSession searchSession;

    @GetMapping("/edit/{id}")
    @PreAuthorize("@permissionsService.userHasPrivilegedAccessTo(#id)")
    public ModelAndView editGet(@PathVariable("id") @Valid UUID id) {

        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        AdminEditRelyingPartyForm editForm =
            AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(relyingPartyResource);

        return new ModelAndView("edit_form_view", Map.of(
            editFormAttrId, editForm,
            detailedViewDataAttrId, relyingPartyResource));
    }

    @Audit(auditId = LOMMEBOK_12_EDIT_RP_REQUEST, includeResult = false, includeParameters = false)
    @PostMapping("/edit/{id}")
    @PreAuthorize("@permissionsService.userHasPrivilegedAccessTo(#id)")
    public ModelAndView editPost(
        @PathVariable("id") UUID id,
        @ModelAttribute(editFormAttrId) @Valid SelfServiceEditRelyingPartyForm editForm,
        @AuditIgnore BindingResult editFormBindingResult) {

        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        Supplier<EditRelyingPartyResource> editResourceSupplier = () ->
            AdminEditRelyingPartyForm.prefillFromRelyingPartyResource(relyingPartyResource)
                                     .withEaas(editForm.getEaas())
                                     .toResource();
        return doEdit(relyingPartyResource, editFormBindingResult, editResourceSupplier);
    }

    @Audit(auditId = LOMMEBOK_12_EDIT_RP_REQUEST, includeResult = false, includeParameters = false)
    @PostMapping("/admin/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView adminEditPost(
        @PathVariable("id") UUID id,
        @ModelAttribute(editFormAttrId) @Valid AdminEditRelyingPartyForm editForm,
        @AuditIgnore BindingResult editFormBindingResult) {

        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        return doEdit(relyingPartyResource, editFormBindingResult, editForm::toResource);
    }

    private ModelAndView doEdit(RelyingPartyResource relyingPartyResource,
                                BindingResult bindingResult,
                                Supplier<EditRelyingPartyResource> editResourceSupplier) {
        ModelAndView mav =
            new ModelAndView("edit_form_view", Map.of(
                detailedViewDataAttrId, relyingPartyResource));
        if (!bindingResult.hasErrors()) {
            EditRelyingPartyResource editResource = editResourceSupplier.get();
            searchSession.edit(relyingPartyResource.id(), editResource);
            mav.setViewName("redirect:/details/" + relyingPartyResource.id());
        }
        return mav;
    }
}
