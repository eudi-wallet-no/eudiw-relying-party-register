package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.service.SignedTrustlist612Service;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
public class TrustlistController {

    private final SignedTrustlist612Service signedTrustListService;

    @Value("${trustlist-service.tsl612.scheme-information.list-issue-date-time:#{T(java.time.ZonedDateTime).now()}}")
    private ZonedDateTime lastModified;

    public TrustlistController(SignedTrustlist612Service signedTrustListService) {
        this.signedTrustListService = signedTrustListService;
    }

    @GetMapping(value = "${trustlist-service.tsl612.trustlist-path-xtsl}", produces = "application/vnd.etsi.tsl+xml")
    public ResponseEntity<@NonNull String> trustlist() {
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(signedTrustListService.getTrustlist());
    }

    // TODO: Temp. for ease of testing, should be removed in the future?
    @GetMapping(value = "${trustlist-service.tsl612.trustlist-path}", produces = "text/xml;charset=UTF-8")
    public ResponseEntity<@NonNull String> trustlistShow() {
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(signedTrustListService.getTrustlist());
    }

    @GetMapping(value = "${trustlist-service.tsl612.trustlist-path-sha2}", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<@NonNull String> trustlistSha2() {
        String sha2 = signedTrustListService.getSha2();
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(sha2);
    }

}
