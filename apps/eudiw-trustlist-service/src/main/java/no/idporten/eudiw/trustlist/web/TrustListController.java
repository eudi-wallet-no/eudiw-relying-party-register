package no.idporten.eudiw.trustlist.web;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.trustlist.service.TSLService;
import no.idporten.eudiw.trustlist.service.XMLSignerService;
import no.idporten.eudiw.trustlist.xml.XMLUtils;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import static no.idporten.eudiw.trustlist.xml.XMLUtils.parseTrustlist;

@RequiredArgsConstructor
@RestController
public class TrustListController {

    public static final String PATH_ACCESS_TRUSTLIST = "/access_tsl.xtsl";

    private final TSLService tslService;
    private final XMLSignerService xmlSignerService;

    @GetMapping(value = PATH_ACCESS_TRUSTLIST, produces = "application/vnd.etsi.tsl+xml")
    public ResponseEntity<String> trustlist() throws Exception {
        return getTrustlist();
    }

    // TODO: Temp. for ease of testing, should be removed in the future?
    @GetMapping(value = "/access_tsl", produces = "text/xml;charset=UTF-8")
    public ResponseEntity<String> trustlistShow() throws Exception {
        return getTrustlist();
    }

    private ResponseEntity<String> getTrustlist() throws Exception {
        TrustServiceStatusList trustServiceStatusList = tslService.generateTrustServiceStatusList();
        Document document = parseTrustlist(trustServiceStatusList);
        Document signedDocument = xmlSignerService.createEnvelopedSignature(document);
        return ResponseEntity.ok(XMLUtils.formatXml(signedDocument));
    }


}
