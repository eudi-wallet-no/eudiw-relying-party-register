package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.service.TrustlistPIDService;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
public class TrustlistPIDController {

    private final TrustlistPIDService pidService;

    @Value("${trustlist-service.tsl-pid.scheme-information.list-issue-datetime:#{T(java.time.ZonedDateTime).now()}}")
    private ZonedDateTime lastModified;

    @Autowired
    public TrustlistPIDController(TrustlistPIDService pidService) {
        this.pidService = pidService;
    }

    /**
     * Format:
     * - JWS Header: x5c (x509 certificate chain), alg, iat
     * - JWS Payload: PID Trustlist as JSON
     * - JWS Signature
     * @return PID Trustlist as JSON in the payload of JWS.
     */
    @GetMapping(value = "${trustlist-service.tsl-pid.path}.jws", produces = "application/jose+json")
    public ResponseEntity<@NonNull String> getSignedTrustlistPID() {
        String loTe = pidService.getSignedPidTrustlist();
        return ResponseEntity.ok().lastModified(lastModified.toInstant()).body(loTe);
    }

    @GetMapping(value = "${trustlist-service.tsl-pid.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<@NonNull LoTE> trustlistShow() {
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(pidService.getPIDTrustlistAsLoTE());
    }
}
