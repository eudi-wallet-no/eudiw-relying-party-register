package no.idporten.eudiw.rp.register.lookup.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.search.resultsview.RelyingPartyOrdering;
import no.idporten.eudiw.rp.register.lookup.web.search.resultsview.SearchSession;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String detailedViewDataAttrId = "detailedViewDataAttr";

    private final SearchSession searchSession;

    @GetMapping("/")
    public ModelAndView searchGet(
        @RequestParam(value = "page") Optional<Integer> oneIndexedPageNum,
        @RequestParam(value = "sort") Optional<RelyingPartyOrdering> ordering) {

        if (!searchSession.isInitialized()) {
                searchSession.doFreshSearch(SearchForm.empty());
        }

        oneIndexedPageNum.ifPresent(i -> searchSession.setCurrentPageIdx(i - 1));
        ordering.ifPresent(searchSession::setOrdering);

        SearchForm lastSearchForm = searchSession.getLastSearchForm();
        return new ModelAndView("search_view", searchFormAttrId, lastSearchForm);
    }

    @PostMapping("/")
    public ModelAndView searchPost(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult) {

        if (!bindingResult.hasErrors()) {
            searchSession.doFreshSearch(searchForm);
        }

        return new ModelAndView("search_view", searchFormAttrId, searchForm);
    }
}
