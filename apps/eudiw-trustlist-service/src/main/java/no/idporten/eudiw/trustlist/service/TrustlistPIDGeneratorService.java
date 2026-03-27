package no.idporten.eudiw.trustlist.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.config.TrustlistPIDProperties;
import no.idporten.eudiw.trustlist.domain.TSUri;
import no.idporten.eudiw.trustlist.domain.etsi602.ListAndSchemeInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static no.idporten.eudiw.trustlist.service.Common602Converter.*;
import static no.idporten.eudiw.trustlist.service.LangCode.EN;
import static no.idporten.eudiw.trustlist.service.LangCode.NO;

@Service
public class TrustlistPIDGeneratorService {

    private final TrustlistPIDProperties trustListPIDProperties;
    private final DigdirProperties digdirProperties;

    public TrustlistPIDGeneratorService(TrustlistPIDProperties trustListPIDProperties, DigdirProperties digdirProperties) {
        this.trustListPIDProperties = trustListPIDProperties;
        this.digdirProperties = digdirProperties;
    }

    public LoTE generateTrustlistPID() {
        ListAndSchemeInformation schemaProps = trustListPIDProperties.schemeInformation();

        LoTE lote = new LoTE();

        no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation listAndSchemeInformation = createListAndSchemeInformation(schemaProps);
        lote.setListAndSchemeInformation(listAndSchemeInformation);
        lote.setTrustedEntitiesList(createListOfTrustedEntity(trustListPIDProperties));

        return lote;
    }

    private no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation createListAndSchemeInformation(ListAndSchemeInformation schemaProps) {
        no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation listAndSchemeInformation = new no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation();

        listAndSchemeInformation.setLoTEVersionIdentifier(1);
        listAndSchemeInformation.setLoTESequenceNumber(schemaProps.sequenceNumber().intValue());
        listAndSchemeInformation.setSchemeOperatorName(createSchemeOperatorName());
        listAndSchemeInformation.setSchemeOperatorAddress(createSchemeOperatorAddress(digdirProperties));
        listAndSchemeInformation.setSchemeName(List.of((createMultiLangString(NO.getCode(), schemaProps.schemeName().langNo())), (createMultiLangString(EN.getCode(), schemaProps.schemeName().langEn()))));
        listAndSchemeInformation.setSchemeInformationURI(createInformationURIs(schemaProps.informationUris()));

        listAndSchemeInformation.setLoTEType(schemaProps.loteType());
        listAndSchemeInformation.setStatusDeterminationApproach(schemaProps.statusDeterminationApproach());
        listAndSchemeInformation.setSchemeTypeCommunityRules(List.of(createNonEmptyMultiLangURI(EN.getCode(), schemaProps.schemeTypeCommunityRules())));
        listAndSchemeInformation.setSchemeTerritory("NO");
        listAndSchemeInformation.setPolicyOrLegalNotice(List.of("TODO: Venter på godkjenning av Endringsforordning (EU) 2024/1183 (eIDAS 2.0/endringsforordningen)"));
        ZonedDateTime issuedDateTime = schemaProps.listIssueDateTime();
        listAndSchemeInformation.setListIssueDateTime(Date.from(issuedDateTime.toInstant()));
        listAndSchemeInformation.setNextUpdate(Date.from(issuedDateTime.plusMonths(6).toInstant()));

        return listAndSchemeInformation;
    }

    /**
     * The SchemeInformationURI component shall contain:
     * a) A URI where users can receive information about the PID providers list;
     * and
     * b) A URI where users can retrieve all previous instances of the PID providers list.
     */
    private List<NonEmptyMultiLangURI> createInformationURIs(@NotNull @Valid TSUri tsUri) {
        return List.of(createNonEmptyMultiLangURI(NO.getCode(), tsUri.langNo()), createNonEmptyMultiLangURI(EN.getCode(), tsUri.langEn()));
    }

    private List<MultiLangString> createSchemeOperatorName() {
        return List.of(createMultiLangString(NO.getCode(), digdirProperties.nameNo()), createMultiLangString(EN.getCode(), digdirProperties.nameEn()));
    }

    private static SchemeOperatorAddress createSchemeOperatorAddress(DigdirProperties digdirProperties) {
        SchemeOperatorAddress schemeOperatorAddress = new SchemeOperatorAddress();

        PostalAddress postalAddress = createPostalAddress(digdirProperties.postalAddress());
        schemeOperatorAddress.setSchemeOperatorPostalAddress(List.of(postalAddress));

        schemeOperatorAddress.setSchemeOperatorElectronicAddress(createListOfElectronicAddresses(digdirProperties));

        return schemeOperatorAddress;
    }

