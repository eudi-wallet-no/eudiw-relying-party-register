package no.idporten.eudiw.rp.register.lookup.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.web.resource.PagedResponse;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GlobalSearchController {

    public static final String searchTermAttrId = "searchTermAttr";
    public static final String relyingPartiesAttrId = "relyingPartiesAttr";
    public static final String credentialsAttrId = "credentialsAttr";
    public static final String totalHitsAttrId = "totalHitsAttr";

    private static final int MAX_RELYING_PARTY_RESULTS = 25;

    private final RelyingPartiesService relyingPartiesService;
    private final CredentialsService credentialsService;

    @GetMapping("/search")
    public ModelAndView search(@RequestParam(value = "searchTerm", required = false) String searchTerm) {
        String normalizedTerm = searchTerm == null ? "" : searchTerm.strip();

        List<RelyingPartyResource> relyingParties;
        List<CredentialResource> credentials;
        if (normalizedTerm.isEmpty()) {
            relyingParties = List.of();
            credentials = List.of();
        } else {
            relyingParties = searchRelyingParties(normalizedTerm);
            credentials = searchCredentials(normalizedTerm);
        }

        ModelAndView mav = new ModelAndView("global_search_view");
        mav.addObject(searchTermAttrId, normalizedTerm);
        mav.addObject(relyingPartiesAttrId, relyingParties);
        mav.addObject(credentialsAttrId, credentials);
        mav.addObject(totalHitsAttrId, relyingParties.size() + credentials.size());
        return mav;
    }

    /**
     * Søk etter brukarstader i registeret. Returnerer tom liste dersom
     * registeret er utilgjengeleg, slik at resultatsida likevel renderes.
     */
    private List<RelyingPartyResource> searchRelyingParties(String searchTerm) {
        try {
            PagedResponse<RelyingPartyResource> page =
                relyingPartiesService.search(new SearchRelyingPartyResource()
                    .withSearchTerm(searchTerm)
                    .withPage(0)
                    .withPageSize(MAX_RELYING_PARTY_RESULTS));
            return page.content();
        } catch (Exception e) {
            log.warn("Could not search relying parties for global search", e);
            return List.of();
        }
    }

    /**
     * Søk etter bevisstypar i sandkassa. Returnerer tom liste dersom
     * bevisregisteret er utilgjengeleg, slik at resultatsida likevel renderes.
     */
    private List<CredentialResource> searchCredentials(String searchTerm) {
        try {
            return credentialsService.search(searchTerm).credentials();
        } catch (Exception e) {
            log.warn("Could not search credentials for global search", e);
            return List.of();
        }
    }
}