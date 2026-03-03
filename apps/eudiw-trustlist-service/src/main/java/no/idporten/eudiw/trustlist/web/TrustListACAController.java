package no.idporten.eudiw.trustlist.web;


import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.service.JsonSignerService;
import no.idporten.eudiw.trustlist.service.TrustListACAGeneratorService;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TrustListACAController {


    private final TrustListACAGeneratorService generatorService;
    private final JsonSignerService signerService;

    @Autowired
    public TrustListACAController(TrustListACAGeneratorService generatorService, JsonSignerService signerService) {
        this.generatorService = generatorService;
        this.signerService = signerService;
    }

    @GetMapping(value = "${trustlist-service.tsl-aca.path}.jws", produces = "application/jose+json")
    public ResponseEntity<@NonNull String> getSignedTrustListACA() {
        LoTE loTE = generatorService.generateTrustlistACA();
        String jws = signerService.signedJson(loTE);
        return ResponseEntity.ok(jws);
    }

    @GetMapping(value = "${trustlist-service.tsl-aca.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<@NonNull LoTE> getTrustListACAAsJson() {
        LoTE loTE = generatorService.generateTrustlistACA();
        return ResponseEntity.ok(loTE);
    }

}
