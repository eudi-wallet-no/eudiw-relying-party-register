package no.idporten.eudiw.credential.registry.integration;

import io.micrometer.core.instrument.Counter;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.credential.registry.exception.CredentialRegisterException;
import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuerUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static no.idporten.eudiw.credential.registry.exception.CredentialRegisterException.ERROR;

@Service
public class RelyingPartyServiceIntegration {

    @Qualifier("restClientRpService")
    private final RestClient restClientRpService;

    @Qualifier("connectInternalApiExceptionCounter")
    private final Counter connectInternalApiExceptionCounter;

    private static final Logger log = LoggerFactory.getLogger(RelyingPartyServiceIntegration.class);


    @Autowired
    public RelyingPartyServiceIntegration(RestClient restClientRpService, Counter connectInternalApiExceptionCounter) {
        this.restClientRpService = restClientRpService;
        this.connectInternalApiExceptionCounter = connectInternalApiExceptionCounter;
    }

    public List<URI> retrieveCredentialIssuerUrlsFromRPService() {
        CredentialIssuerUrls uris;
        try {
            uris = restClientRpService.get()
                    .retrieve()
                    .body(CredentialIssuerUrls.class);
        } catch (ResourceAccessException e) {
            connectInternalApiExceptionCounter.increment();
            throw new CredentialRegisterException(ERROR, "Failed to connect to rp-service", HttpStatus.SERVICE_UNAVAILABLE, e);
        } catch (Exception e) {
            throw new CredentialRegisterException(ERROR, "Failed to fetch issuer-server uris from rp-service", HttpStatus.SERVICE_UNAVAILABLE, e);
        }
        return getValidUris(uris);
    }


    private @NotNull List<URI> getValidUris(CredentialIssuerUrls uris) {
        if(uris == null || uris.credentialIssuerUrls() == null) {
            log.warn("No issuer urls found in response from rp-service");
            return Collections.emptyList();

        }
        List<URI> listOfURI = new ArrayList<>();
        for (URI issuer : uris.credentialIssuerUrls()) {
            if (issuer != null && isHttps(issuer) && !emptyHost(issuer)) {
                listOfURI.add(URI.create(issuer.toString()));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Found urls in list of issuers: {}", listOfURI);
        }
        return listOfURI;
    }


    protected boolean isHttps(URI uri) {
        if (uri.getScheme() == null || !uri.getScheme().equals("https")) {
            log.warn("Issuer {} does not use https in its registered uri", uri);
            return false;
        }
        return true;
    }

    protected boolean emptyHost(URI uri) {
        if (!StringUtils.hasText(uri.getHost())) {
            log.warn("Issuer {} does not contain characters in host", uri);
            return true;
        }
        return false;
    }

}