    /**
     * For each trusted entity in the trusted entity list, it makes spec objects of our PID config objects.
     * and for each trusted entity service within the list of services in the trusted entities, it also does that.
     *
     * @param pidProperties which is the PID config.
     * @return List of trusted entities, the etsi602 spec objects.
     */
    public List<TrustedEntity> createListOfTrustedEntity(TrustlistPIDProperties pidProperties) {
        List<TrustedEntity> finishedList = new ArrayList<>();
        for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity entity : pidProperties.trustedEntities().values()) {
            TrustedEntityInformation trustedEntityInformation = populateTrustedEntityInformation(entity);
            TrustedEntity trustedEntity = new TrustedEntity();
            trustedEntity.setTrustedEntityInformation(trustedEntityInformation);

            List<TrustedEntityService> trustedEntityServices = new ArrayList<>();
            for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService service : entity.trustedEntityServices()) {
                TrustedEntityService trustedEntityService = populatePIDTrustedEntityService(service);
                trustedEntityServices.add(trustedEntityService);
            }
            trustedEntity.setTrustedEntityServices(trustedEntityServices);
            finishedList.add(trustedEntity);
        }
        return finishedList;
    }

    private TrustedEntityInformation populateTrustedEntityInformation(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity trustedEntity) {
        TrustedEntityInformation trustedEntityInformation = new TrustedEntityInformation();
        trustedEntityInformation.setTEName(populateTEName(trustedEntity));
        trustedEntityInformation.setTETradeName(populateTETradeName(trustedEntity));
        trustedEntityInformation.setTEAddress(createTEAddress(trustedEntity));
        trustedEntityInformation.setTEInformationURI(populateTEInformationUri(trustedEntity));
        return trustedEntityInformation;
    }

    private List<MultiLangString> populateTETradeName(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity trustedEntity) {
        return List.of((createMultiLangString(NO.getCode(),
                trustedEntity.trustedEntityInformation().teTradeName())), (createMultiLangString(EN.getCode(),
                trustedEntity.trustedEntityInformation().teTradeName())));
    }

    private List<MultiLangString> populateTEName(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity trustedEntity) {
        return List.of((createMultiLangString(NO.getCode(),
                trustedEntity.trustedEntityInformation().teName())), (createMultiLangString(EN.getCode(),
                trustedEntity.trustedEntityInformation().teName())));
    }

    private List<NonEmptyMultiLangURI> populateTEInformationUri(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity trustedEntity) {
        if (trustedEntity.trustedEntityInformation().informationUri() == null) {
            LoggerFactory.getLogger(this.getClass()).warn("trustedEntity.trustedEntityInformation().informationUri() == null for %s".formatted(trustedEntity.trustedEntityInformation().teName()));
            return List.of();
        }
        ArrayList<NonEmptyMultiLangURI> informationUris = new ArrayList<>();
        if (trustedEntity.trustedEntityInformation().informationUri().a() != null) {
            NonEmptyMultiLangURI a = createNonEmptyMultiLangURI(NO.getCode(), trustedEntity.trustedEntityInformation().informationUri().a());
            informationUris.add(a);
        }
        if (trustedEntity.trustedEntityInformation().informationUri().b() != null) {
            NonEmptyMultiLangURI b = createNonEmptyMultiLangURI(EN.getCode(), trustedEntity.trustedEntityInformation().informationUri().b());
            informationUris.add(b);
        }
        if (trustedEntity.trustedEntityInformation().informationUri().c() != null) {
            NonEmptyMultiLangURI c = createNonEmptyMultiLangURI(NO.getCode(), trustedEntity.trustedEntityInformation().informationUri().c());
            informationUris.add(c);
        }
        return informationUris;
    }


    /**
     *
     * @param trustedEntity Out config object with data about the individual trusted entity on the PID trustlist
     * @return etsi602 spec object of the trusted entity address.
     */
    public TEAddress createTEAddress(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity trustedEntity) {

        PostalAddress postalAddress = createPostalAddress(digdirProperties.postalAddress());
        List<NonEmptyMultiLangURI> electronicAddresses = populateListOfElectronicAddressesPID(digdirProperties, trustedEntity);
        TEAddress teAddress = new TEAddress();
        teAddress.setTEElectronicAddress(electronicAddresses);
        teAddress.setTEPostalAddress(List.of(postalAddress));

        return teAddress;
    }

    private List<NonEmptyMultiLangURI> populateListOfElectronicAddressesPID(DigdirProperties digdirProperties, no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity trustedEntity) {
        NonEmptyMultiLangURI emailNo = createNonEmptyMultiLangURI(NO.getCode(), digdirProperties.email());
        NonEmptyMultiLangURI emailEn = createNonEmptyMultiLangURI(EN.getCode(), digdirProperties.email());
        NonEmptyMultiLangURI webPageNo = createNonEmptyMultiLangURI(NO.getCode(), digdirProperties.web());
        NonEmptyMultiLangURI webPageEn = createNonEmptyMultiLangURI(EN.getCode(), digdirProperties.web());
        // TODO: flytte telefon-nr til digdir props. og gjere NPE sjekking/feilhåndtering.
        NonEmptyMultiLangURI phoneNumberNo = createNonEmptyMultiLangURI(NO.getCode(), trustedEntity.trustedEntityInformation().teAddress().phoneNumber());
        NonEmptyMultiLangURI PhoneNumberEn = createNonEmptyMultiLangURI(EN.getCode(), trustedEntity.trustedEntityInformation().teAddress().phoneNumber());
        return List.of(emailNo, emailEn, webPageNo, webPageEn, phoneNumberNo, PhoneNumberEn);
    }

    /**
     *
     * @param trustedEntityservice the object we take data from.
     * @return etsi602 spec object populated with values from trustedEntityService, which is data from our PID config.
     */

    private TrustedEntityService populatePIDTrustedEntityService(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService trustedEntityservice) {
        no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntityService trustedEntityService = new TrustedEntityService();
        ServiceInformation serviceInformation = populateServiceInformation(trustedEntityservice);
        serviceInformation.setServiceDigitalIdentity(populateX509certificate(trustedEntityservice));
        trustedEntityService.setServiceInformation(serviceInformation);
        return trustedEntityService;
    }

    /**
     * Retrieves the certificate out from our property object, and into the service digital identity etsi602 object
     *
     * @param trustedEntityService the individual service under a trusted entity. This is the part that contains our data
     * @return the serviceDigitalIdentity spec object which we have inserted information from our trustedEntityService object
     */
    private ServiceDigitalIdentity populateX509certificate(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService trustedEntityService) {
        ServiceDigitalIdentity serviceDigitalIdentity = new ServiceDigitalIdentity();
        List<PkiOb> list = new ArrayList<>();
        PkiOb pkiOb = new PkiOb();
        pkiOb.setVal(trustedEntityService.serviceInformation().serviceDigitalIdentity().cert());
        list.add(pkiOb);
        serviceDigitalIdentity.setX509Certificates(list);
        return serviceDigitalIdentity;
    }

    private ServiceInformation populateServiceInformation(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService trustedEntityService) {
        ServiceInformation serviceInformation = new ServiceInformation();
        populateServiceName(trustedEntityService, serviceInformation);
        populateServiceTypeIdentifier(trustedEntityService, serviceInformation);
        return serviceInformation;
    }

    /**
     *
     * @param trustedEntityService the individual service under a trusted entity. This is the part that contains our data
     * @param serviceInformation   we wish to insert our trustedEntityService data about service name into this spec object.
     */
    private void populateServiceName(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService trustedEntityService, ServiceInformation serviceInformation) {
        MultiLangString serviceNameNo = createMultiLangString(NO.getCode(), trustedEntityService.serviceInformation().serviceName().langNo());
        MultiLangString serviceNameEn = createMultiLangString(EN.getCode(), trustedEntityService.serviceInformation().serviceName().langEn());
        serviceInformation.setServiceName(List.of(serviceNameNo, serviceNameEn));
    }

    /**
     *
     * @param trustedEntityService the individual service under a trusted entity. This is the part that contains our data
     * @param serviceInformation   we wish to insert our trustedEntityService data into this spec object.
     */
    private void populateServiceTypeIdentifier(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService trustedEntityService, ServiceInformation serviceInformation) {
        serviceInformation.setServiceTypeIdentifier(URI.create(trustedEntityService.serviceInformation().serviceTypeIdentifier().a()));
    }
}