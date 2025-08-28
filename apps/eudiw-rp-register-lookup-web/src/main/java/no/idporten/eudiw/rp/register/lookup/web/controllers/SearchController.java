package no.idporten.eudiw.rp.register.lookup.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.search.resultsview.RelyingPartiesView;
import no.idporten.eudiw.rp.register.lookup.web.search.resultsview.RelyingPartiesViewOrdering;
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

    private final RelyingPartiesView relyingPartiesView;

    @GetMapping("/")
    public ModelAndView searchGet(
        @RequestParam(value = "page") Optional<Integer> oneIndexedPageNum,
        @RequestParam(value = "sort") Optional<RelyingPartiesViewOrdering> ordering) {

        if (!relyingPartiesView.isInitialized()) {
            relyingPartiesView.doSearch(SearchForm.empty().toResource());
            relyingPartiesView.setLastSearchForm(SearchForm.empty());
        }

        relyingPartiesView.setCurrentPageIdx(oneIndexedPageNum.orElse(1) - 1);
        ordering.ifPresent(relyingPartiesView::setOrdering);

        SearchForm lastSearchForm = relyingPartiesView.getLastSearchForm();
        return new ModelAndView("search_view", searchFormAttrId, lastSearchForm);
    }

    @PostMapping("/")
    public ModelAndView searchPost(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult) {

        if (!bindingResult.hasErrors()) {
            relyingPartiesView.doSearch(searchForm.toResource());
            relyingPartiesView.setLastSearchForm(searchForm);
        }

        return new ModelAndView("search_view", searchFormAttrId, searchForm);
    }
}
