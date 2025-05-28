package no.idporten.eudiw.rp.admin.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.*;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartiesView;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyAccessCertificateSummary;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
@SessionAttributes({SearchController.fullResultsAttrId})
public class SearchController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String fullResultsAttrId = "fullResultsAttr";

    public static final String detailedViewDataAttrId = "detailedViewDataAttr";
    public static final String certificateSummariesAttrId = "certificateSummariesAttr";

    public static final String csrFormAttrId = "csrFormAttr";
    public static final String newCertificateAttrId = "newCertificateAttr";

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/search")
    public ModelAndView searchGet(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult) {
        ModelAndView mav = new ModelAndView("search_view");

        searchForm = searchForm != null ? searchForm : SearchForm.empty();
        mav.addObject(searchFormAttrId, searchForm);

        if (!bindingResult.hasErrors() && !searchForm.searchTerm().isEmpty()) {
            RelyingPartiesView searchResultsView =
                RelyingPartiesView.fromResource(
                    relyingPartiesService.search(searchForm.toResource()));
            mav.addObject(fullResultsAttrId, searchResultsView);
        }
        return mav;
    }

    @GetMapping("/details")
    public ModelAndView detailsGet(
        @RequestParam("id") @Valid UUID id,
        @ModelAttribute(fullResultsAttrId) RelyingPartiesView searchResults) {
        RelyingPartyResource relyingPartyResource =
            searchResults.exists(id)
                ? searchResults.get(id)
                : relyingPartiesService.get(id);
        List<RelyingPartyAccessCertificateSummary> certificatesResource =
            relyingPartiesService.getCertificatesForRelyingParty(id)
                .toSummaries();
        return new ModelAndView(
            "details_view",
            Map.of(detailedViewDataAttrId, relyingPartyResource,
                   certificateSummariesAttrId, certificatesResource));
    }

    @GetMapping("/registerCsr")
    public ModelAndView registerCsrGet(
        @RequestParam("id") @Valid UUID id,
        @ModelAttribute(fullResultsAttrId) RelyingPartiesView searchResults
    ) {
        RelyingPartyResource relyingPartyResource =
            searchResults.exists(id)
                ? searchResults.get(id)
                : relyingPartiesService.get(id);
        return new ModelAndView("csr_form_view", Map.of(
            csrFormAttrId, CsrForm.empty(),
            detailedViewDataAttrId, relyingPartyResource));
    }

    @PostMapping("/registerCsr")
    public ModelAndView registerCsrPost(
        @RequestParam("id") @Valid UUID id,
        @ModelAttribute(fullResultsAttrId) RelyingPartiesView searchResults,
        @ModelAttribute(csrFormAttrId) @Valid CsrForm csrForm,
        BindingResult csrFormBindingResult
    ) {
        RelyingPartyResource relyingPartyResource =
            searchResults.exists(id)
                ? searchResults.get(id)
                : relyingPartiesService.get(id);
        ModelAndView mav =
            new ModelAndView("csr_form_view", Map.of(detailedViewDataAttrId, relyingPartyResource));

        // if given CSR is invalid
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
