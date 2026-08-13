package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.config.Trustlist612Properties;
import no.idporten.eudiw.trustlist.domain.Address;
import no.idporten.eudiw.trustlist.domain.etsi612.TLSchemeInformation;
import no.idporten.eudiw.trustlist.domain.etsi612.TLService;
import no.idporten.eudiw.trustlist.domain.etsi612.TLServiceProvider;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.etsi.uri._02231.v2_.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;

import static no.idporten.eudiw.trustlist.service.LangCode.EN;
import static no.idporten.eudiw.trustlist.service.LangCode.NO;

@Service
public class Trustlist612GeneratorService {

    public static final String TLS_TAG_URI = "http://uri.etsi.org/19612/TSLTag";
    public static final String TSL_TYPE_URI = "http://uri.etsi.org/TrstSvc/TrustedList/TSLType/EUgeneric";
    public static final String STATUS_DETERMINATION_APPROACH_URI = "http://uri.etsi.org/TrstSvc/TrustedList/StatusDetn/EUappropriate";
    public static final String SCHEME_TYPE_COMMUNITY_RULES_URI = "http://uri.etsi.org/TrstSvc/TrustedList/schemerules/EUcommon";

    private final Logger log = LoggerFactory.getLogger(Trustlist612GeneratorService.class);

    private final Trustlist612Properties properties;
    private final DigdirProperties digdirProperties;

    public Trustlist612GeneratorService(Trustlist612Properties properties, DigdirProperties digdirProperties) {
        this.properties = properties;
        this.digdirProperties = digdirProperties;
    }

    public TrustServiceStatusList generateTrustServiceStatusList() {
        TrustServiceStatusList trustServiceStatusList = new TrustServiceStatusList();
        trustServiceStatusList.setId("tsl");
        trustServiceStatusList.setTSLTag(TLS_TAG_URI);

        // 1. provide information on the issuing scheme;
        trustServiceStatusList.setSchemeInformation(createSchemeInformation());

        // • identify the TSPs recognized by the scheme;
        if (properties.serviceProviders() == null) {
            return trustServiceStatusList;
        }
        TrustServiceProviderList trustServiceProviderList = new TrustServiceProviderList();
        for (TLServiceProvider sp : properties.serviceProviders().values()) {
            TrustServiceProvider trustServiceProvider = createTrustServiceProvider(sp);
            trustServiceProviderList.getTrustServiceProviders().add(trustServiceProvider);
        }

        trustServiceStatusList.setTrustServiceProviderList(trustServiceProviderList);

        return trustServiceStatusList;
    }

