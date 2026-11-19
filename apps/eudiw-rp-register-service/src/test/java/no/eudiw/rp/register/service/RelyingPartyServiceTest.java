package no.eudiw.rp.register.service;

import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RelyingPartyServiceTest {

    private final EntitlementRepository entitlementRepository = mock(EntitlementRepository.class);
    private final RelyingPartyInstanceRepository repository = mock(RelyingPartyInstanceRepository.class);
    private final WalletRelyingPartyRepository walletRelyingPartyRepository = mock(WalletRelyingPartyRepository.class);
    private final WalletRelyingPartyLookupService walletRelyingPartyLookupService = mock(WalletRelyingPartyLookupService.class);
    private final WalletRelyingPartyServiceRepository walletRelyingPartyServiceRepository = mock(WalletRelyingPartyServiceRepository.class);
    private final RelyingPartyService service =
        new RelyingPartyService(
            entitlementRepository, repository, walletRelyingPartyRepository, walletRelyingPartyLookupService, walletRelyingPartyServiceRepository);

    @Test
    void createsRelyingPartyForActiveWalletRelyingPartyWithActiveEntitlement() {
        WalletRelyingParty walletRelyingParty = new WalletRelyingParty("Entity", "123456789", false, List.of());
        RelyingPartyEntitlement entitlement = new RelyingPartyEntitlement("active-entitlement");
        RelyingPartyEaa eaa = new RelyingPartyEaa("namespace", "intent");
        when(entitlementRepository.existsByEntitlementAndActive("active-entitlement", true)).thenReturn(true);
        when(walletRelyingPartyLookupService.findWalletRelyingPartyForOrgno("123456789"))
            .thenReturn(Optional.of(walletRelyingParty));
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
        verify(walletRelyingPartyServiceRepository).save(result.getWalletRelyingPartyService());
        assertEquals(List.of(result.getWalletRelyingPartyService()), walletRelyingParty.getServices());
        verify(walletRelyingPartyLookupService, never()).createWalletRelyingParty(any());
    }

    @ParameterizedTest
    @MethodSource("missingCollections")
    void rejectsMissingCollectionsBeforeSaving(
        List<RelyingPartyEntitlement> entitlements, List<RelyingPartyEaa> eaas
    ) {
        assertThrows(BadRequestException.class,
            () -> service.createRelyingParty("123456789", "Name", entitlements, eaas));

        verifyNoInteractions(entitlementRepository, repository, walletRelyingPartyLookupService, walletRelyingPartyServiceRepository);
    }

    private static Stream<Arguments> missingCollections() {
        return Stream.of(
            arguments(null, List.of()),
            arguments(List.of(), null),
            arguments(null, null)
        );
    }

    @Test
    void rejectsUnrecognizedOrInactiveEntitlementBeforeSaving() {
        List<RelyingPartyEntitlement> entitlements =
            List.of(new RelyingPartyEntitlement("invalid-entitlement"));

        assertThrows(BadRequestException.class,
            () -> service.createRelyingParty("123456789", "Name", entitlements, List.of()));

        verify(entitlementRepository).existsByEntitlementAndActive("invalid-entitlement", true);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(walletRelyingPartyLookupService, walletRelyingPartyServiceRepository);
    }

    @Test
    void rejectsInactiveWalletRelyingPartyBeforeSaving() {
        WalletRelyingParty walletRelyingParty = new WalletRelyingParty("Entity", "123456789", false, List.of());
        walletRelyingParty.setActive(false);
        when(walletRelyingPartyLookupService.findWalletRelyingPartyForOrgno("123456789"))
            .thenReturn(Optional.of(walletRelyingParty));

        assertThrows(BadRequestException.class,
            () -> service.createRelyingParty("123456789", "Name", List.of(), List.of()));

        verifyNoInteractions(repository, walletRelyingPartyServiceRepository);
    }

}
