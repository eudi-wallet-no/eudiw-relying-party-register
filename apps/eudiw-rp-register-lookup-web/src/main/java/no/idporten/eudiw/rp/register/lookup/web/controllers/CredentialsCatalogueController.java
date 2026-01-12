package no.idporten.eudiw.rp.register.lookup.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CredentialsCatalogueController {

    public static final String credentialsAttrId = "credentialsAttr";
    public static final String credentialAttrId = "credentialAttr";

    private final CredentialsService credentialsService;

    @GetMapping("/credentials-catalogue")
    public ModelAndView credentialsCatalogueGet(
        @RequestParam(value = "sort", defaultValue = "name") String sortKey) {
        List<CredentialResource> credentials =
            credentialsService.getAvailableCredentials()
                              .sortBy(sortKey, "no");

        ModelAndView mav = new ModelAndView("credentials_view");

        mav.addObject(credentialsAttrId, credentials);
        mav.addObject("sortKeyAttr", sortKey);
        return mav;
    }

    @GetMapping("/credential")
    public ModelAndView credentialDetailsGet(
        @RequestParam(value = "issuer") String issuer,
        @RequestParam(value = "config-id") String configurationId) {

        CredentialResource credential =
            credentialsService.getCredential(issuer, configurationId);
        return new ModelAndView("credential_details_view", credentialAttrId, credential);
    }
}
