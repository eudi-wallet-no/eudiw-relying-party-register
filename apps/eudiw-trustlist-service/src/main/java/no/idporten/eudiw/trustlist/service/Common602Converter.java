package no.idporten.eudiw.trustlist.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.domain.Address;
import no.idporten.eudiw.trustlist.domain.TSUri;
import no.idporten.eudiw.trustlist.domain.etsi602.InformationUri;
import no.idporten.eudiw.trustlist.domain.etsi602.ListAndSchemeInformation;
import no.idporten.eudiw.trustlist.domain.etsi602.ServiceName;
import no.idporten.eudiw.trustlist.domain.etsi602.pojo.LoTEPolicy;
import no.idporten.eudiw.trustlist.etsi119602.pojo.*;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static no.idporten.eudiw.trustlist.service.LangCode.EN;
import static no.idporten.eudiw.trustlist.service.LangCode.NO;

public class Common602Converter {

    protected static List<NonEmptyMultiLangURI> createListOfElectronicAddresses(@NotBlank String email, @NotBlank String web) {
        return createListOfElectronicAddresses(email, web, null);
    }

    private static List<NonEmptyMultiLangURI> createListOfElectronicAddresses(@NotBlank String email, @NotBlank String web, String phoneNumber) {
        NonEmptyMultiLangURI emailNo = createNonEmptyMultiLangURI(NO.getCode(), email);
        NonEmptyMultiLangURI emailEn = createNonEmptyMultiLangURI(EN.getCode(), email);
        NonEmptyMultiLangURI webPageNo = createNonEmptyMultiLangURI(NO.getCode(), web);
        NonEmptyMultiLangURI webPageEn = createNonEmptyMultiLangURI(EN.getCode(), web);

        if (phoneNumber == null) {
            return List.of(emailNo, emailEn, webPageNo, webPageEn);
        } else {
            NonEmptyMultiLangURI phoneNumberNo = createNonEmptyMultiLangURI(NO.getCode(), phoneNumber);
            NonEmptyMultiLangURI phoneNumberEn = createNonEmptyMultiLangURI(EN.getCode(), phoneNumber);
            return List.of(emailNo, emailEn, webPageNo, webPageEn, phoneNumberNo, phoneNumberEn);
        }
    }

    protected static PostalAddress createPostalAddress(Address address) {
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setLang(NO.getCode());
        postalAddress.setStreetAddress(address.streetAddress());
        postalAddress.setPostalCode(address.postalCode());
        postalAddress.setLocality(address.locality());
        postalAddress.setCountry(address.country());
        return postalAddress;
    }

    protected static NonEmptyMultiLangURI createNonEmptyMultiLangURI(String lang, String uriString) {
        NonEmptyMultiLangURI langURI = new NonEmptyMultiLangURI();
        langURI.setLang(lang);
        langURI.setUriValue(URI.create(uriString));
        return langURI;
    }

    protected static MultiLangString createMultiLangString(String lang, @NotEmpty String value) {
        MultiLangString langString = new MultiLangString();
        langString.setLang(lang);
        langString.setValue(value);
        return langString;
    }

    public static no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation createListAndSchemeInformation(ListAndSchemeInformation schemaProps, DigdirProperties digdirProperties) {
        no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation listAndSchemeInformation = new no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation();

        listAndSchemeInformation.setLoTEVersionIdentifier(1);
        listAndSchemeInformation.setLoTESequenceNumber(schemaProps.sequenceNumber().intValue());
        listAndSchemeInformation.setSchemeOperatorName(createSchemeOperatorName(digdirProperties.nameNo(), digdirProperties.nameEn()));
        listAndSchemeInformation.setSchemeOperatorAddress(createSchemeOperatorAddress(digdirProperties));
        listAndSchemeInformation.setSchemeName(createSchemaName(schemaProps));
        return getListAndSchemeInformation(schemaProps, listAndSchemeInformation, createInformationURIs(schemaProps.informationUris()));
    }

    private static no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation getListAndSchemeInformation(ListAndSchemeInformation schemaProps, no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation listAndSchemeInformation, List<NonEmptyMultiLangURI> informationURIs) {
        listAndSchemeInformation.setSchemeInformationURI(informationURIs);

        listAndSchemeInformation.setLoTEType(schemaProps.loteType());
        listAndSchemeInformation.setStatusDeterminationApproach(schemaProps.statusDeterminationApproach());
        listAndSchemeInformation.setSchemeTypeCommunityRules(List.of(createNonEmptyMultiLangURI(EN.getCode(), schemaProps.schemeTypeCommunityRules())));
        listAndSchemeInformation.setSchemeTerritory("NO");
        listAndSchemeInformation.setPolicyOrLegalNotice(createPolicyOrLegalNotice());
        ZonedDateTime issuedDateTime = schemaProps.listIssueDateTime();
        listAndSchemeInformation.setListIssueDateTime(Date.from(issuedDateTime.toInstant()));
        listAndSchemeInformation.setNextUpdate(Date.from(issuedDateTime.plusMonths(6).toInstant()));

        return listAndSchemeInformation;
    }

