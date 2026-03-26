package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TrustlistPIDService {

    private final TrustlistPIDGeneratorService generatorService;

    // TODO: Signer trustlisten
    private final JsonSignerService jsonSignerService;
    private final static Logger log = LoggerFactory.getLogger(TrustlistPIDService.class);
    private volatile String signedTrustlist;

    public TrustlistPIDService(TrustlistPIDGeneratorService generatorService, JsonSignerService jsonSignerService) {
        this.generatorService = generatorService;
        this.jsonSignerService = jsonSignerService;
    }


    public LoTE getPIDTrustlistAsLoTE() {
        return generatorService.generateTrustlistPID();
    }
}
