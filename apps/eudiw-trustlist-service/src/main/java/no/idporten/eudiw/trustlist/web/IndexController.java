package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.config.TrustlistServiceProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    private final TrustlistServiceProperties properties;

    public IndexController(final TrustlistServiceProperties properties) {
        this.properties = properties;
    }


    @GetMapping("/")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok(createPageContent());
    }

    private String createPageContent() {
        String trustlistPathXtsl = properties.getTsl612().trustlistPathXtsl();
        String trustlistPath = properties.getTsl612().trustlistPath();
        String trustlistPathSha2 = properties.getTsl612().trustlistPathSha2();
        String title = properties.getTsl612().schemeInformation().schemeName();

        return """
                <html>
                   <head>
                      <title>Trust List Service</title>
                   </head>
                   <body>
                      <h1>Tillitslister</h1>
                      <ul>
                        <li>%s <a href="%s">[ Last ned ]</a><a href="%s"> [ Vis ]</a> <a href="%s"> [ Sha2 ]</a></li>
                      </ul>
                   </body>
                </html>""".formatted(title, trustlistPathXtsl, trustlistPath, trustlistPathSha2);
    }

}