    private static @NonNull List<Object> createPolicyOrLegalNotice() {
        //String policy = "TODO: Venter på godkjenning av Endringsforordning (EU) 2024/1183 (eIDAS 2.0/endringsforordningen)";
        //LoTELegalNotice legalNotice = new LoTELegalNotice(createMultiLangString(NO.getCode(), policy));
        LoTEPolicy loTEPolicy = new LoTEPolicy(createNonEmptyMultiLangURI(NO.getCode(), "https://samarbeid.digdir.no/digital-lommebok/bruksvilkar-og-samarbeidsavtaler-eidas-sandkassen/3288"));
        // DSS lib validerer ikkje LoTELegalNotice, berre LoTEPolicy ok (dvs url, ikkje tekst direkte i lista).
        return List.of(loTEPolicy);
    }

    private static List<MultiLangString> createSchemaName(ListAndSchemeInformation schemaProps) {
        MultiLangString schemeNameNo = createMultiLangString(NO.getCode(), schemaProps.schemeName().langNo());
        MultiLangString schemeNameEn = createMultiLangString(EN.getCode(), schemaProps.schemeName().langEn());

        return List.of(schemeNameNo, schemeNameEn);
    }

    private static List<MultiLangString> createSchemeOperatorName(@NotEmpty String operatorNameNorwegian, @NotEmpty String operatorNameEnglish) {
        return List.of(createMultiLangString(NO.getCode(), operatorNameNorwegian), createMultiLangString(EN.getCode(), operatorNameEnglish));
    }

    /**
     * The SchemeInformationURI component shall contain:
     * a) A URI where users can receive information about the PID providers list;
     * and
     * b) A URI where users can retrieve all previous instances of the PID providers list.
     */
    public static List<NonEmptyMultiLangURI> createInformationURIs(@NotNull @Valid TSUri tsUri) {
        // TODO kva URL skal me legge inn? burde me legge inn URL til samarbeidsportalen? Ideelt sett laga ei eiga side per trustlist schema (dei ulike listene).
        // Foreløpig bruke same url på alle listene
        return List.of(createNonEmptyMultiLangURI("no", tsUri.langNo()), createNonEmptyMultiLangURI("no", tsUri.langEn()));
    }

    private static SchemeOperatorAddress createSchemeOperatorAddress(DigdirProperties digdirProperties) {
        SchemeOperatorAddress schemeOperatorAddress = new SchemeOperatorAddress();

        PostalAddress postalAddress = createPostalAddress(digdirProperties.postalAddress());
        schemeOperatorAddress.setSchemeOperatorPostalAddress(List.of(postalAddress));

        List<NonEmptyMultiLangURI> electronicAddresses = createListOfElectronicAddresses(digdirProperties.email(), digdirProperties.web());
        schemeOperatorAddress.setSchemeOperatorElectronicAddress(electronicAddresses);

        return schemeOperatorAddress;
    }


    protected static List<MultiLangString> addSameNameForNoEn(@NotBlank String teTradeName) {
        return List.of((createMultiLangString(NO.getCode(),
                teTradeName)), (createMultiLangString(EN.getCode(),
                teTradeName)));
    }

    public static TrustedEntityInformation populateTrustedEntityInformation(DigdirProperties digdirProperties, @Valid no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityInformation entityInformation) {
        TrustedEntityInformation trustedEntityInformation = new TrustedEntityInformation();
        trustedEntityInformation.setTEName(addSameNameForNoEn(entityInformation.teName()));
        trustedEntityInformation.setTETradeName(addSameNameForNoEn(entityInformation.teTradeName()));

        // Ikkje krav om telefonnummer for ACA (men for PID)
        if (entityInformation.teAddress() != null && entityInformation.teAddress().phoneNumber() != null) {
            trustedEntityInformation.setTEAddress(createTEAddress(digdirProperties.postalAddress(), digdirProperties.email(), digdirProperties.web(), entityInformation.teAddress().phoneNumber()));
        } else {
            trustedEntityInformation.setTEAddress(createTEAddress(digdirProperties.postalAddress(), digdirProperties.email(), digdirProperties.web()));
        }

        trustedEntityInformation.setTEInformationURI(populateTEInformationUri(entityInformation.informationUri()));
        return trustedEntityInformation;
    }

