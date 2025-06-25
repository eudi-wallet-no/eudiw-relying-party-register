package no.idporten.eudiw.trustlist.web;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.trustlist.service.TSLService;
import no.idporten.eudiw.trustlist.service.XMLSignerService;
import no.idporten.eudiw.trustlist.xml.XMLUtils;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMResult;

@RequiredArgsConstructor
@RestController
public class TrustListController {

    private final TSLService tslService;
    private final XMLSignerService xmlSignerService;

    @GetMapping(value = "/access_tsl.xtsl", produces = "application/vnd.etsi.tsl+xml")
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
        Document document = marshal(trustServiceStatusList);
        Document signedDocument = xmlSignerService.createEnvelopedSignature(document);
        return ResponseEntity.ok(XMLUtils.formatXml(signedDocument));
    }

    protected Document marshal(TrustServiceStatusList trustServiceStatusList) throws Exception {
        JAXBContext context = JAXBContext.newInstance(trustServiceStatusList.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        DOMResult domResult = new DOMResult();
        marshaller.marshal(trustServiceStatusList, domResult);
        return (Document) domResult.getNode();
    }

}
