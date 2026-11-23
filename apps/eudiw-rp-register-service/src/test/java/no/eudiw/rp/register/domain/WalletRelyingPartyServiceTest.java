package no.eudiw.rp.register.domain;

import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WalletRelyingPartyService intermediary configuration")
class WalletRelyingPartyServiceTest {

    private static final ServiceReference CLIENT_REFERENCE = new ServiceReference(
        List.of(new Identifier("urn:example:identifier", "client-party")),
        "client-service", "https://registry.example/wrp");
    private static final ServiceReference INTERMEDIARY_REFERENCE = new ServiceReference(
        List.of(new Identifier("urn:example:identifier", "party-1")),
        "proxy", "https://registry.example/wrp");

    @Test
    void startsActiveWithCreationDate() {
        var service = new WalletRelyingPartyService("Service", null, List.of());

        assertEquals(WalletRelyingPartyService.STATUS_ACTIVE, service.getStatus());
        assertTrue(service.isActive());
        assertEquals(LocalDate.now(ZoneOffset.UTC), service.getCreatedAt());
        assertNull(service.getSuspendedAt());
        assertNull(service.getRevokedAt());
    }

    @Test
    void suspendAndReactivateUpdateStatusAndDatesOnlyOnTransitions() {
        var service = new WalletRelyingPartyService("Service", null, List.of());

        service.suspend();
        LocalDate suspendedAt = service.getSuspendedAt();
        assertEquals(WalletRelyingPartyService.STATUS_SUSPENDED, service.getStatus());
        assertFalse(service.isActive());
        assertEquals(LocalDate.now(ZoneOffset.UTC), suspendedAt);

        service.suspend();
        assertEquals(suspendedAt, service.getSuspendedAt());

        service.reactivate();
        assertEquals(WalletRelyingPartyService.STATUS_ACTIVE, service.getStatus());
        assertTrue(service.isActive());
        assertNull(service.getSuspendedAt());

        service.reactivate();
        assertEquals(WalletRelyingPartyService.STATUS_ACTIVE, service.getStatus());
        assertTrue(service.isActive());
        assertNull(service.getSuspendedAt());
    }

    @Test
    void revokeIsTerminalAndPreservesSuspensionDate() {
        var service = new WalletRelyingPartyService("Service", null, List.of());
        service.suspend();
        LocalDate suspendedAt = service.getSuspendedAt();

        service.revoke();
        LocalDate revokedAt = service.getRevokedAt();

        assertEquals(WalletRelyingPartyService.STATUS_REVOKED, service.getStatus());
        assertFalse(service.isActive());
        assertEquals(suspendedAt, service.getSuspendedAt());
        assertEquals(LocalDate.now(ZoneOffset.UTC), revokedAt);

        service.revoke();
        assertEquals(revokedAt, service.getRevokedAt());
        assertThrows(BadRequestException.class, service::suspend);
        assertThrows(BadRequestException.class, service::reactivate);
    }

    @Test
    void legacyActiveSetterKeepsTs5StatusInSync() {
        var service = new WalletRelyingPartyService("Service", null, List.of());

        service.setActive(false);
        assertEquals(WalletRelyingPartyService.STATUS_SUSPENDED, service.getStatus());
        assertFalse(service.isActive());

        service.setActive(true);
        assertEquals(WalletRelyingPartyService.STATUS_ACTIVE, service.getStatus());
        assertTrue(service.isActive());
        assertNull(service.getSuspendedAt());
    }

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
            List.of(INTERMEDIARY_REFERENCE),
            List.of()
        );

        assertFalse(isIntermediary(service));
        assertEquals("proxy", service.getUsesIntermediaries().getFirst().serviceIdentifier());
    }

    @Test
    void allowsServedServicesOnlyForIntermediaryServices() {
        var service = new WalletRelyingPartyService(
            "Proxy", null, List.of(), true, List.of(), List.of(CLIENT_REFERENCE));

        assertTrue(isIntermediary(service));
        assertEquals(List.of(CLIENT_REFERENCE), service.getServedWRPServices());
    }

    @Test
    void rejectsIntermediaryThatUsesAnotherIntermediaryAtConstruction() {
        var intermediaryReference = INTERMEDIARY_REFERENCE;

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
            "Invalid", null, List.of(), false, List.of(), List.of(CLIENT_REFERENCE)));
    }

    @Test
    void rejectedReconfigurationLeavesPreviousStateUnchanged() {
        var service = new WalletRelyingPartyService(
            "Proxy", null, List.of(), true, List.of(), List.of(CLIENT_REFERENCE));

        assertThrows(IllegalArgumentException.class, () ->
            service.setIntermediaryConfiguration(false, List.of(), List.of(CLIENT_REFERENCE)));

        assertTrue(isIntermediary(service));
        assertEquals(List.of(CLIENT_REFERENCE), service.getServedWRPServices());
        assertTrue(service.getUsesIntermediaries().isEmpty());
    }

    @Test
    void copiesIncomingListsAndDoesNotExposeMutableState() {
        var servedServices = new ArrayList<>(List.of(CLIENT_REFERENCE));
        var service = new WalletRelyingPartyService(
            "Proxy", null, List.of(), true, List.of(), servedServices);
        servedServices.clear();

        assertEquals(List.of(CLIENT_REFERENCE), service.getServedWRPServices());
        assertThrows(UnsupportedOperationException.class,
            () -> service.getServedWRPServices().add(new ServiceReference(
                List.of(new Identifier("urn:example:identifier", "other-party")),
                "another-service", "https://registry.example/wrp")));
        assertThrows(UnsupportedOperationException.class,
            () -> service.getUsesIntermediaries().add(
                INTERMEDIARY_REFERENCE));
    }

    @Test
    @DisplayName("Should keep party identifiers the same in a service reference")
    void shouldPreserveServiceReferenceIdentifiersAndPreventChangesThroughTheReference() {
        var partyIdentifier = new Identifier("urn:example:identifier", "client-party");
        var identifiers = new ArrayList<>(List.of(partyIdentifier));
        var reference = new ServiceReference(identifiers, "client-service", "https://registry.example/wrp");
        identifiers.clear();

        assertEquals(List.of(partyIdentifier), reference.identifier());
        assertThrows(UnsupportedOperationException.class, () -> reference.identifier().clear());
    }

    private boolean isIntermediary(WalletRelyingPartyService service) {
        return Boolean.TRUE.equals(ReflectionTestUtils.getField(service, "isIntermediary"));
    }
}
