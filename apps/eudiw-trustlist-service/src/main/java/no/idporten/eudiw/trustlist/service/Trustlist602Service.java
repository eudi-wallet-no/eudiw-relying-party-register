package no.idporten.eudiw.trustlist.service;

import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.domain.etsi602.Trustlist;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class Trustlist602Service {

    private final Trustlist602GeneratorService generatorService;

    private final JsonSignerService jsonSignerService;

    private final Trustlist602Properties trustlist602Properties;

    private final static Logger log = LoggerFactory.getLogger(Trustlist602Service.class);

    private volatile Map<String, String> signedTrustlists;

    public Trustlist602Service(Trustlist602GeneratorService generatorService, JsonSignerService jsonSignerService, Trustlist602Properties trustlist602Properties) {
        this.generatorService = generatorService;
        this.jsonSignerService = jsonSignerService;
        this.trustlist602Properties = trustlist602Properties;
    }

    public LoTE getTrustlistAsLoTE(String trustlist) {
        return generatorService.generateTrustlist(trustlist);
    }

    public String getSignedTrustlist(String trustlist) {
        return this.signedTrustlists.get(trustlist);
    }

    protected Map<String, String> generateSignedTrustlists() {
        Map<String, String> signedTrustlists = new HashMap<>();
        if (trustlist602Properties == null || trustlist602Properties.tsl602() == null) {
            // should never happen
            throw new ApplicationException("No 602 trustlists configured in properties");
        }
        for (Map.Entry<String, Trustlist> entry : trustlist602Properties.tsl602().entrySet()) {
            LoTE loTE = getTrustlistAsLoTE(entry.getKey());
            signedTrustlists.put(entry.getKey(), jsonSignerService.signedTrustlist(loTE, entry.getValue().keystore()));
        }
        return signedTrustlists;
    }


    // Only generate trustlists once at application startup
    @PostConstruct
    private void initTrustlists() {
        try {
            this.signedTrustlists = generateSignedTrustlists();
        } catch (ApplicationException e) {
            log.error("Failed to generate 602 trustlists on startup", e);
            throw e;
        }

    }
}
