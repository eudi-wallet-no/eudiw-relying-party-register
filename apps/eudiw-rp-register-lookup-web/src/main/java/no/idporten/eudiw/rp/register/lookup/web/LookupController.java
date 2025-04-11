package no.idporten.eudiw.rp.register.lookup.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.LookupService;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/")
@SessionAttributes(LookupController.searchResultAttrId)
public class LookupController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String searchResultAttrId = "resultsAttr";
    public static final String detailedViewDataAttrId = "detailedViewDataAttr";

    @ModelAttribute(searchResultAttrId)
    private RelyingParties initSearchResults() {
        return new RelyingParties(new HashMap<>());
    }

    private final LookupService lookupService;

    @GetMapping
    public String searchGet(Model model) {
        model.addAttribute(searchFormAttrId, SearchForm.empty());
        return "search";
    }

    @PostMapping
    public String searchPost(@ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
                             BindingResult bindingResult,
                             Model model) {
        if (!bindingResult.hasErrors() && !searchForm.isEmpty()) {
            RelyingParties searchResult =
                RelyingParties.fromResource(lookupService.search(searchForm.toResource()));
            model.addAttribute(searchResultAttrId, searchResult);
        }
        return "search";
    }

    @GetMapping("/details/{id}")
    public String detailedView(@PathVariable("id") UUID id,
                               @ModelAttribute(searchResultAttrId) RelyingParties searchResult,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttrs) {
        if (!bindingResult.hasErrors() && searchResult.exists(id))
            redirectAttrs.addFlashAttribute(detailedViewDataAttrId, searchResult.get(id));
        return "redirect:/";
    }

    @GetMapping("/getAll")
    public String getAll(RedirectAttributes redirectAttrs) {
        RelyingParties allRelyingParties =
            RelyingParties.fromResource(lookupService.getAll());
        redirectAttrs.addFlashAttribute(searchResultAttrId, allRelyingParties);
        return "redirect:/";
    }
}
