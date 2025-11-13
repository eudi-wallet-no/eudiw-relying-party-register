package no.idporten.eudiw.credential.registry.api;

import no.idporten.eudiw.credential.registry.response.CredentialRegisterService;
import org.springframework.web.bind.annotation.GetMapping;
import no.idporten.eudiw.credential.registry.response.model.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CredentialsController {

    private final CredentialRegisterService credentialRegisterService;

    @Autowired
    public CredentialsController(CredentialRegisterService credentialRegisterService) {
        this.credentialRegisterService = credentialRegisterService;
    }


    @GetMapping(value= "/v1/credentials", produces = MediaType.APPLICATION_JSON_VALUE)
    public Credentials credentials() {
        return credentialRegisterService.getCredentials();
    }
}
