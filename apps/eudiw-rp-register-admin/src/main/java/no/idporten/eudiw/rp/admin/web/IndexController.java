package no.idporten.eudiw.rp.admin.web;

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
                              <title>RP Register Admin</title>
                           </head>
                           <body>
                              <h1>RP Register Admin</h1>
                           </body>
                        </html>""");
    }

}
