package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.web.form.CsrIssuerForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.form.CsrAccessForm;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/csr/access/{id}")
    @PreAuthorize("@authorizationService.userHasPrivilegedAccessTo(#id)")
    public ModelAndView registerAccessCsrGet(@PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        return new ModelAndView("access_csr_form_view", Map.of(
            csrFormAttrId, CsrAccessForm.empty(),
            detailedViewDataAttrId, relyingPartyResource));
    }

    @Audit(auditId = LOMMEBOK_11_REGISTER_CSR_REQUEST, includeResult = false)
    @PostMapping("/csr/access/{id}")
    @PreAuthorize("@authorizationService.userHasPrivilegedAccessTo(#id)")
    public ModelAndView registerAccessCsrPost(
        @PathVariable("id") @Valid UUID id,
        @AuditIgnore @ModelAttribute(csrFormAttrId) @Valid CsrAccessForm csrAccessForm,
        @AuditIgnore BindingResult csrFormBindingResult
    ) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        ModelAndView mav =
            new ModelAndView("access_csr_form_view", Map.of(detailedViewDataAttrId, relyingPartyResource));

        if (!csrFormBindingResult.hasErrors()) {
            try {
                RelyingPartyCsrResource csrResource =
                    new RelyingPartyCsrResource(csrAccessForm.getCsr());
                RelyingPartyCertificateResource certResource =
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

    @GetMapping("/csr/issuer/{id}")
    @PreAuthorize("@authorizationService.userHasPrivilegedAccessTo(#id)")
    public ModelAndView registerIssuerCsrGet(@PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        return new ModelAndView("issuer_csr_form_view", Map.of(
            csrFormAttrId, CsrIssuerForm.empty(),
            detailedViewDataAttrId, relyingPartyResource));
    }

    @Audit(auditId = LOMMEBOK_11_REGISTER_CSR_REQUEST, includeResult = false)
    @PostMapping("/csr/issuer/{id}")
    @PreAuthorize("@authorizationService.userHasPrivilegedAccessTo(#id)")
    public ModelAndView registerIssuerCsrPost(
        @PathVariable("id") @Valid UUID id,
        @AuditIgnore @ModelAttribute(csrFormAttrId) @Valid CsrIssuerForm csrIssuerForm,
        @AuditIgnore BindingResult csrFormBindingResult
    ) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        ModelAndView mav =
            new ModelAndView("issuer_csr_form_view", Map.of(detailedViewDataAttrId, relyingPartyResource));

        if (!csrFormBindingResult.hasErrors()) {
            try {
                IssuerCsrResource csrResource = new IssuerCsrResource(csrIssuerForm.getCsr(), csrIssuerForm.getEntitlement());
                RelyingPartyCertificateResource certResource =
                    relyingPartiesService.requestIssuerCertificateForEntitlement(id, csrResource);

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
