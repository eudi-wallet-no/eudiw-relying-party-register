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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping
    public String searchGet(HttpSession session, Model model) {
        SearchForm existingSearchForm = (SearchForm) session.getAttribute(searchFormAttrId);
        model.addAttribute(searchFormAttrId,
                           existingSearchForm != null
                               ? existingSearchForm
                               : SearchForm.empty());
        ResultsViewSpecificationForm existingViewSpecForm =
            (ResultsViewSpecificationForm) session.getAttribute(viewSpecFormAttrId);
        model.addAttribute(viewSpecFormAttrId,
                           existingViewSpecForm != null
                               ? existingViewSpecForm
                               : ResultsViewSpecificationForm.empty());
        return "search";
    }

    @PostMapping("/search")
    public String searchPost(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult,
        Model model,
        HttpSession session
    ) {
        if (!bindingResult.hasErrors() && !searchForm.isEmpty()) {
            RelyingPartiesView searchResultsView =
                RelyingPartiesView.fromResource(lookupService.search(searchForm.toResource()));
            model.addAttribute(fullResultsAttrId, searchResultsView);
            session.setAttribute(searchFormAttrId, searchForm);
        }
        return "redirect:/";
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
        @ModelAttribute(fullResultsAttrId) RelyingPartiesView searchResult,
        BindingResult bindingResult,
        RedirectAttributes redirectAttrs
    ) {
        if (!bindingResult.hasErrors() && searchResult.exists(id))
            redirectAttrs.addFlashAttribute(detailedViewDataAttrId, searchResult.get(id));
        return "redirect:/";
    }

    @GetMapping("/getAll")
    public String getAll(Model model) {
        RelyingPartiesView allRelyingPartiesView =
            RelyingPartiesView.fromResource(lookupService.getAll());
        model.addAttribute(fullResultsAttrId, allRelyingPartiesView);
        return "redirect:/";
    }
}
