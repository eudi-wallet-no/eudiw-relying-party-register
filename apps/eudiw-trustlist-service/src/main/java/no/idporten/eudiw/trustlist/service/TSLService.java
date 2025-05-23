package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.etsi_ts_119_612.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigInteger;
import java.time.ZonedDateTime;

@Service
public class TSLService {

    // viser generell opbygning av liste
    public TrustServiceStatusList generateTrustServiceStatusList() throws Exception {
        TrustServiceStatusList trustServiceStatusList = new TrustServiceStatusList();
        trustServiceStatusList.setId("tsl");
        trustServiceStatusList.setTSLTag("http://uri.etsi.org/19612/TSLTag");

        // 1. provide information on the issuing scheme;
        trustServiceStatusList.setSchemeInformation(createSchemeInformation());

        // • identify the TSPs recognized by the scheme;
        TrustServiceProviderList trustServiceProviderList = new TrustServiceProviderList();
        TrustServiceProvider trustServiceProvider = createTrustServiceProvider();
        trustServiceProviderList.getTrustServiceProviders().add(trustServiceProvider);
        trustServiceStatusList.setTrustServiceProviderList(trustServiceProviderList);

        return trustServiceStatusList;
    }

    // viser hvordan meta-informasjon om tjenesten kan lages
    protected SchemeInformation createSchemeInformation() {
        // sekvens skal øke pr gang genereres
        // varighet må beregnes/økes
        // all info skal minimum på en og helst på språket til land som kontrollerer
        // dette er de aller fleste feltene, noen
        SchemeInformation schemeInformation = new SchemeInformation();
        schemeInformation.setTSLVersionIdentifier(BigInteger.valueOf(3));
        schemeInformation.setTSLSequenceNumber(BigInteger.ONE);
        schemeInformation.setTSLType("http://uri.etsi.org/TrstSvc/TrustedList/TSLType/EUgeneric");
        schemeInformation.setSchemeOperatorName(createInternationalNamesType(
                createMultiLangNormStringType("no", "Digitaliseringsdirektoratet"),
                createMultiLangNormStringType("en", "The Norwegian Digitalisation Agency")));
        schemeInformation.setSchemeOperatorAddress(createDigdirAddressType());
        schemeInformation.setSchemeName(createInternationalNamesType(
                createMultiLangNormStringType("no", "Trusted list for eidas2sandkasse.dev"),
                createMultiLangNormStringType("en", "Trusted list for eidas2sandkasse.dev")
        ));
        schemeInformation.setSchemeInformationURI(new NonEmptyMultiLangURIListType());
        schemeInformation.getSchemeInformationURI().getURIS().add(createNonEmptyMultiLangURIType("no", "https://www.digdir.no/"));
        schemeInformation.getSchemeInformationURI().getURIS().add(createNonEmptyMultiLangURIType("en", "https://www.digdir.no/"));
        schemeInformation.setStatusDeterminationApproach("http://uri.etsi.org/TrstSvc/TSLType/StatusDetn/active");
        schemeInformation.setSchemeTerritory("NO");
        schemeInformation.setHistoricalInformationPeriod(BigInteger.valueOf(65534));
        schemeInformation.setListIssueDateTime(ZonedDateTime.now());
        return schemeInformation;
    }

    // viser oppbygging av en trust service provider
    private TrustServiceProvider createTrustServiceProvider() throws Exception {
        TrustServiceProvider trustServiceProvider = new TrustServiceProvider();
        TSPInformation tspInformation = new TSPInformation();
        tspInformation.setTSPAddress(createDigdirAddressType());
        tspInformation.setTSPName(createInternationalNamesType(
                createMultiLangNormStringType("no", "Digitaliseringsdirektoratet"),
                createMultiLangNormStringType("en", "The Norwegian Digitalisation Agency")));
        trustServiceProvider.setTSPInformation(tspInformation);
        TSPServices tspServices = new TSPServices();
        TSPService tspService = createTspService();
        tspServices.getTSPServices().add(tspService);
        trustServiceProvider.setTSPInformation(tspInformation);
        trustServiceProvider.setTSPServices(tspServices);
        return trustServiceProvider;
    }

