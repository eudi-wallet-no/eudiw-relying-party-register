package no.idporten.eudiw.trustlist.service;

import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.TSL_PID;

@Service
public class TrustlistPIDService {

    private final Trustlist602GeneratorService generatorService;

    private final Trustlist602Properties trustList602Properties;

    private final JsonSignerService jsonSignerService;

    private final static Logger log = LoggerFactory.getLogger(TrustlistPIDService.class);
    private volatile String signedTrustlist;

    public TrustlistPIDService(Trustlist602GeneratorService generatorService, JsonSignerService jsonSignerService, Trustlist602Properties trustList602Properties) {
        this.generatorService = generatorService;
        this.jsonSignerService = jsonSignerService;
        this.trustList602Properties = trustList602Properties;
    }

    public LoTE getPIDTrustlistAsLoTE() {
        return generatorService.generateTrustlist(TSL_PID);
    }

    public String getSignedPidTrustlist() {
       return this.signedTrustlist;
    }

    protected String signedPidJson() {
        LoTE loTE = generatorService.generateTrustlist(TSL_PID);
        return jsonSignerService.signedTrustlist(loTE, trustList602Properties.getPidTrustlist().keystore());
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
