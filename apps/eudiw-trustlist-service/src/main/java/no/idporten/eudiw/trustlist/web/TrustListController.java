package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.service.SignedTrustListService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
public class TrustListController {

    protected static final String PATH_ACCESS_TRUSTLIST = "/access_tsl";
    public static final String PATH_ACCESS_TRUSTLIST_XTSL = PATH_ACCESS_TRUSTLIST + ".xtsl";
    public static final String PATH_ACCESS_TRUSTLIST_SHA = PATH_ACCESS_TRUSTLIST+ ".sha2";

    private final SignedTrustListService signedTrustListService;

    @Value("${trustlist-service.scheme-information.list-issue-datetime:#{T(java.time.ZonedDateTime).now()}}")
    private ZonedDateTime lastModified;

    public TrustListController(SignedTrustListService signedTrustListService) {
        this.signedTrustListService = signedTrustListService;
    }

    @GetMapping(value = PATH_ACCESS_TRUSTLIST_XTSL, produces = "application/vnd.etsi.tsl+xml")
    public ResponseEntity<String> trustlist() {
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(signedTrustListService.getTrustlist());
    }

    // TODO: Temp. for ease of testing, should be removed in the future?
    @GetMapping(value = PATH_ACCESS_TRUSTLIST, produces = "text/xml;charset=UTF-8")
    public ResponseEntity<String> trustlistShow() {
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(signedTrustListService.getTrustlist());
    }

    @GetMapping(value = PATH_ACCESS_TRUSTLIST_SHA, produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> trustlistSha2() {
        String sha2 = signedTrustListService.getSha2();
        return ResponseEntity.ok()
                .lastModified(lastModified.toInstant())
                .body(sha2);
    }

}
