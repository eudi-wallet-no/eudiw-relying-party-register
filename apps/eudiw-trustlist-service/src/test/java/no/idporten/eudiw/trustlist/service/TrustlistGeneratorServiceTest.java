package no.idporten.eudiw.trustlist.service;

import org.etsi.uri._02231.v2_.TSPService;
import org.etsi.uri._02231.v2_.TrustServiceProvider;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        verify1SericeProviderHasContent(trustlist.getTrustServiceProviderList().getTrustServiceProviders());
    }

    private static void verify1SericeProviderHasContent(List<TrustServiceProvider> trustServiceProviders) {
        assertFalse(trustServiceProviders.isEmpty());
        TrustServiceProvider serviceProvider = trustServiceProviders.getFirst();
        assertNotNull(serviceProvider);
        assertNotNull(serviceProvider.getTSPInformation());
        assertNotNull(serviceProvider.getTSPInformation().getTSPName());
        assertFalse(serviceProvider.getTSPInformation().getTSPName().getNames().isEmpty());
        assertNotNull(serviceProvider.getTSPInformation().getTSPTradeName());
        assertFalse(serviceProvider.getTSPInformation().getTSPTradeName().getNames().isEmpty());
        assertNotNull(serviceProvider.getTSPInformation().getTSPInformationURI());
        assertFalse(serviceProvider.getTSPInformation().getTSPInformationURI().getURIS().isEmpty());

        List<TSPService> tspServices = serviceProvider.getTSPServices().getTSPServices();
        verify1ServiceHasContent(tspServices);
    }

    private static void verify1ServiceHasContent(List<TSPService> tspServices) {
        assertNotNull(tspServices);
        assertFalse(tspServices.isEmpty(), "TSPServices should not be empty");
        TSPService service = tspServices.getFirst();
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