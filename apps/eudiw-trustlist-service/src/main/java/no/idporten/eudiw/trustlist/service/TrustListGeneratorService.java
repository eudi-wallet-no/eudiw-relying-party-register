package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.config.TrustlistServiceProperties;
import no.idporten.eudiw.trustlist.domain.TLRpAccessService;
import no.idporten.eudiw.trustlist.domain.TLServiceProvider;
import no.idporten.eudiw.trustlist.web.ApplicationException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.etsi.uri._02231.v2_.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;

@Service
public class TrustListGeneratorService {

    public static final String TLS_TAG_URI = "http://uri.etsi.org/19612/TSLTag";
    public static final String TSL_TYPE_URI = "http://uri.etsi.org/TrstSvc/TrustedList/TSLType/EUgeneric";
    public static final String STATUS_DETERMINATION_APPROACH_URI = "http://uri.etsi.org/TrstSvc/TrustedList/StatusDetn/EUappropriate";
    public static final String SCHEME_TYPE_COMMUNITY_RULES_URI = "http://uri.etsi.org/TrstSvc/TrustedList/schemerules/EUcommon";

    public static final String DIGITALISERINGSDIREKTORATET_LEGAL_NAME_NO = "Digitaliseringsdirektoratet";
    public static final String DIGITALISERINGSDIREKTORATET_LEGAL_NAME_EN = "Norwegian Digitalisation Agency";
    public static final String LANG_CODE_NO = "no";
    public static final String LANG_CODE_EN = "en";


    private final Logger log = LoggerFactory.getLogger(TrustListGeneratorService.class);

    private final TrustlistServiceProperties properties;

    public TrustListGeneratorService(TrustlistServiceProperties properties) {
        this.properties = properties;
    }

    public TrustServiceStatusList generateTrustServiceStatusList() {
        TrustServiceStatusList trustServiceStatusList = new TrustServiceStatusList();
        trustServiceStatusList.setId("tsl");
        trustServiceStatusList.setTSLTag(TLS_TAG_URI);

        // 1. provide information on the issuing scheme;
        trustServiceStatusList.setSchemeInformation(createSchemeInformation());

        // • identify the TSPs recognized by the scheme;
        TrustServiceProviderList trustServiceProviderList = new TrustServiceProviderList();
        TrustServiceProvider trustServiceProvider = createTrustServiceProvider(properties.getServiceProvider());
        trustServiceProviderList.getTrustServiceProviders().add(trustServiceProvider);
        trustServiceStatusList.setTrustServiceProviderList(trustServiceProviderList);

        return trustServiceStatusList;
    }

