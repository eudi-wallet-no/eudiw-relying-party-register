package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.AlreadyExistsException;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyCreateForm;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
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
    public static final String errorResponseMsgAttrId = "errorResponseMsgAttr";

    private static final String LOMMEBOK_10_CREATE_RP_REQUEST = "LOMMEBOK-10-CREATE-RP-REQUEST";

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/create")
    public ModelAndView createGet() {
        return new ModelAndView("create_form_view", Map.of(createFormAttrId, new RelyingPartyCreateForm()));
    }

    @Audit(auditId = LOMMEBOK_10_CREATE_RP_REQUEST, includeResult = false, includeParameters = false)
    @PostMapping("/create")
    public ModelAndView createPost(
        @ModelAttribute(createFormAttrId) @Valid RelyingPartyCreateForm createForm,
        @AuditIgnore BindingResult createFormBindingResult
    ) {
        ModelAndView mav = new ModelAndView("create_form_view",
            Map.of(createFormAttrId, createForm));

        if (!createFormBindingResult.hasErrors()) {
            CreateRelyingPartyResource createResource = createForm.toResource();
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
