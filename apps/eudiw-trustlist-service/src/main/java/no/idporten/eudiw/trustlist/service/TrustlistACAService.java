package no.idporten.eudiw.trustlist.service;

import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.trustlist.config.TrustlistACAProperties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TrustlistACAService {

    private final TrustlistACAGeneratorService generatorService;

    private final JsonSignerService jsonSignerService;

    private final TrustlistACAProperties acaProperties;

    private final static Logger log = LoggerFactory.getLogger(TrustlistACAService.class);

    private volatile String signedTrustlist;

    public TrustlistACAService(TrustlistACAGeneratorService generatorService, JsonSignerService jsonSignerService, TrustlistACAProperties acaProperties) {
        this.generatorService = generatorService;
        this.jsonSignerService = jsonSignerService;
        this.acaProperties = acaProperties;
    }

    public LoTE getACATrustlistAsLoTE() {
        return generatorService.generateTrustlistACA();
    }

    public String getSignedACATrustlist() {
        return this.signedTrustlist;
    }

    protected String signedACAJson() {
        LoTE loTE = generatorService.generateTrustlistACA();
        return jsonSignerService.signedTrustlist(loTE, acaProperties.keystore());
    }


    // Only generate ACA trustlist once at application startup
    @PostConstruct
    private void initTrustlist() {
        try {
            this.signedTrustlist = signedACAJson();
        } catch (ApplicationException e) {
            log.error("Failed to generate ACA Trust Service Status List on startup", e);
            throw e;
        }

    }
}
