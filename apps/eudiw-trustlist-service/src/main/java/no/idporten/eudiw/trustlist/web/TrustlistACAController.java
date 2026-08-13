package no.idporten.eudiw.trustlist.web;


import no.idporten.eudiw.trustlist.domain.etsi602.pojo.LoTEResponse;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.service.Trustlist602Service;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.TSL_ACA;


@RestController
public class TrustlistACAController {

    @Value("${trustlist-service.tsl602.tsl-aca.scheme-information.list-issue-date-time:#{T(java.time.ZonedDateTime).now()}}")
    private ZonedDateTime lastModified;

    private final Trustlist602Service service;

    @Autowired
    public TrustlistACAController(Trustlist602Service service) {
        this.service = service;
    }

    /**
     * Format:
     * - JWS Header: x5c (x509 certificate chain), alg, iat
     * - JWS Payload: ACA Trustlist as JSON
     * - JWS Signature
     *
     * @return ACA Trustlist as JSON in the payload of JWS.
     */
    @GetMapping(value = "${trustlist-service.tsl602.tsl-aca.path}.jws", produces = "application/jose+json")
    public ResponseEntity<@NonNull String> getSignedTrustListACA() {
        String loTe = service.getSignedTrustlist(TSL_ACA);
        return ResponseEntity.ok().lastModified(lastModified.toInstant()).body(loTe);
    }

    /**
     * Only for human convenience. List in JSON format for easy reading.
     *
     * @return ACA Trustlist as JSON
     */
    @GetMapping(value = "${trustlist-service.tsl602.tsl-aca.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<@NonNull LoTEResponse> getTrustListACAAsJson() {
        LoTE loTE = service.getTrustlistAsLoTE(TSL_ACA);
        LoTEResponse loTEResponse = new LoTEResponse(loTE);
        return ResponseEntity.ok().lastModified(lastModified.toInstant()).body(loTEResponse);
    }

}
