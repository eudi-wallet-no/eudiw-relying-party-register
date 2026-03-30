package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.domain.Address;
import no.idporten.eudiw.trustlist.domain.TSName;
import no.idporten.eudiw.trustlist.domain.etsi602.TeAddress;
import no.idporten.eudiw.trustlist.etsi119602.pojo.MultiLangString;
import no.idporten.eudiw.trustlist.etsi119602.pojo.NonEmptyMultiLangURI;
import no.idporten.eudiw.trustlist.etsi119602.pojo.PostalAddress;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

import static no.idporten.eudiw.trustlist.service.LangCode.EN;
import static no.idporten.eudiw.trustlist.service.LangCode.NO;
import static org.junit.jupiter.api.Assertions.*;

class Common602ConverterTest {

    @Test
    void testCreateListOfElectronicAddresses() {
        DigdirProperties digdirProperties = new DigdirProperties("Testnavn NO", "Testnavn EN", "my@test.org", "https://www.test.org", null);

        List<NonEmptyMultiLangURI> listOfElectronicAddresses = Common602Converter.createListOfElectronicAddresses(digdirProperties.email(), digdirProperties.web());
        assertNotNull(listOfElectronicAddresses);
        assertEquals(4, listOfElectronicAddresses.size());

        // norwegian values for email and web page
        List<URI> noValues = listOfElectronicAddresses.stream().filter(uri -> uri.getLang().equals(NO.getCode())).map(NonEmptyMultiLangURI::getUriValue).toList();
        assertNotNull(noValues);
        assertEquals(2, noValues.size());
        assertTrue(noValues.contains(URI.create("my@test.org")));
        assertTrue(noValues.contains(URI.create("https://www.test.org")));

        // english values for email and web page, same as norwegian values
        List<URI> enValues = listOfElectronicAddresses.stream().filter(uri -> uri.getLang().equals(EN.getCode())).map(NonEmptyMultiLangURI::getUriValue).toList();
        assertNotNull(enValues);
        assertEquals(2, enValues.size());
        assertTrue(enValues.contains(URI.create("my@test.org")));
        assertTrue(enValues.contains(URI.create("https://www.test.org")));
    }

    @Test
    void testCreatePostalAddress() {
        Address adr = new Address("Testveien 1", "1234", "Oslo", "NO");

        PostalAddress postalAddress = Common602Converter.createPostalAddress(adr);
        assertNotNull(postalAddress);
        assertEquals(adr.streetAddress(), postalAddress.getStreetAddress());
        assertEquals(adr.postalCode(), postalAddress.getPostalCode());
        assertEquals(adr.locality(), postalAddress.getLocality());
        assertEquals(adr.country(), postalAddress.getCountry());
    }

    @Test
    void testCreateNonEmptyMultiLangURI() {
        String uriString = "https://www.test.org";
        NonEmptyMultiLangURI multiLangURI = Common602Converter.createNonEmptyMultiLangURI(EN.getCode(), uriString);
        assertNotNull(multiLangURI);
        assertEquals(EN.getCode(), multiLangURI.getLang());
        assertEquals(uriString, multiLangURI.getUriValue().toString());
    }

    @Test
    void testCreateMultiLangString() {
        MultiLangString multiLangString = Common602Converter.createMultiLangString(EN.getCode(), "Test value");
        assertNotNull(multiLangString);
        assertEquals(EN.getCode(), multiLangString.getLang());
        assertEquals("Test value", multiLangString.getValue());
    }

        @Test
        void testCreateInformationURIsWithSize2() {
            no.idporten.eudiw.trustlist.domain.TSUri tsUri =
                    new no.idporten.eudiw.trustlist.domain.TSUri("https://example.no/info", "https://example.com/info");

            List<NonEmptyMultiLangURI> uris = Common602Converter.createInformationURIs(tsUri);

            assertNotNull(uris);
            assertEquals(2, uris.size());
            assertEquals(URI.create("https://example.no/info"), uris.get(0).getUriValue());
            assertEquals(URI.create("https://example.com/info"), uris.get(1).getUriValue());
        }

        @Test
        void testCreateListAndSchemeInformation() {
            Address adr = new Address("Testveien 1", "1234", "Oslo", "NO");
            DigdirProperties digdirProperties =
                    new DigdirProperties("Testnavn NO", "Testnavn EN", "my@test.org", "https://www.test.org", adr);

            no.idporten.eudiw.trustlist.domain.etsi602.ListAndSchemeInformation domainInfo =
                    new no.idporten.eudiw.trustlist.domain.etsi602.ListAndSchemeInformation(
                            new TSName("Ordning NO", "Scheme EN"),
                            BigInteger.ONE,
                            ZonedDateTime.parse("2025-01-01T10:15:30Z"),
                            URI.create("http://PID"),
                            new no.idporten.eudiw.trustlist.domain.TSUri("https://example.no/info", "https://example.com/info"),
                            URI.create("urn://MANUAL"),
                            "https://example.com/rules"
                    );

            no.idporten.eudiw.trustlist.etsi119602.pojo.ListAndSchemeInformation result =
                    Common602Converter.createListAndSchemeInformation(domainInfo, digdirProperties);

            assertNotNull(result);
            assertEquals(1, result.getLoTEVersionIdentifier());
            assertEquals(1, result.getLoTESequenceNumber());
            assertNotNull(result.getSchemeOperatorName());
            assertNotNull(result.getSchemeOperatorAddress());
            assertNotNull(result.getSchemeName());
            assertNotNull(result.getSchemeInformationURI());
        }

