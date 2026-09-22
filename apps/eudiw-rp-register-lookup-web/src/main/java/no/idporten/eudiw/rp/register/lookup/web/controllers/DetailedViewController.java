package no.idporten.eudiw.rp.register.lookup.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;


import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DetailedViewController {

    public static final String detailedViewDataAttrId = "detailedViewDataAttr";
    public static final String credentialsByTypeAttrId = "credentialsByTypeAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final CredentialsService credentialsService;

    @GetMapping("/details")
    public ModelAndView detailsWithoutIdRedirectToSearch() {
        return new ModelAndView("redirect:/search");
    }

    @GetMapping("/details/{id}")
    public ModelAndView detailsGet(
        @PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);

        Map<String, CredentialResource> credentialsByType =
            credentialsService.getAvailableCredentials()
                              .credentials()
                              .stream()
                              .collect(Collectors.toMap(
                                  CredentialResource::getCredentialType,
                                  Function.identity(),
                                  (first, _) -> first));

        return new ModelAndView("details_view", Map.of(
            detailedViewDataAttrId, relyingPartyResource,
            credentialsByTypeAttrId, credentialsByType));
    }
}
