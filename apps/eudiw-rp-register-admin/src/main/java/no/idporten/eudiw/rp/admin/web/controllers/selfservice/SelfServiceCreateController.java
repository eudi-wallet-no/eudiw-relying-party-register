package no.idporten.eudiw.rp.admin.web.controllers.selfservice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.AlreadyExistsException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.web.form.CreateRelyingPartyForm;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.BaseOidcUser;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.SelfServiceOidcUser;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.SelfServiceReportee;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class SelfServiceCreateController {

    public static final String createFormAttrId = "createFormAttr";
    public static final String errorResponseMsgAttrId = "errorResponseMsgAttr";

    private static final String LOMMEBOK_10_CREATE_RP_REQUEST = "LOMMEBOK-10-CREATE-RP-REQUEST";

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/create")
    public ModelAndView createGet(@AuthenticationPrincipal BaseOidcUser oidcUser) {
        if (oidcUser.isAdmin()) {
            return new ModelAndView("redirect:/admin/create");
        }
        if (!(oidcUser instanceof SelfServiceOidcUser selfServiceUser)) {
            throw new NotFoundException("Non-admin and non-selfservice user requested /create");
        }
        return new ModelAndView("selfservice/create_form_view",
                                Map.of(createFormAttrId, new CreateRelyingPartyForm(),
                                       "reporteeAttr", selfServiceUser.getReportee()));
    }

    @Audit(auditId = LOMMEBOK_10_CREATE_RP_REQUEST, includeResult = false, includeParameters = false)
    @PostMapping("/create")
    public ModelAndView createPost(
        @ModelAttribute(createFormAttrId) @Valid CreateRelyingPartyForm createForm,
        @AuditIgnore BindingResult createFormBindingResult,
        @AuthenticationPrincipal SelfServiceOidcUser selfServiceUser) {
        if (selfServiceUser == null) {
            throw new NotFoundException("Non-selfservice user attempted to POST /create");
        }

        ModelAndView mav = new ModelAndView("selfservice/create_form_view",
            Map.of(createFormAttrId, createForm));

        if (!createFormBindingResult.hasErrors()) {
            SelfServiceReportee reportee = selfServiceUser.getReportee();

            CreateRelyingPartyResource createResource =
                createForm.toResource(reportee.orgno(),
                                      reportee.name(),
                                      reportee.publicSector());
            try {
                RelyingPartyResource result = relyingPartiesService.create(createResource);
                return new ModelAndView("redirect:/details/" + result.id());
            } catch (AlreadyExistsException e) {
                log.info("Attempt to create RP which already exists", e);
                mav.addObject(errorResponseMsgAttrId, "exception.already_exists");
            }
        }
        return mav;
    }
}
