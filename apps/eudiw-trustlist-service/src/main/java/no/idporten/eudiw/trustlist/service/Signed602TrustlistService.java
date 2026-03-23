package no.idporten.eudiw.trustlist.service;

import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Signed602TrustlistService {

    private final TrustlistACAGeneratorService generatorService;

    private final JsonSignerService jsonSignerService;

    private final static Logger log = LoggerFactory.getLogger(Signed602TrustlistService.class);

    private volatile String signedTrustlist;

    public Signed602TrustlistService(TrustlistACAGeneratorService generatorService, JsonSignerService jsonSignerService) {
        this.generatorService = generatorService;
        this.jsonSignerService = jsonSignerService;
    }

    public LoTE getACATrustlistAsLoTE() {
        return generatorService.generateTrustlistACA();
    }

    public String getSignedACATrustlist() {
        if (this.signedTrustlist == null) {
            log.warn("ACA trustlist is not initialized, try generating again.");
            this.signedTrustlist = signedACAJson();
        }
        return this.signedTrustlist;
    }

    protected String signedACAJson() {
        LoTE loTE = generatorService.generateTrustlistACA();
        return jsonSignerService.signedTrustlist(loTE);
    }


    // Only generate ACA trustlist once at application startup
    @PostConstruct
    public void initTrustlist() {
        try {
            this.signedTrustlist = getSignedACATrustlist();
        } catch (ApplicationException e) {
            log.error("Failed to generate ACA Trust Service Status List on startup", e);
            throw e;
        }

    }
}
