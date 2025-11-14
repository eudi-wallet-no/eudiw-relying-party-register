package no.idporten.eudiw.rp.register.lookup.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@Profile("!(test | prod)")
public class CredentialsCatalogueController {

    public static final String credentialsAttrId = "credentialsAttr";
    public static final String credentialAttrId = "credentialAttr";

    private final CredentialsService credentialsService;

    @GetMapping("/credentials-catalogue")
    public ModelAndView credentialsCatalogueGet() {
        List<CredentialResource> credentials =
            credentialsService.getAvailableCredentials().credentials();
        return new ModelAndView("credentials_view", credentialsAttrId, credentials);
    }

    @GetMapping("/credential/{issuer}/{config-id}")
    public ModelAndView credentialDetailsGet(
        @PathVariable(value = "issuer") String issuer,
        @PathVariable(value = "config-id") String configurationId) {

        issuer = URLDecoder.decode(issuer, StandardCharsets.US_ASCII);
        configurationId = URLDecoder.decode(configurationId, StandardCharsets.US_ASCII);

        CredentialResource credential =
            credentialsService.getCredential(issuer, configurationId);
        return new ModelAndView("credential_details_view", credentialAttrId, credential);
    }
}
