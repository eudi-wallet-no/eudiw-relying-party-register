package no.idporten.eudiw.trustlist;

import jakarta.xml.bind.JAXBException;
import no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.etsi119602.pojo.MultiLangString;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;

import static no.idporten.eudiw.trustlist.xml.XMLUtils.parseTrustlist;

public class TestDataGenerator {


    public static final String DIGDIR = "DIGITALISERINGSDIREKTORATET";

    public static LoTE createLoTETrustlist() {
        LoTE loTE = new LoTE();
        ListAndSchemeInformation listAndSchemeInformation = new ListAndSchemeInformation();
        listAndSchemeInformation.setSchemeTerritory("NO");
        listAndSchemeInformation.setLoTEType(URI.create("http://acaorpid-trustlist-type"));
        listAndSchemeInformation.setSchemeName(List.of(createSchemeNameNo()));
        loTE.setListAndSchemeInformation(listAndSchemeInformation);
        return loTE;
    }

    private static @NonNull MultiLangString createSchemeNameNo() {
        MultiLangString noSchemeName = new MultiLangString();
        noSchemeName.setLang("no");
        noSchemeName.setValue(DIGDIR);
        return noSchemeName;
    }

    public static String createJsonFromLoTE() {
        return createJsonFromLoTE(createLoTETrustlist());
    }

    public static String createJsonFromLoTE(LoTE loTETrustlist) {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(loTETrustlist);
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
