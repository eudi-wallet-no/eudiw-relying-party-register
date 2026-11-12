package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.api.v2.endpoints.entities.RegistrationSummaryResponse;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.repository.RegistrationSummaryRow;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.service.EntitlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationSummaryServiceTest {

    @Mock
    private RelyingPartyInstanceRepository repository;
    @Mock
    private EntitlementService entitlementService;
    @InjectMocks
    private V2ApiService service;

    @Test
    @DisplayName("List each registration with its available role names, including registrations without roles")
    void shouldListRegistrationsWithAvailableRoleNames() {
        UUID issuerInstance = new UUID(0, 2);
        UUID withoutRoles = new UUID(0, 1);
        when(repository.findRegistrationSummariesByOrgno("123456785")).thenReturn(List.of(
            new RegistrationSummaryRow(issuerInstance, "Kontrollteneste", 1700000000000L, 1710000000000L, new UUID(0, 41), "urn:issuer"),
            new RegistrationSummaryRow(issuerInstance, "Kontrollteneste", 1700000000000L, 1710000000000L, new UUID(0, 42), "urn:blank"),
            new RegistrationSummaryRow(issuerInstance, "Kontrollteneste", 1700000000000L, 1710000000000L, new UUID(0, 43), "urn:unknown"),
            new RegistrationSummaryRow(withoutRoles, "Anna teneste", 1700086400000L, 1710000000000L, null, null)));
        when(entitlementService.findAllEntitlements(true)).thenReturn(List.of(
            new Entitlement("urn:issuer", false, "Utferdar", "test-ca"),
            new Entitlement("urn:blank", true, " ", "test-ca")));

        assertEquals(List.of(
            new RegistrationSummaryResponse(issuerInstance, "Kontrollteneste",
                List.of(new RegistrationSummaryResponse.Role("Utferdar"), new RegistrationSummaryResponse.Role("urn:blank"),
                    new RegistrationSummaryResponse.Role("urn:unknown")), 1700000000000L, 1710000000000L),
            new RegistrationSummaryResponse(withoutRoles, "Anna teneste", List.of(), 1700086400000L, 1710000000000L)
        ), service.getRegistrationsByOrgno("123456785"));
    }

    @Test
    @DisplayName("Require an organization before retrieving its registrations")
    void shouldRequireOrganizationToRetrieveRegistrations() {
        assertThrows(BadRequestException.class, () -> service.getRegistrationsByOrgno(" "));
        verifyNoInteractions(repository, entitlementService);
    }
}
