package no.idporten.eudiw.rp.register.lookup.web;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.LookupService;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/")
public class LookupController {

    public static final String searchFormAttrId = "searchFormObject";
    public static final String searchResultAttrId = "searchResultObject";

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping
    public String search(Model model) {
        model.addAttribute(searchFormAttrId, SearchForm.empty());
        return "search";
    }

    @PostMapping
    public String search(@ModelAttribute(searchFormAttrId) SearchForm searchForm,
                         Model model) {
        SearchRelyingPartyResource searchResource = searchForm.toResource();
        List<RelyingPartyResource> searchResult =
            lookupService.search(searchResource).relyingParties();
        model.addAttribute(searchResultAttrId, searchResult);
        return "search";
    }
}
