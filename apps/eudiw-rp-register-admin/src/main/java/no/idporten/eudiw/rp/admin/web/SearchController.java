package no.idporten.eudiw.rp.admin.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.SearchForm;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartiesView;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@SessionAttributes({SearchController.fullResultsAttrId})
public class SearchController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String fullResultsAttrId = "fullResultsAttr";
    public static final String detailedViewDataAttrId = "detailedViewDataAttr";

    private final RelyingPartiesService relyingPartiesService;

    @ModelAttribute(fullResultsAttrId)
    public RelyingPartiesView initFullResultsAttr() {
        return RelyingPartiesView.empty();
    }

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
        @RequestParam("id") @Valid String idStr,
        @ModelAttribute(fullResultsAttrId) RelyingPartiesView searchResults) {
        try {
            UUID id = UUID.fromString(idStr);
            if (searchResults.exists(id)) {
                RelyingPartyResource relyingPartyResource = searchResults.get(id);
                return new ModelAndView(
                    "detailed_view",
                    Map.of(detailedViewDataAttrId, relyingPartyResource));
            }
        } catch (Exception _) { }

        // RP does not exist *or* id is not a valid UUID string.
        return new ModelAndView("redirect:/search");
    }
}
