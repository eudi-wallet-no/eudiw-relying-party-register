package no.idporten.eudiw.trustlist.web;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import no.idporten.eudiw.trustlist.etsi_ts_102_231.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;

@RestController
public class TrustListController {


    private InternationalNamesType createInternationalNamesType(String lang, String value) {
        MultiLangNormStringType multiLangNormStringType = createMultiLangNormStringType(lang, value);
        InternationalNamesType internationalNamesType = new InternationalNamesType();
        internationalNamesType.getNames().add(multiLangNormStringType);
        return internationalNamesType;
    }

    private NonEmptyMultiLangURIType createNonEmptyMultiLangURIType(String lang, String value) {
        NonEmptyMultiLangURIType nonEmptyMultiLangURIType = new NonEmptyMultiLangURIType();
        nonEmptyMultiLangURIType.setLang(lang);
        nonEmptyMultiLangURIType.setValue(value);
        return nonEmptyMultiLangURIType;
    }

    private InternationalNamesType createInternationalNamesType(MultiLangNormStringType... values) {
        InternationalNamesType internationalNamesType = new InternationalNamesType();
        for (MultiLangNormStringType value : values) {
            internationalNamesType.getNames().add(value);
        }
        return internationalNamesType;
    }

    private static MultiLangNormStringType createMultiLangNormStringType(String lang, String value) {
        MultiLangNormStringType multiLangNormStringType = new MultiLangNormStringType();
        multiLangNormStringType.setLang(lang);
        multiLangNormStringType.setValue(value);
        return multiLangNormStringType;
    }

    @GetMapping(value = "/truststatuslist.xts", produces = "application/vnd.etsi.tsl+xml")
    public ResponseEntity<String> trustlist() throws Exception {
        TrustServiceStatusList trustServiceStatusList = new TrustServiceStatusList();
        // provide information on the issuing scheme;
        SchemeInformation schemeInformation = new SchemeInformation();
        schemeInformation.setTSLVersionIdentifier(BigInteger.valueOf(3));
        schemeInformation.setTSLSequenceNumber(BigInteger.ONE);
        schemeInformation.setTSLType("http://uri.etsi.org/TrstSvc/TrustedList/TSLType/EUgeneric");
        schemeInformation.setSchemeOperatorName(createInternationalNamesType("no", "Digitaliseringsdirektoratet"));
        schemeInformation.setSchemeOperatorName(createInternationalNamesType("en", "The Norwegian Digitalisation Agency"));


        PostalAddresses postalAddresses = new PostalAddresses();
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setLang("no");
        postalAddress.setStreetAddress("Lørenfaret 1 C");
        postalAddress.setPostalCode("0580");
        postalAddress.setLocality("Oslo");
        postalAddress.setCountryName("NO");
        ElectronicAddressType electronicAddressType = new ElectronicAddressType();
        electronicAddressType.getURIS().add("servicedesk@digdir.no");
        electronicAddressType.getURIS().add("https://www.digdir.no/");
        AddressType addressType = new AddressType();
        postalAddresses.getPostalAddresses().add(postalAddress);
        addressType.setPostalAddresses(postalAddresses);
        addressType.setElectronicAddress(electronicAddressType);
        schemeInformation.setSchemeOperatorAddress(addressType);

        schemeInformation.setSchemeName(createInternationalNamesType(
                createMultiLangNormStringType("no", "Trusted list for eidas2sandkasse.dev"),
                createMultiLangNormStringType("en", "Trusted list for eidas2sandkasse.dev")
        ));
        SchemeInformationURI schemeInformationURI = new SchemeInformationURI();
        schemeInformationURI.getURIS().add(createNonEmptyMultiLangURIType("no", "https://www.digdir.no/"));
        schemeInformationURI.getURIS().add(createNonEmptyMultiLangURIType("en", "https://www.digdir.no/"));
        schemeInformation.setSchemeInformationURI(schemeInformationURI);
        schemeInformation.setStatusDeterminationApproach("http://uri.etsi.org/TrstSvc/TSLType/StatusDetn/active");








        schemeInformation.setSchemeTerritory("NO");
        schemeInformation.setHistoricalInformationPeriod(BigInteger.valueOf(65534));

        schemeInformation.setListIssueDateTime(ZonedDateTime.now());



        trustServiceStatusList.setSchemeInformation(schemeInformation);

        // • identify the TSPs recognized by the scheme;


        // • indicate the service(s) provided by these TSPs and the current status of those service(s);


        // • indicate for each service the status history of that service.


        JAXBContext context = JAXBContext.newInstance(TrustServiceStatusList.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(trustServiceStatusList, stringWriter);
        return ResponseEntity.ok(stringWriter.toString());

    }

}
