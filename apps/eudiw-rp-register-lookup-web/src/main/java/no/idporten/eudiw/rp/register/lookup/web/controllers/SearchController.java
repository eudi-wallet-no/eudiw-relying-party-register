package no.idporten.eudiw.rp.register.lookup.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.PagedResponse;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String resultsPageAttrId = "resultsPageAttr";
    public static final String credentialsByTypeAttrId = "credentialsByTypeAttr";
    public static final String numPagesAttrId = "numPagesAttr";
    public static final String currentPageNumAttrId = "currentPageNumAttr";
    public static final String totalElementsAttrId = "totalElementsAttr";
    public static final String orderingAttrId = "orderingAttr";
    public static final String paginationWindowAttrId = "paginationWindowAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final CredentialsService credentialsService;

    @GetMapping("/relying-parties")
    public ModelAndView searchGet(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult,
        @RequestParam(value = "page", defaultValue = "1") int oneIndexedPageNum,
        @RequestParam(value = "sort", defaultValue = SearchRelyingPartyResource.DEFAULT_ORDERING) String ordering) {

        ModelAndView mav = new ModelAndView("search_view", searchFormAttrId, searchForm);
        mav.addObject(credentialsByTypeAttrId, credentialsByType());

        if (bindingResult.hasErrors()) {
            mav.addObject(resultsPageAttrId, List.of());
            mav.addObject(numPagesAttrId, 0);
            mav.addObject(currentPageNumAttrId, 1);
            mav.addObject(totalElementsAttrId, 0L);
            mav.addObject(orderingAttrId, ordering);
            mav.addObject(paginationWindowAttrId, new int[0]);
            return mav;
        }

        int pageIdx = Math.max(oneIndexedPageNum - 1, 0);
        PagedResponse<RelyingPartyResource> searchResult =
            relyingPartiesService.search(
                searchForm.toResource()
                    .withPage(pageIdx)
                    .withPageSize(SearchRelyingPartyResource.DEFAULT_PAGE_SIZE)
                    .withOrdering(ordering));

        int numPages = Math.toIntExact(searchResult.page().totalPages());
        int currentPageNum = Math.toIntExact(searchResult.page().number()) + 1;

        mav.addObject(resultsPageAttrId, searchResult.content());
        mav.addObject(numPagesAttrId, numPages);
        mav.addObject(currentPageNumAttrId, currentPageNum);
        mav.addObject(totalElementsAttrId, searchResult.page().totalElements());
        mav.addObject(orderingAttrId, ordering);
        mav.addObject(paginationWindowAttrId, paginationWindow(currentPageNum, numPages));
        return mav;
    }

    private int[] paginationWindow(int currentPageNum, int numPages) {
        int windowSize = Math.min(5, numPages);
        int windowRadius = windowSize / 2;
        int lo = currentPageNum + windowRadius <= numPages
                     ? Math.max(1, currentPageNum - windowRadius)
                     : numPages - windowSize + 1;
        int hi = lo + windowSize;
        return IntStream.range(lo, hi).toArray();
    }

    private Map<String, CredentialResource> credentialsByType() {
        return credentialsService.getAvailableCredentials()
                                 .credentials()
                                 .stream()
                                 .collect(Collectors.toMap(
                                     CredentialResource::getCredentialType,
                                     Function.identity(),
                                     (first, _) -> first));
    }
}
