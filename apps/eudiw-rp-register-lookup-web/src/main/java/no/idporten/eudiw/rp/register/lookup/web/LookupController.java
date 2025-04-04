package no.idporten.eudiw.rp.register.lookup.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.LookupService;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
    public static final String focusResultAttrId = "relyingPartyAttr";

    @ModelAttribute(searchResultAttrId)
    private HashMap<UUID, RelyingPartyResource> initSearchResults() {
        return new HashMap<>();
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
        if (!bindingResult.hasErrors()) {
            SearchRelyingPartyResource searchResource = searchForm.toResource();
            RelyingPartiesResource searchResult = lookupService.search(searchResource);
            model.addAttribute(searchResultAttrId, searchResult.toMap());
        }
        return "search";
    }

    @GetMapping("/getAll")
    public String getAll(Model model) {
        RelyingPartiesResource allRelyingParties = lookupService.getAll();
        model.addAttribute(searchResultAttrId, allRelyingParties.toMap());
        return "redirect:/";
    }

    @GetMapping("/details/{id}")
    public String detailedView(@PathVariable("id") UUID id,
                               @ModelAttribute(searchResultAttrId)
                               HashMap<UUID, RelyingPartyResource> searchResults,
                               Model model) {
        RelyingPartyResource focusResult = searchResults.get(id);
        if (focusResult == null) // if user navigated here without searching first.
            return "errors/id_not_found";
        model.addAttribute(focusResultAttrId, focusResult);
        return "detailed_view";
    }
}
