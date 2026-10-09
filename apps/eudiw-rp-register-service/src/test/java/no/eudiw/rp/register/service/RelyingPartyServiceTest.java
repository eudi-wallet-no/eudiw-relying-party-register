package no.eudiw.rp.register.service;

import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.exception.NotFoundException;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RelyingPartyServiceTest {

    private final EntitlementRepository entitlementRepository = mock(EntitlementRepository.class);
    private final RelyingPartyInstanceRepository repository = mock(RelyingPartyInstanceRepository.class);
    private final WalletRelyingPartyLookupService walletRelyingPartyLookupService = mock(WalletRelyingPartyLookupService.class);
    private final WalletRelyingPartyServiceRepository serviceRepository = mock(WalletRelyingPartyServiceRepository.class);
    private final RelyingPartyService service =
        new RelyingPartyService(entitlementRepository, repository, walletRelyingPartyLookupService, serviceRepository);

    @Test
    void createsRelyingPartyForActiveWalletRelyingPartyWithActiveEntitlement() {
        WalletRelyingParty walletRelyingParty = new WalletRelyingParty("Entity", "123456789", false, List.of());
        RelyingPartyEntitlement entitlement = new RelyingPartyEntitlement("active-entitlement");
        RelyingPartyEaa eaa = new RelyingPartyEaa("namespace", "intent");
        when(entitlementRepository.existsByEntitlementAndActive("active-entitlement", true)).thenReturn(true);
        when(walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno("123456789")).thenReturn(walletRelyingParty);
        when(repository.saveAndFlush(any(RelyingPartyInstance.class))).thenAnswer(call -> call.getArgument(0));

        RelyingPartyInstance result =
            service.createRelyingParty("123456789", "Trade name", List.of(entitlement), List.of(eaa));

        ArgumentCaptor<RelyingPartyInstance> saved = ArgumentCaptor.forClass(RelyingPartyInstance.class);
        verify(repository).saveAndFlush(saved.capture());
        assertSame(saved.getValue(), result);
        assertSame(walletRelyingParty, result.getWalletRelyingPartyService().getWalletRelyingParty());
        assertEquals("Trade name", result.getWalletRelyingPartyService().getServiceTradeName());
        assertSame(result, entitlement.getRelyingPartyInstance());
        assertSame(result, eaa.getRelyingPartyInstance());
        verify(serviceRepository).save(result.getWalletRelyingPartyService());
        assertEquals(List.of(result.getWalletRelyingPartyService()), walletRelyingParty.getServices());
    }

    @ParameterizedTest
    @MethodSource("missingCollections")
    void rejectsMissingCollectionsBeforeSaving(
        List<RelyingPartyEntitlement> entitlements, List<RelyingPartyEaa> eaas
    ) {
        assertThrows(BadRequestException.class,
            () -> service.createRelyingParty("123456789", "Name", entitlements, eaas));
        assertThrows(BadRequestException.class,
            () -> service.updateRelyingParty(UUID.randomUUID(), "Name", true, entitlements, eaas));

        verifyNoInteractions(entitlementRepository, repository, walletRelyingPartyLookupService, serviceRepository);
    }

    private static Stream<Arguments> missingCollections() {
        return Stream.of(
            arguments(null, List.of()),
            arguments(List.of(), null),
            arguments(null, null)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsUnrecognizedOrInactiveEntitlementBeforeSaving(boolean update) {
        List<RelyingPartyEntitlement> entitlements =
            List.of(new RelyingPartyEntitlement("invalid-entitlement"));

        if (update) {
            assertThrows(BadRequestException.class,
                () -> service.updateRelyingParty(UUID.randomUUID(), "Name", true, entitlements, List.of()));
        } else {
            assertThrows(BadRequestException.class,
                () -> service.createRelyingParty("123456789", "Name", entitlements, List.of()));
        }

        verify(entitlementRepository).existsByEntitlementAndActive("invalid-entitlement", true);
        verifyNoInteractions(repository, walletRelyingPartyLookupService, serviceRepository);
    }

    @Test
    void rejectsInactiveWalletRelyingPartyBeforeSaving() {
        WalletRelyingParty walletRelyingParty = new WalletRelyingParty("Entity", "123456789", false, List.of());
        walletRelyingParty.setActive(false);
        when(walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno("123456789")).thenReturn(walletRelyingParty);

        assertThrows(BadRequestException.class,
            () -> service.createRelyingParty("123456789", "Name", List.of(), List.of()));

        verifyNoInteractions(repository, serviceRepository);
    }

    @Test
    void updatesExistingRelyingPartyWithoutReplacingWalletRelyingParty() {
        UUID id = UUID.randomUUID();
        WalletRelyingParty walletRelyingParty = new WalletRelyingParty("Entity", "123456789", false, List.of());
        RelyingPartyInstance instance = EntityGenerator.generateRelyingPartyInstance("Old name", List.of(), List.of(), List.of());
        instance.getWalletRelyingPartyService().setWalletRelyingParty(walletRelyingParty);
        when(repository.findById(id)).thenReturn(Optional.of(instance));
        when(repository.saveAndFlush(instance)).thenReturn(instance);

        RelyingPartyInstance result =
            service.updateRelyingParty(id, "New name", false, List.of(), List.of());

        assertSame(instance, result);
        assertSame(walletRelyingParty, result.getWalletRelyingPartyService().getWalletRelyingParty());
        assertEquals("New name", result.getWalletRelyingPartyService().getServiceTradeName());
        assertFalse(result.isActive());
        verify(repository).saveAndFlush(instance);
        verifyNoInteractions(walletRelyingPartyLookupService);
    }

    @Test
    void marksInstanceUpdatedWhenServiceIsRenamed() {
        UUID id = UUID.randomUUID();
        RelyingPartyInstance instance = EntityGenerator.generateRelyingPartyInstance("Old name", List.of(), List.of(), List.of());
        long lastUpdatedBefore = instance.getLastUpdatedMs();
        when(repository.findById(id)).thenReturn(Optional.of(instance));
        when(repository.saveAndFlush(instance)).thenReturn(instance);

        RelyingPartyInstance result = service.updateRelyingParty(id, "New name", true, List.of(), List.of());

        assertTrue(result.getLastUpdatedMs() > lastUpdatedBefore);
    }

    @Test
    void rejectsUpdateWithoutId() {
        assertThrows(BadRequestException.class,
            () -> service.updateRelyingParty(null, "Name", true, List.of(), List.of()));

        verifyNoInteractions(repository, entitlementRepository, walletRelyingPartyLookupService);
    }

    @Test
    void preservesNotFoundForReadAndBadRequestForUpdate() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findRelyingParty(id));
        assertThrows(BadRequestException.class,
            () -> service.updateRelyingParty(id, "Name", true, List.of(), List.of()));

        verify(repository, never()).saveAndFlush(any());
    }
}
