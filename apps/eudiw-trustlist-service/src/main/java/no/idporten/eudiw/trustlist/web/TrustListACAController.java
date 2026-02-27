package no.idporten.eudiw.trustlist.web;


import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
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

    @Autowired
    public TrustListACAController(TrustListACAGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    // For JSON trust list.
    @GetMapping(value = "${trustlist-service.tsl-aca.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<@NonNull LoTE> getTrustListACA() {
        LoTE loTE = generatorService.generateTrustlistACA();
        return ResponseEntity.ok(loTE);
    }



}
