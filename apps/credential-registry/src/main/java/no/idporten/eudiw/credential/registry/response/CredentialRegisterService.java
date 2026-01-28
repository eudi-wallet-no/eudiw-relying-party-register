package no.idporten.eudiw.credential.registry.response;


import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.credential.registry.exception.CredentialRegisterException;
import no.idporten.eudiw.credential.registry.integration.CredentialIssuerMetadataRetriever;
import no.idporten.eudiw.credential.registry.integration.model.CredentialConfiguration;
import no.idporten.eudiw.credential.registry.response.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
        try {
            this.credentialIssuerMetadataRetriever.updateListOfIssuer();
        } catch (Exception ex) {
            throw new CredentialRegisterException("Error updating credentials", "failed_update_credential_registry", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
        if (this.credentialIssuerMetadataRetriever.getListOfIssuer() != null) {
            log.info("Updating credential metadata retriever");
            setResponse();
        }
    }

    protected void setResponse() {
        List<CredentialsIssuer> outputCredentials =
        this.credentialIssuerMetadataRetriever.getListOfIssuer().stream().flatMap((issuer) ->
            issuer.getCredentialConfiguration().entrySet().stream().map((key) ->
                 inputDataToResponseIssuer(issuer.getCredentialIssuer(), key.getKey(), key.getValue(), issuer.getDisplay()))
            ).toList();
        credentials = new Credentials(outputCredentials);
    }

    private CredentialsIssuer inputDataToResponseIssuer(String issuer, String key, CredentialConfiguration credentialConfiguration, List<no.idporten.eudiw.credential.registry.integration.model.Display> issuerDisplay) {
        List<Display> outerListOfDisplay = credentialConfiguration.getCredentialMetadata().getDisplay().stream().map(display -> new Display(display.getName(), display.getLocale(), display.getDescription())).toList();
        List<Claims> claimsList = credentialConfiguration.getCredentialMetadata().getClaims().stream().map(claims -> new Claims(claims.getPath(), claims.getDisplay().stream().map(display -> new Display(display.getName(), display.getLocale(), display.getDescription())).toList())).toList();
        List<Display> issuerPrettyName = issuerDisplay.stream().map(display -> new Display(display.getName(), display.getLocale(), display.getDescription())).toList();
        CredentialMetadata newCredentialMetadata = new CredentialMetadata(outerListOfDisplay, claimsList);
        return new CredentialsIssuer(issuer, key, credentialConfiguration.findTypeByFormat(), credentialConfiguration.getFormat(), newCredentialMetadata, issuerPrettyName);
    }

    public Credentials  getCredentials() {
        return credentials;
    }
}