    protected SchemeInformation createSchemeInformation() {
        // all info skal minimum på engelsk (en) og helst på språket til land som kontrollerer (no)
        SchemeInformation schemeInformation = new SchemeInformation();
        schemeInformation.setTSLVersionIdentifier(BigInteger.valueOf(6));
        schemeInformation.setTSLSequenceNumber(properties.getSchemeInformation().sequenceNumber());

        schemeInformation.setTSLType(TSL_TYPE_URI);
        schemeInformation.setSchemeOperatorName(createInternationalNamesType(
                createMultiLangNormStringType(LANG_CODE_NO, DIGITALISERINGSDIREKTORATET_LEGAL_NAME_NO),
                createMultiLangNormStringType(LANG_CODE_EN, DIGITALISERINGSDIREKTORATET_LEGAL_NAME_EN)));
        schemeInformation.setSchemeOperatorAddress(createDigdirAddressType());
        schemeInformation.setSchemeName(createInternationalNamesType(
                createMultiLangNormStringType(LANG_CODE_NO, "Tillitsliste for eidas2sandkasse.net"),
                createMultiLangNormStringType(LANG_CODE_EN, "Trust list for eidas2sandkasse.net")
        ));
        schemeInformation.setSchemeInformationURI(new NonEmptyMultiLangURIListType());
        schemeInformation.getSchemeInformationURI().getURIS().add(createNonEmptyMultiLangURIType(LANG_CODE_NO, "https://www.digdir.no/"));
        schemeInformation.getSchemeInformationURI().getURIS().add(createNonEmptyMultiLangURIType(LANG_CODE_EN, "https://www.digdir.no/"));
        schemeInformation.setStatusDeterminationApproach(STATUS_DETERMINATION_APPROACH_URI);
        schemeInformation.setSchemeTerritory("NO");
        schemeInformation.setSchemeTypeCommunityRules(new NonEmptyMultiLangURIListType());
        schemeInformation.getSchemeTypeCommunityRules().getURIS().add(createNonEmptyMultiLangURIType(LANG_CODE_NO, SCHEME_TYPE_COMMUNITY_RULES_URI));
        schemeInformation.setHistoricalInformationPeriod(BigInteger.valueOf(65534));
        ZonedDateTime issuedDateTime = properties.getSchemeInformation().listIssueDateTime();
        schemeInformation.setListIssueDateTime(issuedDateTime);
        NextUpdate nextUpdate = new NextUpdate();
        nextUpdate.setDateTime(issuedDateTime.plusMonths(6));
        schemeInformation.setNextUpdate(nextUpdate);

        //TODO: move validation elsewhere/generalize?
        ZonedDateTime now = ZonedDateTime.now();
        if (nextUpdate.getDateTime().isBefore(now)) {
            log.error("List is expire and invalid since not updated in 6 months, nextUpdate is in the past: {}", nextUpdate.getDateTime());
        } else if (nextUpdate.getDateTime().minusWeeks(1).isBefore(now)) {
            log.warn("List is about to expire, nextUpdate is less than 1 week away: {}", nextUpdate.getDateTime());
        }

        return schemeInformation;
    }

    private TrustServiceProvider createTrustServiceProvider(TLServiceProvider serviceProviderData) {
        TrustServiceProvider trustServiceProvider = new TrustServiceProvider();
        TSPInformation tspInformation = new TSPInformation();

        tspInformation.setTSPTradeName(createInternationalNamesType(
                createMultiLangNormStringType(LANG_CODE_NO, serviceProviderData.tradeName().langNo()),
                createMultiLangNormStringType(LANG_CODE_EN, serviceProviderData.tradeName().langEn())));
        tspInformation.setTSPInformationURI(createNonEmptyMultiLangURIListType(
                createNonEmptyMultiLangURIType(LANG_CODE_NO, serviceProviderData.informationUri().langNo())));
        tspInformation.setTSPAddress(createDigdirAddressType()); // still hard-coded to Digdir address, should be configurable
        tspInformation.setTSPName(createInternationalNamesType(
                createMultiLangNormStringType(LANG_CODE_NO, serviceProviderData.name().langNo()),
                createMultiLangNormStringType(LANG_CODE_EN, serviceProviderData.name().langEn())));

        trustServiceProvider.setTSPInformation(tspInformation);

        TSPServices tspServices = new TSPServices();
        for (TLRpAccessService rpAccessService : serviceProviderData.rpAccessServices()) {
            tspServices.getTSPServices().add(createRpAccessTspService(rpAccessService));
        }
        trustServiceProvider.setTSPServices(tspServices);
        return trustServiceProvider;
    }

    private TSPService createRpAccessTspService(TLRpAccessService rpAccessService)  {
        TSPService tspService = new TSPService();
        ServiceInformation serviceInformation = new ServiceInformation();
        serviceInformation.setServiceName(createInternationalNamesType(
                createMultiLangNormStringType(LANG_CODE_NO, rpAccessService.name().langNo()),
                createMultiLangNormStringType(LANG_CODE_EN, rpAccessService.name().langEn())));
        serviceInformation.setServiceTypeIdentifier(TLRpAccessService.SERVICE_TYPE_IDENTIFIER_URI_RP_ACCESS);
        try {
            serviceInformation.setServiceDigitalIdentity(createServiceDigitalIdentity(rpAccessService.getCertificate()));
        } catch (IOException e) {
            throw new ApplicationException("Failed to parse Certificate from string: %s ".formatted(rpAccessService.cert()), e);
        }
        serviceInformation.setServiceStatus(TLRpAccessService.SERVICE_STATUS_URI);
        serviceInformation.setStatusStartingTime(rpAccessService.startingTime());
        tspService.setServiceInformation(serviceInformation);

        // add history on first change of service (new version):
        //   ServiceHistory serviceHistory = new ServiceHistory();
        //   tspService.setServiceHistory(serviceHistory);
        return tspService;
    }

