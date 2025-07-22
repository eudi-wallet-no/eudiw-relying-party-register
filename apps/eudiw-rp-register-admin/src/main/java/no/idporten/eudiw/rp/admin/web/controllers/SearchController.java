package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartiesViewOrdering;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartiesView;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String detailedViewDataAttrId = "detailedViewDataAttr";

    private final RelyingPartiesView relyingPartiesView;

    @GetMapping("/search")
    public ModelAndView searchGet(
        @RequestParam(value = "page") Optional<Integer> oneIndexedPageNum,
        @RequestParam(value = "sort") Optional<RelyingPartiesViewOrdering> ordering) {

        relyingPartiesView.setCurrentPageIdx(oneIndexedPageNum.orElse(1) - 1);
        ordering.ifPresent(relyingPartiesView::setOrdering);

        SearchForm lastSearchForm = relyingPartiesView.getLastSearchForm();
        return new ModelAndView("search_view", searchFormAttrId, lastSearchForm);
    }

    @PostMapping("/search")
    public ModelAndView searchPost(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult) {

        if (!bindingResult.hasErrors() && !searchForm.searchTerm().isEmpty()) {
            relyingPartiesView.doSearch(searchForm.toResource());
            relyingPartiesView.setLastSearchForm(searchForm);
        }

        return new ModelAndView("search_view", searchFormAttrId, searchForm);
    }
}
