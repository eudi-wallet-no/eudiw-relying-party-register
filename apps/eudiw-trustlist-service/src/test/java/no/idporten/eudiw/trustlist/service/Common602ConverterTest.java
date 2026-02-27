package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.domain.Address;
import no.idporten.eudiw.trustlist.etsi119602.pojo.MultiLangString;
import no.idporten.eudiw.trustlist.etsi119602.pojo.NonEmptyMultiLangURI;
import no.idporten.eudiw.trustlist.etsi119602.pojo.PostalAddress;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static no.idporten.eudiw.trustlist.service.LangCode.EN;
import static no.idporten.eudiw.trustlist.service.LangCode.NO;
import static org.junit.jupiter.api.Assertions.*;

class Common602ConverterTest {

    @Test
    void testCreateListOfElectronicAddresses() {
        DigdirProperties digdirProperties = new DigdirProperties("Testnavn NO", "Testnavn EN", "my@test.org", "https://www.test.org", null);

        List<NonEmptyMultiLangURI> listOfElectronicAddresses = Common602Converter.createListOfElectronicAddresses(digdirProperties);
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
}