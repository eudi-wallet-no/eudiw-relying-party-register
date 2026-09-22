package no.idporten.eudiw.rp.register.lookup.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import no.idporten.eudiw.rp.register.lookup.web.search.resultsview.SearchSession;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchController {

    public static final String searchFormAttrId = "searchFormAttr";
    public static final String resultsPageAttrId = "resultsPageAttr";
    public static final String credentialsByTypeAttrId = "credentialsByTypeAttr";

    private final SearchSession searchSession;
    private final CredentialsService credentialsService;

    @GetMapping("/relying-parties")
    public ModelAndView searchGet(
        @RequestParam(value = "page") Optional<Integer> oneIndexedPageNum,
        @RequestParam(value = "sort") Optional<String> ordering) {

        oneIndexedPageNum.ifPresent(i -> searchSession.setCurrentPageIdx(i - 1));
        ordering.ifPresent(searchSession::setOrdering);

        List<RelyingPartyResource> resultsPage = searchSession.refreshSearch();
        return new ModelAndView("search_view", Map.of(
            resultsPageAttrId, resultsPage,
            searchFormAttrId, searchSession.getLastSearchForm(),
            credentialsByTypeAttrId, credentialsByType()));
    }

    @PostMapping("/relying-parties")
    public ModelAndView searchPost(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult) {

        ModelAndView mav = new ModelAndView("search_view", searchFormAttrId, searchForm);
        if (!bindingResult.hasErrors()) {
            List<RelyingPartyResource> resultsPage = searchSession.doFreshSearch(searchForm);
            mav.addObject(resultsPageAttrId, resultsPage);
        }
        mav.addObject(credentialsByTypeAttrId, credentialsByType());
        return mav;
    }

    private Map<String, CredentialResource> credentialsByType() {
        return credentialsService.getAvailableCredentials()
                                 .credentials()
                                 .stream()
                                 .collect(Collectors.toMap(
                                     CredentialResource::getCredentialType,
                                     Function.identity(),
                                     (first, _) -> first));
    }
}