    protected ServiceDigitalIdentity createServiceDigitalIdentity(X509CertificateHolder certificate) {
        ServiceDigitalIdentity serviceDigitalIdentity = new ServiceDigitalIdentity();
        DigitalIdentityType digitalIdentityTypeSubjectName = new DigitalIdentityType();
        digitalIdentityTypeSubjectName.setX509SubjectName(certificate.getSubject().toString());
        DigitalIdentityType digitalIdentityTypeX509Certificate = new DigitalIdentityType();
        try {
            digitalIdentityTypeX509Certificate.setX509Certificate(certificate.getEncoded());
        } catch (IOException e) {
            throw new ApplicationException("Failed to decode X509CertificateHolder with decimal SerialNumber: %d".formatted(certificate.getSerialNumber()),e);
        }
        serviceDigitalIdentity.getDigitalIds().add(digitalIdentityTypeSubjectName);
        serviceDigitalIdentity.getDigitalIds().add(digitalIdentityTypeX509Certificate);
        return serviceDigitalIdentity;
    }

    // Hardkpder adresse for Digdir, ut i konfig eller database senere?
    private AddressType createDigdirAddressType() {
        PostalAddresses postalAddresses = new PostalAddresses();
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setLang(LANG_CODE_NO);
        postalAddress.setStreetAddress("Lørenfaret 1C");
        postalAddress.setPostalCode("0580");
        postalAddress.setLocality("Oslo");
        postalAddress.setCountryName("NO");

        ElectronicAddress electronicAddress = new ElectronicAddress();
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType(LANG_CODE_NO, "mailto:servicedesk@digdir.no"));
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType(LANG_CODE_EN, "mailto:servicedesk@digdir.no"));
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType(LANG_CODE_NO, "https://www.digdir.no/"));
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType(LANG_CODE_EN, "https://www.digdir.no/"));
        AddressType addressType = new AddressType();
        postalAddresses.getPostalAddresses().add(postalAddress);
        addressType.setPostalAddresses(postalAddresses);
        addressType.setElectronicAddress(electronicAddress);
        return addressType;
    }

    private NonEmptyMultiLangURIType createNonEmptyMultiLangURIType(String lang, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        NonEmptyMultiLangURIType nonEmptyMultiLangURIType = new NonEmptyMultiLangURIType();
        nonEmptyMultiLangURIType.setLang(lang);
        nonEmptyMultiLangURIType.setValue(value);
        return nonEmptyMultiLangURIType;
    }

    private InternationalNamesType createInternationalNamesType(MultiLangNormStringType... values) {
        InternationalNamesType internationalNamesType = new InternationalNamesType();
        for (MultiLangNormStringType value : values) {
            if (value != null) {
                internationalNamesType.getNames().add(value);
            }
        }
        return internationalNamesType;
    }

    private NonEmptyMultiLangURIListType createNonEmptyMultiLangURIListType(NonEmptyMultiLangURIType... values) {
        NonEmptyMultiLangURIListType internationalNamesType = new NonEmptyMultiLangURIListType();
        for (NonEmptyMultiLangURIType value : values) {
            if (value != null) {
                internationalNamesType.getURIS().add(value);
            }
        }
        return internationalNamesType;
    }

    private static MultiLangNormStringType createMultiLangNormStringType(String lang, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        MultiLangNormStringType multiLangNormStringType = new MultiLangNormStringType();
        multiLangNormStringType.setLang(lang);
        multiLangNormStringType.setValue(value);
        return multiLangNormStringType;
    }

}
