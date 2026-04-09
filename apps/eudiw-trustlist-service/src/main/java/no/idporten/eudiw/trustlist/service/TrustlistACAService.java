package no.idporten.eudiw.trustlist.service;

import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TrustlistACAService {

    private final Trustlist602GeneratorService generatorService;

    private final JsonSignerService jsonSignerService;

    private final Trustlist602Properties trustlist602Properties;

    private final static Logger log = LoggerFactory.getLogger(TrustlistACAService.class);

    private volatile String signedTrustlist;

    public TrustlistACAService(Trustlist602GeneratorService generatorService, JsonSignerService jsonSignerService, Trustlist602Properties trustlist602Properties) {
        this.generatorService = generatorService;
        this.jsonSignerService = jsonSignerService;
        this.trustlist602Properties = trustlist602Properties;
    }

    public LoTE getACATrustlistAsLoTE() {

        return generatorService.generateTrustlist(Trustlist602Properties.TSL_ACA);
    }

    public String getSignedACATrustlist() {
        return this.signedTrustlist;
    }

    protected String signedACAJson() {
        LoTE loTE = generatorService.generateTrustlist(Trustlist602Properties.TSL_ACA);
        return jsonSignerService.signedTrustlist(loTE, trustlist602Properties.getAcaTrustlist().keystore());
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
