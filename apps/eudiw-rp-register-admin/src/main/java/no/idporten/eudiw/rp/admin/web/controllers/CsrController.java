package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.form.CsrForm;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyCsrResource;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CsrController {

    public static final String detailedViewDataAttrId = SearchController.detailedViewDataAttrId;
    public static final String csrFormAttrId = "csrFormAttr";
    public static final String newCertificateAttrId = "newCertificateAttr";

    public static final String errorResponseMsgAttrId = "errorResponseMsgAttr";
    private static final String LOMMEBOK_11_REGISTER_CSR_REQUEST = "LOMMEBOK-11-REGISTER-CSR-REQUEST";

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/csr/{id}")
    public ModelAndView registerCsrGet(@PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        return new ModelAndView("csr_form_view", Map.of(
            csrFormAttrId, CsrForm.empty(),
            detailedViewDataAttrId, relyingPartyResource));
    }

    @Audit(auditId = LOMMEBOK_11_REGISTER_CSR_REQUEST, includeResult = false)
    @PostMapping("/csr/{id}")
    public ModelAndView registerCsrPost(
        @PathVariable("id") @Valid UUID id,
        @AuditIgnore @ModelAttribute(csrFormAttrId) @Valid CsrForm csrForm,
        @AuditIgnore BindingResult csrFormBindingResult
    ) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        ModelAndView mav =
            new ModelAndView("csr_form_view", Map.of(detailedViewDataAttrId, relyingPartyResource));

        if (!csrFormBindingResult.hasErrors()) {
            try {
                RelyingPartyCsrResource csrResource =
                    new RelyingPartyCsrResource(csrForm.getCsr());
                RelyingPartyAccessCertificateResource certResource =
                    relyingPartiesService.requestCertificateForRelyingParty(id, csrResource);

                mav.setViewName("csr_submit_success_view");
                mav.addObject(newCertificateAttrId, certResource.toSummary());
                return mav;
            }
            catch (ErrorResponseException e) {
                log.info("CSR registration rejected for id={}", id, e);
                mav.addObject(errorResponseMsgAttrId, "exception.error_response");
            }
        }

        return mav;
    }
}