    protected SchemeInformation createSchemeInformation() {
        // all info skal minimum på engelsk (en) og helst på språket til land som kontrollerer (no)
        SchemeInformation schemeInformation = new SchemeInformation();
        schemeInformation.setTSLVersionIdentifier(BigInteger.valueOf(6));
        TLSchemeInformation tlSchemeInformation = properties.schemeInformation();
        schemeInformation.setTSLSequenceNumber(tlSchemeInformation.sequenceNumber());

        schemeInformation.setTSLType(TSL_TYPE_URI);
        schemeInformation.setSchemeOperatorName(createInternationalNamesType(
                createMultiLangNormStringType(NO.getCode(), digdirProperties.nameNo()),
                createMultiLangNormStringType(EN.getCode(), digdirProperties.nameEn())));
        schemeInformation.setSchemeOperatorAddress(createDigdirAddressType());
        schemeInformation.setSchemeName(createInternationalNamesType(
                createMultiLangNormStringType(NO.getCode(), tlSchemeInformation.schemeName().langNo()),
                createMultiLangNormStringType(EN.getCode(), tlSchemeInformation.schemeName().langEn()
                )));
        if (tlSchemeInformation.informationUris() != null) { //obligatorisk felt, bør ha betre feilhåndtering
            schemeInformation.setSchemeInformationURI(new NonEmptyMultiLangURIListType());
            schemeInformation.getSchemeInformationURI().getURIS().add(createNonEmptyMultiLangURIType(NO.getCode(), tlSchemeInformation.informationUris().langNo()));
            schemeInformation.getSchemeInformationURI().getURIS().add(createNonEmptyMultiLangURIType(EN.getCode(), tlSchemeInformation.informationUris().langEn()));
        }
        schemeInformation.setStatusDeterminationApproach(STATUS_DETERMINATION_APPROACH_URI);
        schemeInformation.setSchemeTerritory("NO");
        schemeInformation.setSchemeTypeCommunityRules(new NonEmptyMultiLangURIListType());
        schemeInformation.getSchemeTypeCommunityRules().getURIS().add(createNonEmptyMultiLangURIType(NO.getCode(), SCHEME_TYPE_COMMUNITY_RULES_URI));
        schemeInformation.setHistoricalInformationPeriod(BigInteger.valueOf(65534));
        ZonedDateTime issuedDateTime = tlSchemeInformation.listIssueDateTime();
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
                createMultiLangNormStringType(NO.getCode(), serviceProviderData.tradeName().langNo()),
                createMultiLangNormStringType(EN.getCode(), serviceProviderData.tradeName().langEn())));
        tspInformation.setTSPInformationURI(createNonEmptyMultiLangURIListType(
                createNonEmptyMultiLangURIType(NO.getCode(), serviceProviderData.informationUris().langNo()),
                createNonEmptyMultiLangURIType(EN.getCode(), serviceProviderData.informationUris().langEn())));

        setAddresses(serviceProviderData, tspInformation);
        tspInformation.setTSPName(createInternationalNamesType(
                createMultiLangNormStringType(NO.getCode(), serviceProviderData.name().langNo()),
                createMultiLangNormStringType(EN.getCode(), serviceProviderData.name().langEn())));

        trustServiceProvider.setTSPInformation(tspInformation);

        if (serviceProviderData.services() == null) {
            return trustServiceProvider;
        }

        TSPServices tspServices = new TSPServices();
        for (TLService rpAccessService : serviceProviderData.services()) {
            tspServices.getTSPServices().add(createRpAccessTspService(rpAccessService));
        }
        trustServiceProvider.setTSPServices(tspServices);
        return trustServiceProvider;
    }

    private void setAddresses(TLServiceProvider serviceProviderData, TSPInformation tspInformation) {
        PostalAddresses postalAddresses = new PostalAddresses();
        postalAddresses.getPostalAddresses().add(createPostalAddress(serviceProviderData.postalAddress()));

        ElectronicAddress electronicAddress = createElectronicAddress(serviceProviderData.email(), serviceProviderData.website());

        AddressType addressType = new AddressType();
        addressType.setPostalAddresses(postalAddresses);
        addressType.setElectronicAddress(electronicAddress);
        tspInformation.setTSPAddress(addressType);
    }

    private TSPService createRpAccessTspService(TLService rpAccessService) {
        TSPService tspService = new TSPService();
        ServiceInformation serviceInformation = new ServiceInformation();
        serviceInformation.setServiceName(createInternationalNamesType(
                createMultiLangNormStringType(NO.getCode(), rpAccessService.name().langNo()),
                createMultiLangNormStringType(EN.getCode(), rpAccessService.name().langEn())));
        serviceInformation.setServiceTypeIdentifier(rpAccessService.serviceTypeIdentifier());
        try {
            serviceInformation.setServiceDigitalIdentity(createServiceDigitalIdentity(rpAccessService.getCertificate()));
        } catch (IOException e) {
            throw new ApplicationException("Failed to parse Certificate from string: %s ".formatted(rpAccessService.cert()), e);
        }
        serviceInformation.setServiceStatus(TLService.SERVICE_STATUS_URI);
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
            throw new ApplicationException("Failed to decode X509CertificateHolder with decimal SerialNumber: %d".formatted(certificate.getSerialNumber()), e);
        }
        serviceDigitalIdentity.getDigitalIds().add(digitalIdentityTypeSubjectName);
        serviceDigitalIdentity.getDigitalIds().add(digitalIdentityTypeX509Certificate);
        return serviceDigitalIdentity;
    }

    private AddressType createDigdirAddressType() {
        PostalAddresses postalAddresses = new PostalAddresses();
        postalAddresses.getPostalAddresses().add(createPostalAddress(digdirProperties.postalAddress()));

        ElectronicAddress electronicAddress = createElectronicAddress(digdirProperties.email(), digdirProperties.web());

        AddressType addressType = new AddressType();
        addressType.setPostalAddresses(postalAddresses);
        addressType.setElectronicAddress(electronicAddress);
        return addressType;
    }

    private @NonNull ElectronicAddress createElectronicAddress(String email, String website) {
        ElectronicAddress electronicAddress = new ElectronicAddress();
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType(NO.getCode(), email));
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType(EN.getCode(), email));
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType(NO.getCode(), website));
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType(EN.getCode(), website));
        return electronicAddress;
    }

    private static @NonNull PostalAddress createPostalAddress(Address digdirAddress) {
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setLang(NO.getCode());
        postalAddress.setStreetAddress(digdirAddress.streetAddress());
        postalAddress.setPostalCode(digdirAddress.postalCode());
        postalAddress.setLocality(digdirAddress.locality());
        postalAddress.setCountryName(digdirAddress.country());
        return postalAddress;
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
