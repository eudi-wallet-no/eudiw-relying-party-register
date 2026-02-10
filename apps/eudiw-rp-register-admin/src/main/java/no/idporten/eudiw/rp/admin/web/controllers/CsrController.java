package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.web.form.CsrAccessForm;
import no.idporten.eudiw.rp.admin.web.form.CsrIssuerForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummaryBuilder;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.logging.audit.Audit;
import no.idporten.logging.audit.AuditIgnore;
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
public class CsrController {

    public static final String DETAILED_VIEW_DATA_ATTR = SearchController.DETAILED_VIEW_DATA_ATTR;
    public static final String CSR_FORM_ATTR = "csrFormAttr";
    public static final String NEW_CERTIFICATE_ATTR = "newCertificateAttr";

    public static final String ERROR_RESPONSE_MSG_ATTR = "errorResponseMsgAttr";

    private static final String LOMMEBOK_11_REGISTER_CSR_REQUEST = "LOMMEBOK-11-REGISTER-CSR-REQUEST";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;
    private final RelyingPartyCertificateSummaryBuilder relyingPartyCertificateSummaryBuilder;

    @GetMapping("/csr/access/{id}")
    public ModelAndView registerAccessCsrGet(@PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(relyingPartyResource.orgno());

        return new ModelAndView("access_csr_form_view", Map.of(
            CSR_FORM_ATTR, CsrAccessForm.empty(),
            DETAILED_VIEW_DATA_ATTR, relyingPartyResource));
    }

    @Audit(auditId = LOMMEBOK_11_REGISTER_CSR_REQUEST, includeResult = false)
    @PostMapping("/csr/access/{id}")
    public ModelAndView registerAccessCsrPost(
        @PathVariable("id") @Valid UUID id,
        @AuditIgnore @ModelAttribute(CSR_FORM_ATTR) @Valid CsrAccessForm csrAccessForm,
        @AuditIgnore BindingResult csrFormBindingResult
    ) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(relyingPartyResource.orgno());

        ModelAndView mav =
            new ModelAndView("access_csr_form_view", Map.of(DETAILED_VIEW_DATA_ATTR, relyingPartyResource));

        if (!csrFormBindingResult.hasErrors()) {
            try {
                RelyingPartyCsrResource csrResource =
                    new RelyingPartyCsrResource(csrAccessForm.getCsr());
                RelyingPartyCertificateResource certResource =
                    relyingPartiesService.requestCertificateForRelyingParty(id, csrResource);

                mav.setViewName("csr_submit_success_view");
                mav.addObject(NEW_CERTIFICATE_ATTR, relyingPartyCertificateSummaryBuilder.build(certResource));
                return mav;
            }
            catch (ErrorResponseException e) {
                log.info("CSR registration rejected for id={}", id, e);
                mav.addObject(ERROR_RESPONSE_MSG_ATTR, "exception.error_response");
            }
        }

        return mav;
    }

    @GetMapping("/csr/issuer/{id}")
    public ModelAndView registerIssuerCsrGet(@PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(relyingPartyResource.orgno());

        return new ModelAndView("issuer_csr_form_view", Map.of(
            CSR_FORM_ATTR, CsrIssuerForm.empty(),
            DETAILED_VIEW_DATA_ATTR, relyingPartyResource));
    }

    @Audit(auditId = LOMMEBOK_11_REGISTER_CSR_REQUEST, includeResult = false)
    @PostMapping("/csr/issuer/{id}")
    public ModelAndView registerIssuerCsrPost(
        @PathVariable("id") @Valid UUID id,
        @AuditIgnore @ModelAttribute(CSR_FORM_ATTR) @Valid CsrIssuerForm csrIssuerForm,
        @AuditIgnore BindingResult csrFormBindingResult
    ) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(relyingPartyResource.orgno());

        ModelAndView mav =
            new ModelAndView("issuer_csr_form_view", Map.of(DETAILED_VIEW_DATA_ATTR, relyingPartyResource));

        if (!csrFormBindingResult.hasErrors()) {
            try {
                IssuerCsrResource csrResource = new IssuerCsrResource(csrIssuerForm.getCsr(), csrIssuerForm.getEntitlement());
                RelyingPartyCertificateResource certResource =
                    relyingPartiesService.requestIssuerCertificateForEntitlement(id, csrResource);

                mav.setViewName("csr_issuer_submit_success_view");
                mav.addObject(NEW_CERTIFICATE_ATTR, relyingPartyCertificateSummaryBuilder.build(certResource));
                return mav;
            }
            catch (ErrorResponseException e) {
                log.info("CSR registration rejected for id={}", id, e);
                mav.addObject(ERROR_RESPONSE_MSG_ATTR, "exception.error_response");
            }
        }

        return mav;
    }
}
