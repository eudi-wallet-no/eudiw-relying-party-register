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

import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.TSL_PID;
import static no.idporten.eudiw.trustlist.web.TrustlistMediaTypes.APPLICATION_JOSE_JSON;
import static no.idporten.eudiw.trustlist.web.TrustlistMediaTypes.APPLICATION_VND_LOTE_JSON;

@RestController
public class TrustlistPIDController {

    private final Trustlist602Service service;

    @Value("${trustlist-service.tsl602.tsl-pid.scheme-information.list-issue-date-time:#{T(java.time.ZonedDateTime).now()}}")
    private ZonedDateTime lastModified;

    @Autowired
    public TrustlistPIDController(Trustlist602Service service) {
        this.service = service;
    }

    /**
     * Format:
     * - JWS Header: x5c (x509 certificate chain), alg, iat
     * - JWS Payload: PID Trustlist as JSON
     * - JWS Signature
     * @return PID Trustlist as JSON in the payload of JWS.
     */
    @GetMapping(value = "${trustlist-service.tsl602.tsl-pid.path}.jws", produces = {APPLICATION_JOSE_JSON, APPLICATION_VND_LOTE_JSON})
    public ResponseEntity<@NonNull String> getSignedTrustlistPID() {
        String loTe = service.getSignedTrustlist(TSL_PID);
        return ResponseEntity.ok().lastModified(lastModified.toInstant()).body(loTe);
    }

    @GetMapping(value = "${trustlist-service.tsl602.tsl-pid.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<@NonNull LoTEResponse> trustlistShow() {
        LoTE loTE = service.getTrustlistAsLoTE(TSL_PID);
        LoTEResponse loTEResponse = new LoTEResponse(loTE);
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(loTEResponse);
    }
}
