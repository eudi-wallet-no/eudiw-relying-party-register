package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.web.SearchSession;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchController {

    public static final String SEARCH_FORM_ATTR = "searchFormAttr";
    public static final String DETAILED_VIEW_DATA_ATTR = "detailedViewDataAttr";
    public static final String RESULTS_PAGE_ATTR = "resultsPageAttr";

    private final SearchSession searchSession;

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView searchGet(
        @RequestParam(value = "page") Optional<Integer> oneIndexedPageNum,
        @RequestParam(value = "sort") Optional<String> ordering) {

        oneIndexedPageNum.ifPresent(i -> searchSession.setCurrentPageIdx(i - 1));
        ordering.ifPresent(searchSession::setOrdering);

        List<RelyingPartyResource> resultsPage = searchSession.refreshSearch();
        return new ModelAndView("search_view", Map.of(
            RESULTS_PAGE_ATTR, resultsPage,
            SEARCH_FORM_ATTR, searchSession.getLastSearchForm()));
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView searchPost(
        @ModelAttribute(SEARCH_FORM_ATTR) @Valid SearchForm searchForm,
        BindingResult bindingResult) {

        ModelAndView mav = new ModelAndView("search_view", SEARCH_FORM_ATTR, searchForm);
        if (!bindingResult.hasErrors()) {
            List<RelyingPartyResource> resultsPage = searchSession.doFreshSearch(searchForm);
            mav.addObject(RESULTS_PAGE_ATTR, resultsPage);
        }
        return mav;
    }
}
