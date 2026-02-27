package no.idporten.eudiw.trustlist.service;


import jakarta.validation.constraints.NotEmpty;
import no.idporten.eudiw.trustlist.config.TrustListACAProperties;
import no.idporten.eudiw.trustlist.domain.TLSchemeInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Service
public class TrustListACAGeneratorService {


    private final TrustListACAProperties acaProperties;

    @Autowired
    public TrustListACAGeneratorService(TrustListACAProperties acaProperties) {
        this.acaProperties = acaProperties;
    }

    public LoTE generateTrustlistACA() {
        TLSchemeInformation schemaProps = acaProperties.schemeInformation();

        LoTE lote = new LoTE();

        ListAndSchemeInformation listAndSchemeInformation = createListAndSchemeInformation(schemaProps);
        lote.setListAndSchemeInformation(listAndSchemeInformation);

        return lote;
    }

    private static ListAndSchemeInformation createListAndSchemeInformation(TLSchemeInformation schemaProps) {
        ListAndSchemeInformation listAndSchemeInformation = new ListAndSchemeInformation();

        listAndSchemeInformation.setLoTEVersionIdentifier(1);
        listAndSchemeInformation.setLoTESequenceNumber(schemaProps.sequenceNumber().intValue());
        listAndSchemeInformation.setSchemeOperatorName(createSchemeOperatorName());
        listAndSchemeInformation.setSchemeOperatorAddress(createSchemeOperatorAddress());
        listAndSchemeInformation.setSchemeName(createSchemaName(schemaProps));
        listAndSchemeInformation.setSchemeInformationURI(createInformationURIs());

        listAndSchemeInformation.setLoTEType(URI.create("http://uri.etsi.org/19602/LoTEType/EUWRPACProvidersList"));
        listAndSchemeInformation.setStatusDeterminationApproach(URI.create("http://uri.etsi.org/19602/WRPACProvidersList/StatusDetn/EU"));
        listAndSchemeInformation.setSchemeTypeCommunityRules(List.of(createNonEmptyMultiLangURI("en", "http://uri.etsi.org/19602/WRPACProvidersList/schemerules/EU")));
        listAndSchemeInformation.setSchemeTerritory("NO");
        listAndSchemeInformation.setPolicyOrLegalNotice(List.of("TODO: Venter på godkjenning av Endringsforordning (EU) 2024/1183 (eIDAS 2.0/endringsforordningen)"));
        ZonedDateTime issuedDateTime = schemaProps.listIssueDateTime();
        listAndSchemeInformation.setListIssueDateTime(Date.from(issuedDateTime.toInstant()));
        listAndSchemeInformation.setNextUpdate(Date.from(issuedDateTime.plusMonths(6).toInstant()));

        return listAndSchemeInformation;
    }

    private static List<NonEmptyMultiLangURI> createInformationURIs() {
        // TODO kva URL skal me legge inn? burde me legge inn URL til samarbeidsportalen? Ideelt sett laga ei eiga side per trustlist schema (dei ulike listene).
        return List.of(createNonEmptyMultiLangURI("no", "https://docs.digdir.no/docs/lommebok/lommebok_om.html"), createNonEmptyMultiLangURI("no", "https://docs.digdir.no/docs/lommebok/wallet_sandbox_summary.html"));
    }

    private static List<MultiLangString> createSchemeOperatorName() {
        return List.of(createMultiLangString("no", "Digitaliseringsdirektoratet"), createMultiLangString("en", "Norwegian Digitalisation Agency"));
    }

    private static SchemeOperatorAddress createSchemeOperatorAddress() {
        SchemeOperatorAddress schemeOperatorAddress = new SchemeOperatorAddress();

        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setLang("no");
        postalAddress.setStreetAddress("Lørenfaret 1C");
        postalAddress.setPostalCode("0580");
        postalAddress.setLocality("Oslo");
        postalAddress.setCountry("NO");

        schemeOperatorAddress.setSchemeOperatorPostalAddress(List.of(postalAddress));

        NonEmptyMultiLangURI emailNo = createNonEmptyMultiLangURI("no", "mailto:servicedesk@digdir.no");
        NonEmptyMultiLangURI emailEn = createNonEmptyMultiLangURI("en", "mailto:servicedesk@digdir.no");
        NonEmptyMultiLangURI webPageNo = createNonEmptyMultiLangURI("no", "https://www.digdir.no/");
        NonEmptyMultiLangURI webPageEn = createNonEmptyMultiLangURI("en", "https://www.digdir.no/");

        schemeOperatorAddress.setSchemeOperatorElectronicAddress(List.of(emailNo, emailEn, webPageNo, webPageEn));

        return schemeOperatorAddress;
    }

    private static NonEmptyMultiLangURI createNonEmptyMultiLangURI(String lang, String uriString) {
        NonEmptyMultiLangURI multiLangUri = new NonEmptyMultiLangURI();
        multiLangUri.setLang(lang);
        multiLangUri.setUriValue(URI.create(uriString));
        return multiLangUri;
    }

    private static List<MultiLangString> createSchemaName(TLSchemeInformation schemaProps) {
        MultiLangString schemeNameNo = createMultiLangString("no", schemaProps.schemeName().langNo());
        MultiLangString schemeNameEn = createMultiLangString("en", schemaProps.schemeName().langEn());

        return List.of(schemeNameNo, schemeNameEn);
    }

    private static MultiLangString createMultiLangString(String lang, @NotEmpty String value) {
        MultiLangString schemeNameEn = new MultiLangString();
        schemeNameEn.setLang(lang);
        schemeNameEn.setValue(value);
        return schemeNameEn;
    }

}
