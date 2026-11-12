package no.idporten.eudiw.rp.admin.service;

import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient.Registration;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient.Role;
import no.idporten.eudiw.rp.admin.web.resource.RegistrationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class V2RegistrationsServiceTests {

    private static final String ORGNO = "123456785";
    @Mock
    private V2RegisterClient client;
    @InjectMocks
    private V2RegistrationsService service;

    @Test
    @DisplayName("Prepare registrations for display as received, including duplicates and missing fields")
    void shouldKeepRegistrationsAsReceivedForDisplay() {
        Registration partial = new Registration(new UUID(0, 2), "Kontrollteneste",
            Arrays.asList(new Role("Utferdar"), null), 1700000000000L, null);
        Registration missing = new Registration(null, null, null, null, null);
        when(client.getRegistrations(ORGNO)).thenReturn(List.of(partial, missing, partial));

        assertEquals(List.of(
            new RegistrationSummary(partial.id(), "Kontrollteneste",
                List.of(new RegistrationSummary.Role("Utferdar"), new RegistrationSummary.Role(null)), 1700000000000L, null),
            new RegistrationSummary(missing.id(), null, null, null, null),
            new RegistrationSummary(partial.id(), "Kontrollteneste",
                List.of(new RegistrationSummary.Role("Utferdar"), new RegistrationSummary.Role(null)), 1700000000000L, null)
        ), service.getAllByOrgno(ORGNO));
    }

    @Test
    @DisplayName("Should search service names without losing matching duplicates or their order")
    void shouldSearchServiceNamesWithoutLosingMatchingDuplicatesOrTheirOrder() {
        Registration matching = new Registration(new UUID(0, 1), "KontrollTENESTE", List.of(), null, null);
        Registration other = new Registration(new UUID(0, 2), "Annan teneste", List.of(), null, null);
        Registration missing = new Registration(new UUID(0, 3), null, null, null, null);
        when(client.getRegistrations(ORGNO)).thenReturn(List.of(matching, other, missing, matching));

        assertEquals(List.of(RegistrationSummary.from(matching), RegistrationSummary.from(matching)),
            service.searchByOrgno(ORGNO, "  kontroll  "));
        assertTrue(service.searchByOrgno(ORGNO, "Ukjend").isEmpty());
        assertEquals(List.of(RegistrationSummary.from(matching), RegistrationSummary.from(other),
            RegistrationSummary.from(missing), RegistrationSummary.from(matching)), service.searchByOrgno(ORGNO, " "));
    }

    @Test
    @DisplayName("Require an organization before retrieving its registrations")
    void shouldRequireOrganizationToRetrieveRegistrations() {
        assertThrows(IllegalArgumentException.class, () -> service.getAllByOrgno(" "));
        verifyNoInteractions(client);
    }
}
