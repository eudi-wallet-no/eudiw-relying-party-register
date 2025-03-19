package no.idporten.eudiw.rp.ca.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CAApiController {

    @GetMapping("/")
    public ResponseEntity<String> api() {
        return ResponseEntity.ok("""
                {"api": "ca"}""");
    }

}
