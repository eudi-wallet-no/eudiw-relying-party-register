package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.config.Trustlist612Properties;
import no.idporten.eudiw.trustlist.config.TrustlistACAProperties;
import no.idporten.eudiw.trustlist.config.TrustlistPIDProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    private final Trustlist612Properties list612Properties;
    private final TrustlistACAProperties acaProperties;
    private final TrustlistPIDProperties pidProperties;

    public IndexController(final Trustlist612Properties list612Properties, final TrustlistACAProperties acaProperties, final  TrustlistPIDProperties pidProperties) {
        this.list612Properties = list612Properties;
        this.acaProperties = acaProperties;
        this.pidProperties = pidProperties;
    }


    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<@NonNull String> index() {
        return ResponseEntity.ok(createPageContent());
    }

    private String createPageContent() {
        String tsl612TrustlistLinks = getTrustlistLinksWithSha(list612Properties.schemeInformation().schemeName().langNo(), list612Properties.trustlistPathXtsl(), list612Properties.trustlistPath(), list612Properties.trustlistPathSha2());
        String acaTrustlistLinks = getTrustlistLinks(acaProperties.schemeInformation().schemeName().langNo(), acaProperties.path());
        String pidItustlistLinks = getTrustlistLinks(pidProperties.schemeInformation().schemeName().langNo(), pidProperties.path());

        return """
                <html>
                   <head>
                      <title>Trust List Service</title>
                   </head>
                   <body>
                      <h1>Tillitslister</h1>
                      <ul>
                        <li>%s</li>
                        <li>%s</li>
                        <li>%s</li>
                      </ul>
                   </body>
                </html>""".formatted(tsl612TrustlistLinks, acaTrustlistLinks,  pidItustlistLinks);
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
