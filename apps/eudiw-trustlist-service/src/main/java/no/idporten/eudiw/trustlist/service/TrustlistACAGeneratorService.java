package no.idporten.eudiw.trustlist.service;


import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.config.TrustlistACAProperties;
import no.idporten.eudiw.trustlist.domain.etsi602.ListAndSchemeInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.*;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static no.idporten.eudiw.trustlist.service.Common602Converter.*;
import static no.idporten.eudiw.trustlist.service.LangCode.EN;
import static no.idporten.eudiw.trustlist.service.LangCode.NO;


@Service
public class TrustlistACAGeneratorService {

    private final TrustlistACAProperties acaProperties;
    private final DigdirProperties digdirProperties;
    private final static Logger log = LoggerFactory.getLogger(TrustlistACAGeneratorService.class);

    public TrustlistACAGeneratorService(TrustlistACAProperties acaProperties, DigdirProperties digdirProperties) {
        this.acaProperties = acaProperties;
        this.digdirProperties = digdirProperties;
    }

    public LoTE generateTrustlistACA() {
        ListAndSchemeInformation schemaProps = acaProperties.schemeInformation();

        LoTE lote = new LoTE();

        no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation listAndSchemeInformation = createListAndSchemeInformation(schemaProps);
        lote.setListAndSchemeInformation(listAndSchemeInformation);
        try{
            lote.setTrustedEntitiesList(createListOfTrustedEntity(acaProperties));
        } catch (ApplicationException e) {
            log.warn("Det har skjedd en feil ved setting av Trusted Entity lista til "
                    +  lote.getListAndSchemeInformation().getSchemeName().getFirst().getValue() + e);
        }

        return lote;
    }

    private no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation createListAndSchemeInformation(ListAndSchemeInformation schemaProps) {
        no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation listAndSchemeInformation = new no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation();

        listAndSchemeInformation.setLoTEVersionIdentifier(1);
        listAndSchemeInformation.setLoTESequenceNumber(schemaProps.sequenceNumber().intValue());
        listAndSchemeInformation.setSchemeOperatorName(createSchemeOperatorName());
        listAndSchemeInformation.setSchemeOperatorAddress(createSchemeOperatorAddress(digdirProperties));
        listAndSchemeInformation.setSchemeName(createSchemaName(schemaProps));
        listAndSchemeInformation.setSchemeInformationURI(createInformationURIs());

        listAndSchemeInformation.setLoTEType(schemaProps.loteType());
        listAndSchemeInformation.setStatusDeterminationApproach(schemaProps.statusDeterminationApproach());
        listAndSchemeInformation.setSchemeTypeCommunityRules(List.of(createNonEmptyMultiLangURI("en", schemaProps.schemeTypeCommunityRules())));
        listAndSchemeInformation.setSchemeTerritory("NO");
        listAndSchemeInformation.setPolicyOrLegalNotice(List.of("TODO: Venter på godkjenning av Endringsforordning (EU) 2024/1183 (eIDAS 2.0/endringsforordningen)"));
        ZonedDateTime issuedDateTime = schemaProps.listIssueDateTime();
        listAndSchemeInformation.setListIssueDateTime(Date.from(issuedDateTime.toInstant()));
        listAndSchemeInformation.setNextUpdate(Date.from(issuedDateTime.plusMonths(6).toInstant()));

        return listAndSchemeInformation;
    }


    private static List<MultiLangString> createSchemaName(ListAndSchemeInformation schemaProps) {
        MultiLangString schemeNameNo = createMultiLangString(NO.getCode(), schemaProps.schemeName().langNo());
        MultiLangString schemeNameEn = createMultiLangString(EN.getCode(), schemaProps.schemeName().langEn());

        return List.of(schemeNameNo, schemeNameEn);
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

    public List<TrustedEntity> createListOfTrustedEntity(TrustlistACAProperties acaProperties) {

        List<no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity> te = acaProperties.trustedEntities();
        List<TrustedEntity> finishedList = new ArrayList<>();
        for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity entity : te) {
            TrustedEntityInformation trustedEntityInformation = new TrustedEntityInformation();
            MultiLangString teNameNO = createMultiLangString(NO.getCode(), entity.trustedEntityInformation().teName());
            MultiLangString teNameEN = createMultiLangString(EN.getCode(), entity.trustedEntityInformation().teName());
            trustedEntityInformation.setTEName(List.of(teNameNO, teNameEN));

            trustedEntityInformation.setTEAddress(createTEAddress());

            NonEmptyMultiLangURI a = createNonEmptyMultiLangURI(
                    NO.getCode(), entity.trustedEntityInformation().informationUri().a());

            NonEmptyMultiLangURI c = createNonEmptyMultiLangURI(
                    NO.getCode(), entity.trustedEntityInformation().informationUri().c());

            trustedEntityInformation.setTEInformationURI(List.of(a, c));
            TrustedEntity trustedEntity = new TrustedEntity();
            trustedEntity.setTrustedEntityInformation(trustedEntityInformation);

            TrustedEntityService trustedEntityService = createRpAccessTrustedEntityService(entity, acaProperties);
            trustedEntity.setTrustedEntityServices(List.of(trustedEntityService));
            finishedList.add(trustedEntity);
        }
        return finishedList;
    }


    public TEAddress createTEAddress() {

        PostalAddress postalAddress = createPostalAddress(digdirProperties.postalAddress());
        List<NonEmptyMultiLangURI> electronicAddresses = createListOfElectronicAddresses(digdirProperties);

        TEAddress teAddress = new TEAddress();
        teAddress.setTEElectronicAddress(electronicAddresses);
        teAddress.setTEPostalAddress(List.of(postalAddress));

        return teAddress;
    }

    private TrustedEntityService createRpAccessTrustedEntityService(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity trustedEntity, TrustlistACAProperties acaProperties) {
        no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntityService trustedEntityService = new TrustedEntityService();

        MultiLangString serviceNameNo = createMultiLangString(NO.getCode(), acaProperties.trustedEntities().getFirst().trustedEntityServices().getFirst().serviceInformation().serviceName().langNo());
        MultiLangString serviceNameEn = createMultiLangString(EN.getCode(), acaProperties.trustedEntities().getFirst().trustedEntityServices().getFirst().serviceInformation().serviceName().langEn());
        ServiceInformation serviceInformation = new ServiceInformation();
        serviceInformation.setServiceName(List.of(serviceNameNo, serviceNameEn));
        ServiceDigitalIdentity serviceDigitalIdentity = new ServiceDigitalIdentity();

        List<PkiOb> list = new ArrayList<>();
        PkiOb pkiOb = new PkiOb();
        pkiOb.setVal(trustedEntity.trustedEntityServices().getFirst().serviceInformation().serviceDigitalIdentity().cert());
        list.add(pkiOb);
        serviceDigitalIdentity.setX509Certificates(list);
        serviceInformation.setServiceDigitalIdentity(serviceDigitalIdentity);
        trustedEntityService.setServiceInformation(serviceInformation);
        return trustedEntityService;
    }
}