    /**
     *
     * @param address
     * @param email
     * @param website
     * @return etsi602 spec object of the trusted entity address.
     */
    private static TEAddress createTEAddress(@Valid @NotNull Address address, @NotBlank String email, @NotBlank String website) {
        return createTEAddress(address, email, website, null);
    }

    /**
     *
     * @param address
     * @param email
     * @param website
     * @param phoneNumber
     * @return etsi602 spec object of the trusted entity address.
     */
    private static TEAddress createTEAddress(@Valid @NotNull Address address, @NotBlank String email, @NotBlank String website, String phoneNumber) {

        PostalAddress postalAddress = createPostalAddress(address);
        List<NonEmptyMultiLangURI> electronicAddresses = createListOfElectronicAddresses(email, website, phoneNumber);
        TEAddress teAddress = new TEAddress();
        teAddress.setTEElectronicAddress(electronicAddresses);
        teAddress.setTEPostalAddress(List.of(postalAddress));

        return teAddress;
    }

    private static List<NonEmptyMultiLangURI> populateTEInformationUri(@Valid @NotNull InformationUri informationUri) {

        ArrayList<NonEmptyMultiLangURI> informationUris = new ArrayList<>();
        if (informationUri.a() != null) {
            NonEmptyMultiLangURI a = createNonEmptyMultiLangURI(NO.getCode(), informationUri.a());
            informationUris.add(a);
        }
        if (informationUri.b() != null) {
            NonEmptyMultiLangURI b = createNonEmptyMultiLangURI(EN.getCode(), informationUri.b());
            informationUris.add(b);
        }
        if (informationUri.c() != null) {
            NonEmptyMultiLangURI c = createNonEmptyMultiLangURI(NO.getCode(), informationUri.c());
            informationUris.add(c);
        }
        return informationUris;
    }

    /**
     *
     * @param trustedEntityservice the object we take data from.
     * @return etsi602 spec object populated with values from trustedEntityService in our config.
     */

    public static TrustedEntityService populateTrustedEntityService(no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService trustedEntityservice) {
        no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntityService trustedEntityService = new TrustedEntityService();
        ServiceInformation serviceInformation = populateServiceInformation(trustedEntityservice.serviceInformation());
        serviceInformation.setServiceDigitalIdentity(populateX509certificate(trustedEntityservice.serviceInformation().serviceDigitalIdentity()));
        trustedEntityService.setServiceInformation(serviceInformation);
        return trustedEntityService;
    }


    private static ServiceInformation populateServiceInformation(no.idporten.eudiw.trustlist.domain.etsi602.ServiceInformation serviceInfo) {
        ServiceInformation serviceInformation = new ServiceInformation();
        serviceInformation.setServiceName(populateServiceName(serviceInfo.serviceName()));
        if (serviceInfo.serviceTypeIdentifier() != null) {
            serviceInformation.setServiceTypeIdentifier(serviceInfo.serviceTypeIdentifier().a());
        }
        return serviceInformation;
    }

    /**
     *
     * @param serviceName
     */
    private static List<MultiLangString> populateServiceName(@Valid @NotNull ServiceName serviceName) {
        MultiLangString serviceNameNo = createMultiLangString(NO.getCode(), serviceName.langNo());
        MultiLangString serviceNameEn = createMultiLangString(EN.getCode(), serviceName.langEn());
        return List.of(serviceNameNo, serviceNameEn);
    }

    /**
     * Retrieves the certificate out from our property object, and into the service digital identity etsi602 object
     *
     * @param digitalIdentity ServiceDigitalIdentity from properties
     * @return the serviceDigitalIdentity spec object which we have inserted information from our trustedEntityService object
     */
    private static ServiceDigitalIdentity populateX509certificate(no.idporten.eudiw.trustlist.domain.etsi602.ServiceDigitalIdentity digitalIdentity) {
        ServiceDigitalIdentity serviceDigitalIdentity = new ServiceDigitalIdentity();
        List<PkiOb> list = new ArrayList<>();
        for (String cert : digitalIdentity.certs()) {
            PkiOb pkiOb = new PkiOb();
            pkiOb.setVal(cert);
            list.add(pkiOb);
        }
        serviceDigitalIdentity.setX509Certificates(list);
        return serviceDigitalIdentity;
    }
}
