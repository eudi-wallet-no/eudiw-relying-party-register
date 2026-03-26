package no.idporten.eudiw.trustlist;

import jakarta.xml.bind.JAXBException;
import no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.URI;

import static no.idporten.eudiw.trustlist.xml.XMLUtils.parseTrustlist;

public class TestDataGenerator {


    public static LoTE createLoTETrustlist() {
        LoTE loTE = new LoTE();
        ListAndSchemeInformation listAndSchemeInformation = new ListAndSchemeInformation();
        listAndSchemeInformation.setSchemeTerritory("NO");
        listAndSchemeInformation.setLoTEType(URI.create("http://acaorpid-trustlist-type"));
        loTE.setListAndSchemeInformation(listAndSchemeInformation);
        return loTE;
    }

    public static Document createSignedTrustlist() throws JAXBException {
        Document trustlist = parseTrustlist(createTrustServiceStatusList());
        Element signature = trustlist.createElement("Signature");
        signature.setAttribute("fakeSignature", "but I do not care");
        trustlist.getDocumentElement().appendChild(signature);
        return trustlist;
    }

    @NotNull
    public static TrustServiceStatusList createTrustServiceStatusList() {
        TrustServiceStatusList trustServiceStatusList = new TrustServiceStatusList();
        trustServiceStatusList.setId("trustlist-id");
        return trustServiceStatusList;
    }


}
