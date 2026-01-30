package no.idporten.eudiw.credential.registry.response;


import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.credential.registry.exception.CredentialRegisterException;
import no.idporten.eudiw.credential.registry.integration.CredentialIssuerMetadataRetriever;
import no.idporten.eudiw.credential.registry.integration.model.CredentialConfiguration;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
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
            mapInputToResponse();
        }
    }

    protected void mapInputToResponse() {
        List<CredentialsIssuer> outputCredentials =
                this.credentialIssuerMetadataRetriever.getListOfIssuer().stream().flatMap((issuer) ->
                        issuer.getCredentialConfiguration().entrySet().stream().map((key) ->
                                inputDataToResponseIssuer(issuer, key.getKey(), key.getValue()))
                ).toList();
        credentials = new Credentials(outputCredentials);
    }

    private CredentialsIssuer inputDataToResponseIssuer(CredentialIssuer issuer, String key, CredentialConfiguration credentialConfiguration) {
        List<Display> issuerPrettyDisplay = issuerPrettyDisplay(issuer);
        List<Display> credentialMetadataDisplay = credentialMetadataDisplay(credentialConfiguration);
        List<Claims> issuerMetadataClaims = issuerMetadataClaims(credentialConfiguration);
        CredentialMetadata newCredentialMetadata = new CredentialMetadata(credentialMetadataDisplay, issuerMetadataClaims);
        return new CredentialsIssuer(issuer.getCredentialIssuer(), key, credentialConfiguration.findTypeByFormat(), credentialConfiguration.getFormat(), newCredentialMetadata, issuerPrettyDisplay);
    }

    private List<Display> credentialMetadataDisplay(CredentialConfiguration credentialConfiguration) {
        if (credentialConfiguration.getCredentialMetadata() != null && credentialConfiguration.getCredentialMetadata().getDisplay() != null) {
            return credentialConfiguration.getCredentialMetadata().getDisplay().stream().map(display -> new Display(display.getName(), display.getLocale(), display.getDescription())).toList();
        } else {
            return null;
        }
    }

    private List<Claims> issuerMetadataClaims(CredentialConfiguration credentialConfiguration) {
        if(credentialConfiguration.getCredentialMetadata() != null && credentialConfiguration.getCredentialMetadata().getClaims() != null) {
            if (credentialConfiguration.getCredentialMetadata().getClaims().stream().anyMatch(claims -> claims.getDisplay() != null)){
                return credentialConfiguration.getCredentialMetadata().getClaims().stream().map(claims -> new Claims(claims.getPath(), claims.getDisplay().stream().map(display -> new Display(display.getName(), display.getLocale(), display.getDescription())).toList())).toList();
            } else{
                return credentialConfiguration.getCredentialMetadata().getClaims().stream().map(claims -> new Claims(claims.getPath(), null)).toList();
            }
        } else {
            return null;
        }
    }

    private List<Display> issuerPrettyDisplay(CredentialIssuer issuer) {
        if (issuer.getDisplay()!= null){
            return issuer.getDisplay().stream().map(display -> new Display(display.getName(), display.getLocale(), display.getDescription())).toList();
        } else {
            return null;
        }
    }


    public Credentials  getCredentials() {
        return credentials;
    }
}
