package no.idporten.eudiw.trustlist.service;


import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.config.TrustListACAProperties;
import no.idporten.eudiw.trustlist.domain.TLSchemeInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.*;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static no.idporten.eudiw.trustlist.service.Common602Converter.*;
import static no.idporten.eudiw.trustlist.service.LangCode.EN;
import static no.idporten.eudiw.trustlist.service.LangCode.NO;

@Service
public class TrustListACAGeneratorService {


    private final TrustListACAProperties acaProperties;
    private final DigdirProperties digdirProperties;

    public TrustListACAGeneratorService(TrustListACAProperties acaProperties, DigdirProperties digdirProperties) {
        this.acaProperties = acaProperties;
        this.digdirProperties = digdirProperties;
    }

    public LoTE generateTrustlistACA() {
        TLSchemeInformation schemaProps = acaProperties.schemeInformation();

        LoTE lote = new LoTE();

        ListAndSchemeInformation listAndSchemeInformation = createListAndSchemeInformation(schemaProps);
        lote.setListAndSchemeInformation(listAndSchemeInformation);

        return lote;
    }

    private ListAndSchemeInformation createListAndSchemeInformation(TLSchemeInformation schemaProps) {
        ListAndSchemeInformation listAndSchemeInformation = new ListAndSchemeInformation();

        listAndSchemeInformation.setLoTEVersionIdentifier(1);
        listAndSchemeInformation.setLoTESequenceNumber(schemaProps.sequenceNumber().intValue());
        listAndSchemeInformation.setSchemeOperatorName(createSchemeOperatorName());
        listAndSchemeInformation.setSchemeOperatorAddress(createSchemeOperatorAddress(digdirProperties));
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

    private List<MultiLangString> createSchemeOperatorName() {
        return List.of(createMultiLangString(NO.getCode(), digdirProperties.nameNo()), createMultiLangString(EN.getCode(), digdirProperties.nameEn()));
    }

    private static SchemeOperatorAddress createSchemeOperatorAddress(DigdirProperties digdirProperties) {
        SchemeOperatorAddress schemeOperatorAddress = new SchemeOperatorAddress();

        PostalAddress postalAddress = createPostalAddress(digdirProperties.postalAddress());
        schemeOperatorAddress.setSchemeOperatorPostalAddress(List.of(postalAddress));

        List<NonEmptyMultiLangURI> electronicAddresses = createListOfElectronicAddresses(digdirProperties);
        schemeOperatorAddress.setSchemeOperatorElectronicAddress(electronicAddresses);

        return schemeOperatorAddress;
    }

    private static List<MultiLangString> createSchemaName(TLSchemeInformation schemaProps) {
        MultiLangString schemeNameNo = createMultiLangString(NO.getCode(), schemaProps.schemeName().langNo());
        MultiLangString schemeNameEn = createMultiLangString(EN.getCode(), schemaProps.schemeName().langEn());

        return List.of(schemeNameNo, schemeNameEn);
    }

}
