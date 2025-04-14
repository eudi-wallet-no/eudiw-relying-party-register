package no.idporten.eudiw.trustlist.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @GetMapping("/")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok(
                """
                        <html>
                           <head>
                              <title>Trust List Service</title>
                           </head>
                           <body>
                              <h1>Trust List Service</h1>
                              <h2>Trust status lists</h2>
                              <a href="access_tsl.xts">eidas2sandkasse RP access</a>
                           </body>
                        </html>""");
    }

}
