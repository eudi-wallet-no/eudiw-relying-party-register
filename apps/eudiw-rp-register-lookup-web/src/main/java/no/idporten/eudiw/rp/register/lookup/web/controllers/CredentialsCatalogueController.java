package no.idporten.eudiw.rp.register.lookup.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@Profile("dev | docker | systest")
public class CredentialsCatalogueController {

    public static final String credentialsAttrId = "credentialsAttr";

    private final CredentialsService credentialsService;

    @GetMapping("/credentials-catalogue")
    public ModelAndView credentialsGet() {
        List<CredentialResource> credentials =
            credentialsService.getAvailableCredentials().credentials();
        return new ModelAndView("credentials_view", credentialsAttrId, credentials);
    }
}
