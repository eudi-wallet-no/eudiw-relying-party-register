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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/")
public class LookupController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String searchResultAttrId = "resultsAttr";

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
    public String getAll(RedirectAttributes redirectAttrs) {
        RelyingPartiesResource allRelyingParties = lookupService.getAll();
        redirectAttrs.addFlashAttribute(searchResultAttrId, allRelyingParties.toMap());
        return "redirect:/";
    }
}
