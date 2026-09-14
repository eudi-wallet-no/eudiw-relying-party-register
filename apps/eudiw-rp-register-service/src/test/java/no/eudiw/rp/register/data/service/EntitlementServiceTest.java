package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.entitlements.EntitlementsResource;
import no.eudiw.rp.register.data.entity.Entitlement;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.service.Converter;
import no.eudiw.rp.register.service.EntitlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("junit")
class EntitlementServiceTest {

    @MockitoBean
    @SuppressWarnings("unused")
    private EntitlementRepository repository;

    @MockitoBean
    @SuppressWarnings("unused")
    private Converter converter;

    @Autowired
    private EntitlementService service;

    @Nested
    @DisplayName("findAllEntitlements")
    class FindAll {

        @Test
        @DisplayName("with includeInactive=false retrieves active entitlements")
        void findsOnlyActiveEntitlements() {
            List<Entitlement> activeEntitlements = List.of(
                new Entitlement("entitlement1", true, "entitlement1", "access")
            );
            EntitlementsResource expected = new EntitlementsResource(List.of());
            when(repository.findAllByActive(true)).thenReturn(activeEntitlements);
            when(converter.toEntitlementsResource(activeEntitlements)).thenReturn(expected);

            EntitlementsResource result = service.findAllEntitlements(false);

            verify(repository).findAllByActive(true);
            verify(converter).toEntitlementsResource(activeEntitlements);
            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("with includeInactive=true retrieves all entitlements")
        void findsAllEntitlements() {
            List<Entitlement> entitlements = List.of(
                new Entitlement("entitlement1", true, "entitlement1", "access"),
                new Entitlement("inactive-entitlement", false, "inactive-entitlement", "access")
            );
            EntitlementsResource expected = new EntitlementsResource(List.of());
            when(repository.findAll()).thenReturn(entitlements);
            when(converter.toEntitlementsResource(entitlements)).thenReturn(expected);

            EntitlementsResource result = service.findAllEntitlements(true);

            verify(repository).findAll();
            verify(converter).toEntitlementsResource(entitlements);
            assertThat(result).isSameAs(expected);
        }
    }
}