        @Test
        void testPopulateTrustedEntityInformationWithPhone() {
            Address adr = new Address("Testveien 1", "1234", "Oslo", "NO");
            DigdirProperties digdirProperties =
                    new DigdirProperties("Testnavn NO", "Testnavn EN", "my@test.org", "https://www.test.org", adr);

            no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityInformation domainInfo =
                    new no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityInformation(
                            "Entity Name",
                            "Entity Trade Name",
                            new no.idporten.eudiw.trustlist.domain.etsi602.InformationUri("https://a.no", null, "https://c.com"),
                            new TeAddress("55555555")
                    );

            var result = Common602Converter.populateTrustedEntityInformation(digdirProperties, domainInfo);

            assertNotNull(result);
            assertNotNull(result.getTEName());
            assertNotNull(result.getTETradeName());
            assertNotNull(result.getTEAddress());
            assertNotNull(result.getTEInformationURI());
            assertNotNull(result.getTEAddress().getTEElectronicAddress());
            assertEquals(6, result.getTEAddress().getTEElectronicAddress().size());
            assertEquals(2, result.getTEName().size());
            assertEquals(2, result.getTETradeName().size());
        }
        @Test
        void testPopulateTrustedEntityInformationWithoutPhone() {
            Address adr = new Address("Testveien 1", "1234", "Oslo", "NO");
            DigdirProperties digdirProperties =
                    new DigdirProperties("Testnavn NO", "Testnavn EN", "my@test.org", "https://www.test.org", adr);

            no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityInformation domainInfo =
                    new no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityInformation(
                            "Entity Name",
                            "Entity Trade Name",
                            new no.idporten.eudiw.trustlist.domain.etsi602.InformationUri("https://a.no", null, "https://c.com"),
                            null
                    );

            var result = Common602Converter.populateTrustedEntityInformation(digdirProperties, domainInfo);

            assertNotNull(result);
            assertNotNull(result.getTEName());
            assertNotNull(result.getTETradeName());
            assertNotNull(result.getTEAddress());
            assertNotNull(result.getTEInformationURI());
            assertNotNull(result.getTEAddress().getTEElectronicAddress());
            assertEquals(4, result.getTEAddress().getTEElectronicAddress().size());
            assertEquals(2, result.getTEName().size());
            assertEquals(2, result.getTETradeName().size());
        }

        @Test
        void testPopulateTrustedEntityService() {
            no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService domainService =
                    new no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService(
                            new no.idporten.eudiw.trustlist.domain.etsi602.ServiceInformation(
                                    null,
                                    new no.idporten.eudiw.trustlist.domain.etsi602.ServiceName("Tjeneste NO", "Service EN"),
                                    new no.idporten.eudiw.trustlist.domain.etsi602.ServiceDigitalIdentity("BASE64CERT")
                            )
                    );

            var result = Common602Converter.populateTrustedEntityService(domainService);

            assertNotNull(result);
            assertNotNull(result.getServiceInformation());
            assertNotNull(result.getServiceInformation().getServiceName());
            assertNull(result.getServiceInformation().getServiceTypeIdentifier());
            assertNotNull(result.getServiceInformation().getServiceDigitalIdentity());
            assertEquals(1, result.getServiceInformation().getServiceDigitalIdentity().getX509Certificates().size());
            assertEquals("BASE64CERT",
                    result.getServiceInformation().getServiceDigitalIdentity().getX509Certificates().getFirst().getVal());
        }
        @Test
        void testPopulateTrustedEntityServiceWithServiceType() {
            no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService domainService =
                    new no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService(
                            new no.idporten.eudiw.trustlist.domain.etsi602.ServiceInformation(
                                    new no.idporten.eudiw.trustlist.domain.etsi602.ServiceTypeIdentifier(URI.create("urn:test:type")),
                                    new no.idporten.eudiw.trustlist.domain.etsi602.ServiceName("Tjeneste NO", "Service EN"),
                                    new no.idporten.eudiw.trustlist.domain.etsi602.ServiceDigitalIdentity("BASE64CERT")
                            )
                    );

            var result = Common602Converter.populateTrustedEntityService(domainService);

            assertNotNull(result);
            assertNotNull(result.getServiceInformation());
            assertNotNull(result.getServiceInformation().getServiceName());
            assertNotNull(result.getServiceInformation().getServiceDigitalIdentity());
            assertNotNull(result.getServiceInformation().getServiceTypeIdentifier());
            assertEquals(1, result.getServiceInformation().getServiceDigitalIdentity().getX509Certificates().size());
            assertEquals("BASE64CERT",
                    result.getServiceInformation().getServiceDigitalIdentity().getX509Certificates().getFirst().getVal());
        }
}