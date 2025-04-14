package no.idporten.eudiw.trustlist.web;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.trustlist.etsi_ts_102_231.TrustServiceStatusList;
import no.idporten.eudiw.trustlist.service.TSLService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter;

@RequiredArgsConstructor
@RestController
public class TrustListController {

    private final TSLService tslService;

    @GetMapping(value = "/access_tsl.xts", produces = "application/vnd.etsi.tsl+xml")
    public ResponseEntity<String> trustlist() throws Exception {
        TrustServiceStatusList trustServiceStatusList = tslService.generateTrustServiceStatusList();
        JAXBContext context = JAXBContext.newInstance(TrustServiceStatusList.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(trustServiceStatusList, stringWriter);
        return ResponseEntity.ok(stringWriter.toString());
    }

}
