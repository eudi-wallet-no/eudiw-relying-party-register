package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.service.SignedTrustListService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
public class TrustListController {

    private final SignedTrustListService signedTrustListService;

    @Value("${trustlist-service.tsl-612.scheme-information.list-issue-datetime:#{T(java.time.ZonedDateTime).now()}}")
    private ZonedDateTime lastModified;

    public TrustListController(SignedTrustListService signedTrustListService) {
        this.signedTrustListService = signedTrustListService;
    }

    @GetMapping(value = "${trustlist-service.tsl-612.trustlist-path-xtsl}", produces = "application/vnd.etsi.tsl+xml")
    public ResponseEntity<String> trustlist() {
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(signedTrustListService.getTrustlist());
    }

    // TODO: Temp. for ease of testing, should be removed in the future?
    @GetMapping(value = "${trustlist-service.tsl-612.trustlist-path}", produces = "text/xml;charset=UTF-8")
    public ResponseEntity<String> trustlistShow() {
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(signedTrustListService.getTrustlist());
    }

    @GetMapping(value = "${trustlist-service.tsl-612.trustlist-path-sha2}", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> trustlistSha2() {
        String sha2 = signedTrustListService.getSha2();
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(sha2);
    }

}
