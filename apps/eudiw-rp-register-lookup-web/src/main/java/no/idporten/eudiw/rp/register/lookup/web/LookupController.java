package no.idporten.eudiw.rp.register.lookup.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.LookupService;
import no.idporten.eudiw.rp.register.lookup.web.searchresults.viewing.ResultsViewSpecification;
import no.idporten.eudiw.rp.register.lookup.web.searchresults.ResultsViewSpecificationForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.searchresults.RelyingPartiesView;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/")
@SessionAttributes({LookupController.fullResultsAttrId, LookupController.detailedViewDataAttrId})
public class LookupController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String viewSpecFormAttrId = "viewSpecFormAttr";
    public static final String fullResultsAttrId = "fullResultsAttr";
    public static final String detailedViewDataAttrId = "detailedViewDataAttr";

    private final LookupService lookupService;

    @GetMapping("/")
    public ModelAndView searchGet(HttpSession session) {
        return defaultModelAndSearchView(session);
    }

    @PostMapping("/")
    public ModelAndView searchPost(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult,
        HttpSession session
    ) {
        session.removeAttribute(detailedViewDataAttrId);
        session.removeAttribute(viewSpecFormAttrId);

        ModelAndView mav = defaultModelAndSearchView(session);
        mav.addObject(searchFormAttrId, searchForm);
        session.setAttribute(searchFormAttrId, searchForm);

        RelyingPartiesView searchResultsView =
            !bindingResult.hasErrors() ? doSearch(searchForm) : null;
        mav.addObject(fullResultsAttrId, searchResultsView);
        return mav;
    }

    @PostMapping("/view")
    public String setView(
        @ModelAttribute(fullResultsAttrId) RelyingPartiesView searchResult,
        @ModelAttribute(viewSpecFormAttrId) ResultsViewSpecificationForm viewSpecForm,
        HttpSession session
    ) {
        session.setAttribute(viewSpecFormAttrId, viewSpecForm);
        searchResult.setViewSpec(ResultsViewSpecification.fromForm(viewSpecForm));
        return "redirect:/";
    }

    @GetMapping("/details/{id}")
    public String detailedView(
        @PathVariable("id") UUID id,
        @ModelAttribute(fullResultsAttrId) RelyingPartiesView searchResults,
        RedirectAttributes redirectAttrs
    ) {
        if (searchResults.exists(id)) {
            redirectAttrs.addFlashAttribute(detailedViewDataAttrId, searchResults.get(id));
        }
        return "redirect:/";
    }

    @GetMapping("/getAll")
    public String getAll(SessionStatus status, RedirectAttributes redirectAttrs) {
        status.setComplete();
        RelyingPartiesView allRelyingPartiesView = RelyingPartiesView.fromResource(lookupService.getAll());
        redirectAttrs.addFlashAttribute(fullResultsAttrId, allRelyingPartiesView);
        return "redirect:/";
    }

    private ModelAndView defaultModelAndSearchView(HttpSession session) {
        Map<String, Object> formAttrs = Map.of(
            searchFormAttrId,   Objects.requireNonNullElse(session.getAttribute(searchFormAttrId),
                                                           SearchForm.empty()),
            viewSpecFormAttrId, Objects.requireNonNullElse(session.getAttribute(viewSpecFormAttrId),
                                                           ResultsViewSpecificationForm.empty())
        );
        return new ModelAndView("search", formAttrs);
    }

    private RelyingPartiesView doSearch(SearchForm searchForm) {
        return RelyingPartiesView.fromResource(
            lookupService.search(searchForm.toResource()));
    }
}
