package no.idporten.eudiw.trustlist.service;

import org.etsi.uri._02231.v2_.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("Trustlist is generated with content")
class TrustlistGeneratorServiceTest {

    @Autowired
    TrustlistGeneratorService trustListGeneratorService;

    @Test
    @DisplayName("for first ServiceProvider and it has content for first Service")
    void verifyGenerateTrustListIsHasContent() {
        TrustServiceStatusList trustlist = trustListGeneratorService.generateTrustServiceStatusList();
        assertNotNull(trustlist);
        assertNotNull(trustlist.getTrustServiceProviderList());
        assertNotNull(trustlist.getTrustServiceProviderList().getTrustServiceProviders());
        verifyServiceProvidersHasContent(trustlist.getTrustServiceProviderList().getTrustServiceProviders());
    }

    private static void verifyServiceProvidersHasContent(List<TrustServiceProvider> trustServiceProviders) {
        assertFalse(trustServiceProviders.isEmpty());
        assertEquals(2, trustServiceProviders.size(), "There should be exactly two TrustServiceProvider in the list");
        for (TrustServiceProvider serviceProvider : trustServiceProviders) {
            assertNotNull(serviceProvider);
            TSPInformation tspInformation = serviceProvider.getTSPInformation();
            assertNotNull(tspInformation);
            assertNotNull(tspInformation.getTSPName());
            List<MultiLangNormStringType> tspNames = tspInformation.getTSPName().getNames();
            assertFalse(tspNames.isEmpty());
            assertNotNull(tspInformation.getTSPTradeName());
            assertFalse(tspInformation.getTSPTradeName().getNames().isEmpty());
            assertNotNull(tspInformation.getTSPInformationURI());
            assertFalse(tspInformation.getTSPInformationURI().getURIS().isEmpty());
            verifyHasTSPAddress(tspInformation.getTSPAddress());
            List<TSPService> tspServices = serviceProvider.getTSPServices().getTSPServices();
            verifyServicesHasContent(tspServices);
        }
    }

    private static void verifyHasTSPAddress(AddressType tspAddress) {
        assertNotNull(tspAddress);

        assertNotNull(tspAddress.getElectronicAddress());
        List<NonEmptyMultiLangURIType> contactUris = tspAddress.getElectronicAddress().getURIS();
        assertNotNull(contactUris);
        assertTrue(contactUris.size() >= 4, "There should be at least 4 URI in the electronic address ( Norwegian and English versions of email and web)");
        assertTrue(contactUris.stream().filter(uri -> "no".equals(uri.getLang())).toList().size() >= 2);
        assertTrue(contactUris.stream().filter(uri -> "en".equals(uri.getLang())).toList().size() >= 2);

        assertNotNull(tspAddress.getPostalAddresses());
        List<PostalAddress> postalAddresses = tspAddress.getPostalAddresses().getPostalAddresses();
        assertNotNull(postalAddresses);
        assertFalse(postalAddresses.isEmpty(), "There should be at least 1 postal address");
        assertNotNull(postalAddresses.getFirst());
        assertNotNull(postalAddresses.getFirst().getStreetAddress());
        assertNotNull(postalAddresses.getFirst().getPostalCode());
        assertNotNull(postalAddresses.getFirst().getLocality());
        assertNotNull(postalAddresses.getFirst().getCountryName());
    }

    private static void verifyServicesHasContent(List<TSPService> tspServices) {
        assertNotNull(tspServices);
        assertFalse(tspServices.isEmpty(), "TSPServices should not be empty");
        for (TSPService service : tspServices) {
            assertNotNull(service);
            assertNotNull(service.getServiceInformation());
            assertNotNull(service.getServiceInformation().getServiceName());
            assertFalse(service.getServiceInformation().getServiceName().getNames().isEmpty());
            assertNotNull(service.getServiceInformation().getServiceStatus());
            assertNotNull(service.getServiceInformation().getServiceTypeIdentifier());
            assertNotNull(service.getServiceInformation().getServiceDigitalIdentity());
            assertFalse(service.getServiceInformation().getServiceDigitalIdentity().getDigitalIds().isEmpty());
        }
    }
}