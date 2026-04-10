package no.idporten.eudiw.trustlist.web;

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

import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.TSL_WALLET;

@RestController
public class TrustlistWalletController {

    private final Trustlist602Service service;

    @Value("${trustlist-service.tsl602.tsl-wallet.scheme-information.list-issue-datetime:#{T(java.time.ZonedDateTime).now()}}")
    private ZonedDateTime lastModified;

    @Autowired
    public TrustlistWalletController(Trustlist602Service service) {
        this.service = service;
    }

    /**
     * Format:
     * - JWS Header: x5c (x509 certificate chain), alg, iat
     * - JWS Payload: Wallet Trustlist as JSON
     * - JWS Signature
     * @return Wallet Trustlist as JSON in the payload of JWS.
     */
    @GetMapping(value = "${trustlist-service.tsl602.tsl-wallet.path}.jws", produces = "application/jose+json")
    public ResponseEntity<@NonNull String> getSignedTrustlistWallet() {
        String loTe = service.getSignedTrustlist(TSL_WALLET);
        return ResponseEntity.ok().lastModified(lastModified.toInstant()).body(loTe);
    }

    @GetMapping(value = "${trustlist-service.tsl602.tsl-wallet.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<@NonNull LoTE> trustlistShow() {
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(service.getTrustlistAsLoTE(TSL_WALLET));
    }
}
