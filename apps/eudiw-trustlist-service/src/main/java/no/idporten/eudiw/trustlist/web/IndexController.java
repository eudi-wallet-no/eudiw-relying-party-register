package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.config.Trustlist612Properties;
import no.idporten.eudiw.trustlist.domain.etsi602.Trustlist;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    private final Trustlist612Properties list612Properties;
    private final Trustlist602Properties trustlist602Properties;

    public IndexController(final Trustlist612Properties list612Properties, Trustlist602Properties trustlist602Properties) {
        this.list612Properties = list612Properties;
        this.trustlist602Properties = trustlist602Properties;
    }


    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<@NonNull String> index() {
        return ResponseEntity.ok(createPageContent());
    }

    private String createPageContent() {
        String tsl612TrustlistLinks = getTrustlistLinksWithSha(list612Properties.schemeInformation().schemeName().langNo(), list612Properties.trustlistPathXtsl(), list612Properties.trustlistPath(), list612Properties.trustlistPathSha2());
        StringBuilder tsl602TrustlistsLinks = new StringBuilder();
        for(Trustlist trustlist : trustlist602Properties.tsl602().values()){
            String links = getTrustlistLinks(trustlist.schemeInformation().schemeName().langNo(), trustlist.path());
            tsl602TrustlistsLinks.append("<li>").append(links).append("</li>");
        }
        return """
                <html>
                   <head>
                      <title>Trust List Service</title>
                   </head>
                   <body>
                      <h1>Tillitslister</h1>
                      <ul>
                        <li>%s</li>
                        %s
                      </ul>
                   </body>
                </html>""".formatted(tsl612TrustlistLinks, tsl602TrustlistsLinks.toString());
    }

    private String getTrustlistLinksWithSha(String title, String trustlistPathXtsl, String trustlistPath, String trustlistPathSha2) {
        // Tillitsliste tittel [ Last ned ] [ Vis ] [ Sha2 ]
        return "%s <a href=\"%s\">[ Last ned ]</a><a href=\"%s\"> [ Vis ]</a> <a href=\"%s\"> [ Sha2 ]</a>".formatted(title, trustlistPathXtsl, trustlistPath, trustlistPathSha2);
    }

    private String getTrustlistLinks(String title, String trustlistPath) {
        // Tillitsliste tittel [ Last ned ] [ Vis JSON ]
        return "%s <a href=\"%s.jws\">[ Last ned ]</a><a href=\"%s\"> [ Vis JSON ]</a>".formatted(title, trustlistPath, trustlistPath);
    }

}
