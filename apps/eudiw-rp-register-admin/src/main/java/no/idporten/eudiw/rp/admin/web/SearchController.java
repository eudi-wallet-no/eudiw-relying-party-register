package no.idporten.eudiw.rp.admin.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.SearchForm;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartiesView;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyAccessCertificateSummary;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@SessionAttributes({SearchController.fullResultsAttrId})
public class SearchController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String fullResultsAttrId = "fullResultsAttr";
    public static final String detailedViewDataAttrId = "detailedViewDataAttr";
    public static final String certificateSummariesAttrId = "certificateSummariesAttr";

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/search")
    public ModelAndView searchGet(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult) {

        ModelAndView mav = new ModelAndView("search");
        mav.addObject(searchFormAttrId, searchForm);
        if (!bindingResult.hasErrors()) {
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
        if (searchResults.exists(id)) {
            RelyingPartyResource relyingPartyResource = searchResults.get(id);
            List<RelyingPartyAccessCertificateSummary> certificatesResource =
                relyingPartiesService.getCertificatesForRelyingParty(id)
                    .toSummaries();
            return new ModelAndView(
                "detailed_view",
                Map.of(detailedViewDataAttrId, relyingPartyResource,
                       certificateSummariesAttrId, certificatesResource));
        }

        return new ModelAndView("redirect:/search");
    }
}
