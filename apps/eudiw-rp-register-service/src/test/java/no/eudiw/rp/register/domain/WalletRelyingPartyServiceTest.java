package no.eudiw.rp.register.domain;

import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WalletRelyingPartyService intermediary configuration")
class WalletRelyingPartyServiceTest {

    @Test
    void legacyConstructorDefaultsToNonIntermediaryWithEmptyLists() {
        var service = new WalletRelyingPartyService("Service", null, List.of());

        assertFalse(isIntermediary(service));
        assertTrue(service.getUsesIntermediaries().isEmpty());
        assertTrue(service.getServedWRPServices().isEmpty());
    }

    @Test
    void allowsBothListsToBeEmptyForEitherIntermediaryFlag() {
        var regularService = new WalletRelyingPartyService(
            "Regular", null, List.of(), false, List.of(), List.of());
        var intermediaryService = new WalletRelyingPartyService(
            "Intermediary", null, List.of(), true, List.of(), List.of());

        assertFalse(isIntermediary(regularService));
        assertTrue(isIntermediary(intermediaryService));
    }

    @Test
    void allowsUsesIntermediariesOnlyForNonIntermediaryServices() {
        var service = new WalletRelyingPartyService(
            "Client",
            null,
            List.of(),
            false,
            List.of(new IntermediaryReference("proxy", "Proxy", "https://registry.example/wrp")),
            List.of()
        );

        assertFalse(isIntermediary(service));
        assertEquals("proxy", service.getUsesIntermediaries().getFirst().serviceIdentifier());
    }

    @Test
    void allowsServedServicesOnlyForIntermediaryServices() {
        var service = new WalletRelyingPartyService(
            "Proxy", null, List.of(), true, List.of(), List.of("client-service"));

        assertTrue(isIntermediary(service));
        assertEquals(List.of("client-service"), service.getServedWRPServices());
    }

    @Test
    void rejectsIntermediaryThatUsesAnotherIntermediaryAtConstruction() {
        var intermediaryReference = new IntermediaryReference("proxy", "Proxy", "https://registry.example/wrp");

        assertThrows(IllegalArgumentException.class, () -> new WalletRelyingPartyService(
            "Invalid",
            null,
            List.of(),
            true,
            List.of(intermediaryReference),
            List.of()
        ));
    }

    @Test
    void rejectsServedServicesForNonIntermediaryAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new WalletRelyingPartyService(
            "Invalid", null, List.of(), false, List.of(), List.of("client-service")));
    }

    @Test
    void rejectedReconfigurationLeavesPreviousStateUnchanged() {
        var service = new WalletRelyingPartyService(
            "Proxy", null, List.of(), true, List.of(), List.of("client-service"));

        assertThrows(IllegalArgumentException.class, () ->
            service.setIntermediaryConfiguration(false, List.of(), List.of("client-service")));

        assertTrue(isIntermediary(service));
        assertEquals(List.of("client-service"), service.getServedWRPServices());
        assertTrue(service.getUsesIntermediaries().isEmpty());
    }

    @Test
    void copiesIncomingListsAndDoesNotExposeMutableState() {
        var servedServices = new ArrayList<>(List.of("client-service"));
        var service = new WalletRelyingPartyService(
            "Proxy", null, List.of(), true, List.of(), servedServices);
        servedServices.clear();

        assertEquals(List.of("client-service"), service.getServedWRPServices());
        assertThrows(UnsupportedOperationException.class,
            () -> service.getServedWRPServices().add("another-service"));
        assertThrows(UnsupportedOperationException.class,
            () -> service.getUsesIntermediaries().add(
                new IntermediaryReference("proxy", "Proxy", "https://registry.example/wrp")));
    }

    private boolean isIntermediary(WalletRelyingPartyService service) {
        return Boolean.TRUE.equals(ReflectionTestUtils.getField(service, "isIntermediary"));
    }
}