    // viser oppbyggingen av en tsp service
    private TSPService createTspService() throws Exception {
        TSPService tspService = new TSPService();
        ServiceInformation serviceInformation = new ServiceInformation();
        serviceInformation.setServiceName(createInternationalNamesType(
                createMultiLangNormStringType("en", "Root CA for eidas2sandkasse.dev")));
        serviceInformation.setServiceTypeIdentifier("????rp/access????");
        ServiceDigitalIdentity serviceDigitalIdentity = createServiceDigitalIdentity();
        serviceInformation.setServiceDigitalIdentity(serviceDigitalIdentity);
        serviceInformation.setServiceStatus("http://uri.etsi.org/TrstSvc/Svcstatus/inaccord");
        serviceInformation.setStatusStartingTime(ZonedDateTime.now());
        ExtensionsListType extensionsListType = new ExtensionsListType();
        Extension extension = new Extension();
        extension.setCritical(true);
        extensionsListType.getExtensions().add(extension);
        serviceInformation.setServiceInformationExtensions(extensionsListType);
        ServiceHistory serviceHistory = new ServiceHistory();
        tspService.setServiceInformation(serviceInformation);
        tspService.setServiceHistory(serviceHistory);
        return tspService;
    }

    // viser hvordan service digital identity kan bygges fra sertifikater
    protected ServiceDigitalIdentity createServiceDigitalIdentity() throws Exception {
        String cert = """
                -----BEGIN CERTIFICATE-----
                MIIFhjCCA26gAwIBAgIJAPbsfA8ONFcRMA0GCSqGSIb3DQEBDAUAMGIxGDAWBgNV
                BGETD05UUk5PLTk5MTgyNTgyNzELMAkGA1UEBhMCbm8xDzANBgNVBAsTBkRpZ2Rp
                cjEoMCYGA1UEAxMfZWlkYXMyc2FuZGthc3NlIHJvb3QgQ0Egc3lzdGVzdDAeFw0y
                NTAzMzExMTM1MDdaFw0zMDAzMzAxMTM1MDdaMGIxGDAWBgNVBGETD05UUk5PLTk5
                MTgyNTgyNzELMAkGA1UEBhMCbm8xDzANBgNVBAsTBkRpZ2RpcjEoMCYGA1UEAxMf
                ZWlkYXMyc2FuZGthc3NlIHJvb3QgQ0Egc3lzdGVzdDCCAiIwDQYJKoZIhvcNAQEB
                BQADggIPADCCAgoCggIBANlJnsGmLcHCj+NOYrqFfTq4rn97WlbvFRvMVcVgeeEU
                pqjLwVWL+qs1uZSrkxs1zAqj2FQ++RcyJzPfLx5zv7MmNdltCOeK7d31Wf+f8qCQ
                3fiRlcXgMA81dqfRDuMHNDpMcASx+sWCNomRvnLkJMHdPZipfIeuPt0R5EYCFhDx
                esghnzVJynyJz4EmaLO2aIEsf1UqadTt49bCPr+jyhaZlSFtnhrje3TWTnmSLjpb
                CiMFDtui8pDRp59ECXz9r8J0xzWsCRp/vX8BstVjZxMfLU6U34zD/GHT3onr/gAH
                l+5Suc/jQO1oUG4mjYP+xLUE5geuqFFJ7/VoGozgTLm+WQgIcKfYtJlL1frfbw/c
                Do/Db0hUWbsWSIbGkIfvzdPBSEfL53nnAFs0Np2VHJr6dAEjRnB6tmwgXd3bpPj5
                KmiCwEzJteTD4cCVBZu4vE+dwAzz0YnX7nkn70GBEIUsrsTCbmGUk/Piq1CFtTEK
                CF4a5kdBlsRu+swCHw051S94jJ5TgbcaS1gebiqDHKH13hNSDy9HXU0ynPz4nHKI
                u0Wt44Zy9aRcsA3fvvt+QKRcAOCAO3CS3YG2uHeZbrCAbFXYtKlQgJl0OrmxuXSx
                FKjoVQeqP1YV6bJf7grQMMD66CL6cYj/zUDtuWN6qAf9LsVt6XjNh0rqr7zD3xaJ
                AgMBAAGjPzA9MB0GA1UdDgQWBBSDLHefDZmzEO55p/SamaAPSuTKTTAOBgNVHQ8B
                Af8EBAMCAQYwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQwFAAOCAgEAiSseiZmT
                P4TJXPj2GSAdGp9s9erLuM30h1xF8vhvdIfZdN4yNPlUBuIYbfMDbdcnOELwL9Sx
                VlwEDKF4+ZTQpjmO1JwDEFmZBghUaUBH/TrjSoN/oqq/cEruu0TJViy04daqM2B7
                +J+L73P+5dxodzlBmGuaN6jEHu/RsP/p5uowDEXmRaYZm4hOFYOq2XgLmOFwIoiK
                cmPXbS3nzXjBdshjq68fndoGyBknD/ppUL6Be1jhjH3zH0yGDGEkrcUgLA2YhF4w
                GfHg7x0oV8y+xBw3c6rrezP4OTv5ehPi6bejfoGuAAzdaGIdQzQSioGM6wV+73+I
                Q6OBXsQS/jnPvr3RwpImCeW5EqKJ9HK1i2RSkMlMc6sWRMTOH0nNcyHlv3lH6R5N
                a6PpYGSfgzdLl5d0BYar51o+WeKSlegjF5gg1FoOMA1ktoknnKz2Fuw7qlZ51mNQ
                Gp3SGuAfNjjVXHCa6zISNj+ROU3aZgJApRH2NmATLkWo3N13scpDq9n0ajmPgS/1
                VN3Feby6zO8GGYUCZdWfwTce8j7kbtNgjxhm/xucmY+N5rg0J9qNZ124arOymzym
                l3tDrknoSnRMtPzG6ZA4YrTJUeV7swjODUO5ltJjscRi+xnJM92fvXjBjuwbJg38
                PmK78RkxHeopCG4Hh9MyCycOqbQnPVSp6jY=
                -----END CERTIFICATE-----
                """;
        PEMParser pemParser = new PEMParser(new StringReader(cert));
        X509CertificateHolder certificate = (X509CertificateHolder) pemParser.readObject();
        ServiceDigitalIdentity serviceDigitalIdentity = new ServiceDigitalIdentity();
        DigitalIdentityType digitalIdentityTypeSubjectName = new DigitalIdentityType();
        digitalIdentityTypeSubjectName.setX509SubjectName(certificate.getSubject().toString());
        DigitalIdentityType digitalIdentityTypeX509Certificate = new DigitalIdentityType();
        digitalIdentityTypeX509Certificate.setX509Certificate(certificate.getEncoded());
        serviceDigitalIdentity.getDigitalIds().add(digitalIdentityTypeSubjectName);
        serviceDigitalIdentity.getDigitalIds().add(digitalIdentityTypeX509Certificate);
        return serviceDigitalIdentity;
    }

    // Det er ganske komplisert å bygge typene, bli rmye skyfkling av data fra konfig e.l.
    private AddressType createDigdirAddressType() {
        PostalAddresses postalAddresses = new PostalAddresses();
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setLang("no");
        postalAddress.setStreetAddress("Lørenfaret 1 C");
        postalAddress.setPostalCode("0580");
        postalAddress.setLocality("Oslo");
        postalAddress.setCountryName("NO");

        ElectronicAddress electronicAddress = new ElectronicAddress();
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType("en", "servicedesk@digdir.no"));
        electronicAddress.getURIS().add(createNonEmptyMultiLangURIType("en", "https://www.digdir.no/"));
        AddressType addressType = new AddressType();
        postalAddresses.getPostalAddresses().add(postalAddress);
        addressType.setPostalAddresses(postalAddresses);
        addressType.setElectronicAddress(electronicAddress);
        return addressType;
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

}
