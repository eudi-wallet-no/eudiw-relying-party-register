package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.CsrForm;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyCsrResource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/registerCsr")
    public ModelAndView registerCsrGet(@RequestParam("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        return new ModelAndView("csr_form_view", Map.of(
            csrFormAttrId, CsrForm.empty(),
            detailedViewDataAttrId, relyingPartyResource));
    }

    @PostMapping("/registerCsr")
    public ModelAndView registerCsrPost(
        @RequestParam("id") @Valid UUID id,
        @ModelAttribute(csrFormAttrId) @Valid CsrForm csrForm,
        BindingResult csrFormBindingResult) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        ModelAndView mav =
            new ModelAndView("csr_form_view", Map.of(detailedViewDataAttrId, relyingPartyResource));

        if (!csrFormBindingResult.hasErrors()) {
            RelyingPartyCsrResource csrResource =
                new RelyingPartyCsrResource(csrForm.csr());
            RelyingPartyAccessCertificateResource certResource =
                relyingPartiesService.requestCertificateForRelyingParty(id, csrResource);

            mav.setViewName("csr_submit_success_view");
            mav.addObject(newCertificateAttrId, certResource.toSummary());
        }
        return mav;
    }
}
