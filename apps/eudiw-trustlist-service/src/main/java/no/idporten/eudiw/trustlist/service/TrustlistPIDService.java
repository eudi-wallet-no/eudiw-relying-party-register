package no.idporten.eudiw.trustlist.service;

import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.trustlist.config.TrustlistPIDProperties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TrustlistPIDService {

    private final TrustlistPIDGeneratorService generatorService;

    private final TrustlistPIDProperties trustListPIDProperties;

    private final JsonSignerService jsonSignerService;

    private final static Logger log = LoggerFactory.getLogger(TrustlistPIDService.class);
    private volatile String signedTrustlist;

    public TrustlistPIDService(TrustlistPIDGeneratorService generatorService, JsonSignerService jsonSignerService, TrustlistPIDProperties trustListPIDProperties) {
        this.generatorService = generatorService;
        this.jsonSignerService = jsonSignerService;
        this.trustListPIDProperties = trustListPIDProperties;
    }

    public LoTE getPIDTrustlistAsLoTE() {
        return generatorService.generateTrustlistPID();
    }

    public String getSignedPidTrustlist() {
       return this.signedTrustlist;
    }

    protected String signedPidJson() {
        LoTE loTE = generatorService.generateTrustlistPID();
        return jsonSignerService.signedTrustlist(loTE, trustListPIDProperties.keystore());
    }

    // Only generate Pid trustlist once at application startup
    @PostConstruct
    private void initTrustlist() {
        try {
            this.signedTrustlist = signedPidJson();
        } catch (ApplicationException e) {
            log.error("Failed to generate Pid Trust Service Status List on startup", e);
            throw e;
        }

    }
}
