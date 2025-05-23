package no.idporten.eudiw.trustlist.web;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.trustlist.etsi_ts_119_612.TrustServiceStatusList;
import no.idporten.eudiw.trustlist.service.TSLService;
import no.idporten.eudiw.trustlist.service.XMLSignerService;
import no.idporten.eudiw.trustlist.xml.XMLUtils;
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

    @GetMapping(value = "/access_tsl.xts", produces = "application/vnd.etsi.tsl+xml")
    public ResponseEntity<String> trustlist() throws Exception {
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
