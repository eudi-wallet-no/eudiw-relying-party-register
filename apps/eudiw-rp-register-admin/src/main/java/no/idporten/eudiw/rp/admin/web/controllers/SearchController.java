package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchController {

    public static final String SEARCH_FORM_ATTR = "searchFormAttr";
    public static final String DETAILED_VIEW_DATA_ATTR = "detailedViewDataAttr";
    public static final String RESULTS_PAGE_ATTR = "resultsPageAttr";
    public static final String ENTITLEMENT_OPTIONS_ATTR = "entitlementOptionsAttr";

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView searchGet(
        @ModelAttribute(SEARCH_FORM_ATTR) @Valid SearchForm searchForm,
        BindingResult bindingResult,
        @RequestParam(value = "page", defaultValue = "1") int oneIndexedPageNum,
        @RequestParam(value = "sort", defaultValue = SearchRelyingPartyResource.DEFAULT_ORDERING) String ordering) {

        ModelAndView mav = new ModelAndView("search_view", SEARCH_FORM_ATTR, searchForm);
        mav.addObject("orderingAttr", ordering);
        if (bindingResult.hasErrors()) {
            mav.addObject(RESULTS_PAGE_ATTR, List.of());
            mav.addObject("numPagesAttr", 0);
            mav.addObject("currentPageNumAttr", 1);
            mav.addObject("paginationWindowAttr", new int[0]);
        } else {
            var searchResult = relyingPartiesService.search(searchForm.toResource()
                .withPage(Math.max(oneIndexedPageNum, 1) - 1)
                .withPageSize(SearchRelyingPartyResource.DEFAULT_PAGE_SIZE)
                .withOrdering(ordering));

            int numPages = Math.toIntExact(searchResult.page().totalPages());
            int currentPageNum = Math.toIntExact(searchResult.page().number()) + 1;
            mav.addObject(RESULTS_PAGE_ATTR, searchResult.content());
            mav.addObject("numPagesAttr", numPages);
            mav.addObject("currentPageNumAttr", currentPageNum);
            mav.addObject("paginationWindowAttr", paginationWindow(currentPageNum, numPages));
        }
        mav.addObject(ENTITLEMENT_OPTIONS_ATTR, relyingPartiesService.getValidEntitlementOptions());
        return mav;
    }

    private int[] paginationWindow(int currentPageNum, int numPages) {
        int windowSize = Math.min(5, numPages);
        int windowRadius = windowSize / 2;
        int lo = currentPageNum + windowRadius <= numPages
            ? Math.max(1, currentPageNum - windowRadius)
            : numPages - windowSize + 1;
        return IntStream.range(lo, lo + windowSize).toArray();
    }
}
