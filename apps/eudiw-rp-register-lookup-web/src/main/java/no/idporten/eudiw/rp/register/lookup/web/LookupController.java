package no.idporten.eudiw.rp.register.lookup.web;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.LookupService;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/")
@SessionAttributes(LookupController.searchResultAttrId)
public class LookupController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String searchResultAttrId = "resultsAttr";
    public static final String focusResultAttrId = "relyingPartyAttr";

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping
    public String searchGet(Model model) {
        model.addAttribute(searchFormAttrId, SearchForm.empty());
        return "search";
    }

    @PostMapping
    public String searchPost(@ModelAttribute(searchFormAttrId) SearchForm searchForm,
                             Model model) {
        SearchRelyingPartyResource searchResource = searchForm.toResource();
        RelyingPartiesResource searchResult = lookupService.search(searchResource);
        model.addAttribute(searchResultAttrId, searchResult.toMap());
        return "search";
    }

    @GetMapping("/details/{id}")
    public String detailedView(@PathVariable("id") UUID id,
                               @ModelAttribute(searchResultAttrId)
                               Map<UUID, RelyingPartyResource> searchResults,
                               Model model) {
        RelyingPartyResource focusResult = searchResults.get(id);
        if (focusResult == null)
            return "redirect:/";
        model.addAttribute(focusResultAttrId, focusResult);
        return "detailed_view";
    }
}
