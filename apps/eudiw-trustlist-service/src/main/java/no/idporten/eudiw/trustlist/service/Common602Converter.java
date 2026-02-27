package no.idporten.eudiw.trustlist.service;

import jakarta.validation.constraints.NotEmpty;
import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.domain.Address;
import no.idporten.eudiw.trustlist.etsi119602.pojo.MultiLangString;
import no.idporten.eudiw.trustlist.etsi119602.pojo.NonEmptyMultiLangURI;
import no.idporten.eudiw.trustlist.etsi119602.pojo.PostalAddress;

import java.net.URI;
import java.util.List;

import static no.idporten.eudiw.trustlist.service.LangCode.EN;
import static no.idporten.eudiw.trustlist.service.LangCode.NO;

public class Common602Converter {

    public static List<NonEmptyMultiLangURI> createListOfElectronicAddresses(DigdirProperties digdirProperties) {
        NonEmptyMultiLangURI emailNo = createNonEmptyMultiLangURI(NO.getCode(), digdirProperties.email());
        NonEmptyMultiLangURI emailEn = createNonEmptyMultiLangURI(EN.getCode(), digdirProperties.email());
        NonEmptyMultiLangURI webPageNo = createNonEmptyMultiLangURI(NO.getCode(), digdirProperties.web());
        NonEmptyMultiLangURI webPageEn = createNonEmptyMultiLangURI(EN.getCode(), digdirProperties.web());

        return List.of(emailNo, emailEn, webPageNo, webPageEn);
    }

    public static PostalAddress createPostalAddress(Address address) {
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setLang(NO.getCode());
        postalAddress.setStreetAddress(address.streetAddress());
        postalAddress.setPostalCode(address.postalCode());
        postalAddress.setLocality(address.locality());
        postalAddress.setCountry(address.country());
        return postalAddress;
    }

    public static NonEmptyMultiLangURI createNonEmptyMultiLangURI(String lang, String uriString) {
        NonEmptyMultiLangURI langURI = new NonEmptyMultiLangURI();
        langURI.setLang(lang);
        langURI.setUriValue(URI.create(uriString));
        return langURI;
    }

    public static MultiLangString createMultiLangString(String lang, @NotEmpty String value) {
        MultiLangString langString = new MultiLangString();
        langString.setLang(lang);
        langString.setValue(value);
        return langString;
    }
}
