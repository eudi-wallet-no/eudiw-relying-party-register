package no.idporten.eudiw.credential.registry.response;


import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.credential.registry.integration.CredentialIssuerMetadataRetriever;
import no.idporten.eudiw.credential.registry.integration.model.CredentialConfiguration;
import no.idporten.eudiw.credential.registry.response.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CredentialRegisterService {

    private static final Logger log = LoggerFactory.getLogger(CredentialRegisterService.class);
    private final CredentialIssuerMetadataRetriever credentialIssuerMetadataRetriever;
    private Credentials credentials;
    @Autowired
    public CredentialRegisterService(CredentialIssuerMetadataRetriever credentialIssuerMetadataRetriever) {
        this.credentialIssuerMetadataRetriever = credentialIssuerMetadataRetriever;
    }

    @PostConstruct
    public void init() {
        updateCredentialMetadataRetriever();
    }

    @Scheduled(cron = "${credential-registry.scheduled-reading}")
    public void updateCredentialMetadataRetriever() {
        this.credentialIssuerMetadataRetriever.updateListOfIssuer();
        log.info("Updating credential metadata retriever");
        setResponse();
    }

    public void setResponse() {
        List<CredentialsIssuer> outputCredentials =
        this.credentialIssuerMetadataRetriever.getListOfIssuer().stream().flatMap((issuer) ->
            issuer.credentialConfiguration().entrySet().stream().map((key) ->
                 inputDataToResponseIssuer(issuer.credentialIssuer(), key.getKey(), key.getValue(), issuer.display())
            )
        ).toList();
        credentials = new Credentials(outputCredentials);
    }

    private CredentialsIssuer inputDataToResponseIssuer(String issuer, String key, CredentialConfiguration credentialConfiguration, List<no.idporten.eudiw.credential.registry.integration.model.Display> issuerDisplay) {
        List<Display> outerListOfDisplay = credentialConfiguration.credentialMetadata().display().stream().map(display -> new Display(display.name(), display.locale(), display.description())).toList();
        List<Claims> claimsList = credentialConfiguration.credentialMetadata().claims().stream().map(claims -> new Claims(claims.path(), claims.display().stream().map(display -> new Display(display.name(), display.locale(), display.description())).toList())).toList();
        List<Display> issuerPrettyName = issuerDisplay.stream().map(display -> new Display(display.name(), display.locale(), display.description())).toList();
        CredentialMetadata newCredentialMetadata = new CredentialMetadata(outerListOfDisplay, claimsList);
        return new CredentialsIssuer(issuer, key, credentialConfiguration.doctype(), credentialConfiguration.format(), newCredentialMetadata, issuerPrettyName);
    }

    public Credentials  getCredentials() {
        return credentials;
    }
}
