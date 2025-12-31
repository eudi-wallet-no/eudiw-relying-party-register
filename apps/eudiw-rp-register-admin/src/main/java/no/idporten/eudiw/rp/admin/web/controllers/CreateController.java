package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.BaseCreateRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.form.admin.AdminCreateRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CreateController {

    public static final String createFormAttrId = "createFormAttr";
    public static final String reporteeAuthorityAttrId = "reporteeAuthorityAttr";

    private static final String LOMMEBOK_10_CREATE_RP_REQUEST = "LOMMEBOK-10-CREATE-RP-REQUEST";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @GetMapping("/create")
    public ModelAndView createGet() {
        if (userAuthorityService.userHasAdminAuthority()) {
            return new ModelAndView("create_form_view",
                createFormAttrId, new AdminCreateRelyingPartyForm());
        }

        // if user does not have admin authority, they must have a reportee authority.
        ReporteeAuthority reportee = userAuthorityService.getReporteeAuthority();
        return new ModelAndView("create_form_view", Map.of(
            createFormAttrId, new BaseCreateRelyingPartyForm(),
            reporteeAuthorityAttrId, reportee));
    }

    @Audit(auditId = LOMMEBOK_10_CREATE_RP_REQUEST, includeResult = false, includeParameters = false)
    @PostMapping("/create")
    public ModelAndView selfServiceCreatePost(
        @ModelAttribute(createFormAttrId) @Valid BaseCreateRelyingPartyForm createForm,
        @AuditIgnore BindingResult createFormBindingResult) {
        ReporteeAuthority reportee = userAuthorityService.getReporteeAuthority();
        if (createFormBindingResult.hasErrors()) {
            return new ModelAndView("create_form_view", Map.of(
                createFormAttrId, createForm,
                reporteeAuthorityAttrId, reportee));
        }

        CreateRelyingPartyResource createResource = createForm.toResource(reportee.orgno());
        RelyingPartyResource result = relyingPartiesService.create(createResource);
        return new ModelAndView("redirect:/details/" + result.id());
    }

    @Audit(auditId = LOMMEBOK_10_CREATE_RP_REQUEST, includeResult = false, includeParameters = false)
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView adminCreatePost(
        @ModelAttribute(createFormAttrId) @Valid AdminCreateRelyingPartyForm createForm,
        @AuditIgnore BindingResult createFormBindingResult) {
        if (createFormBindingResult.hasErrors()) {
            return new ModelAndView("create_form_view", createFormAttrId, createForm);
        }

        CreateRelyingPartyResource createResource = createForm.toResource();
        RelyingPartyResource result = relyingPartiesService.create(createResource);
        return new ModelAndView("redirect:/details/" + result.id());
    }
}
