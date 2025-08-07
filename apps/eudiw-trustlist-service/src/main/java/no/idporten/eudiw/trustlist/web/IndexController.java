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
                              eidas2sandkasse RP access <a href="access_tsl.xtsl">[ Download ]</a><a href="access_tsl"> [ Display ]</a>
                              <a href="access_tsl.sha2"> [ Sha2 ]</a>
                           </body>
                        </html>""");
    }

}
