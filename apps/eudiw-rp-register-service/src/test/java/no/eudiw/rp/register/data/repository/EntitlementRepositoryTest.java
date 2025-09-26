package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.Entitlement;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("When using EntitlementRepository")
class EntitlementRepositoryTest {

    @Autowired
    private EntitlementRepository repository;

    @Test
    @DisplayName("findAllByActive(true) returns only active entitlements")
    void findAllByActive_true() {
        List<Entitlement> active = repository.findAllByActive(true);

        assertThat(active)
            .allMatch(Entitlement::isActive);
    }

    @Test
    @DisplayName("findAllByActive(false) returns only inactive entitlements")
    void findAllByActive_false() {
        String inactiveEntitlement = UUID.randomUUID().toString();
        repository.save(new Entitlement(inactiveEntitlement, false, inactiveEntitlement));
        List<Entitlement> inactive = repository.findAllByActive(false);

        assertThat(inactive)
            .allMatch(entitlement -> !entitlement.isActive());
    }

    @Nested
    @DisplayName("existsByEntitlementAndActive")
    class ExistsByEntitlementAndActive {

        @Test
        @DisplayName("returns true for matching entitlement+active")
        void existingMatching() {
            String inactiveEntitlement = UUID.randomUUID().toString();
            repository.save(new Entitlement(inactiveEntitlement, false, inactiveEntitlement));
            String activeEntitlement = UUID.randomUUID().toString();
            repository.save(new Entitlement(activeEntitlement, true, activeEntitlement));

            assertThat(repository.existsByEntitlementAndActive(activeEntitlement, true)).isTrue();
            assertThat(repository.existsByEntitlementAndActive(inactiveEntitlement, false)).isTrue();
        }

        @Test
        @DisplayName("returns false when active flag does not match")
        void mismatchedActive() {
            String inactiveEntitlement = UUID.randomUUID().toString();
            repository.save(new Entitlement(inactiveEntitlement, false, inactiveEntitlement));
            String activeEntitlement = UUID.randomUUID().toString();
            repository.save(new Entitlement(activeEntitlement, true, activeEntitlement));

            assertThat(repository.existsByEntitlementAndActive(activeEntitlement, false)).isFalse();
            assertThat(repository.existsByEntitlementAndActive(inactiveEntitlement, true)).isFalse();
        }

        @Test
        @DisplayName("returns false for non-existent entitlement")
        void nonExistingEntitlement() {
            assertThat(repository.existsByEntitlementAndActive(UUID.randomUUID().toString(), true)).isFalse();
            assertThat(repository.existsByEntitlementAndActive(UUID.randomUUID().toString(), false)).isFalse();
        }
    }

    @Test
    @DisplayName("existsByEntitlement returns true only when entitlement exists")
    void existsByEntitlement_onlyOnExistence() {
        String inactiveEntitlement = UUID.randomUUID().toString();
        repository.save(new Entitlement(inactiveEntitlement, false, inactiveEntitlement));
        String activeEntitlement = UUID.randomUUID().toString();
        repository.save(new Entitlement(activeEntitlement, true, activeEntitlement));
        String randomEntitlement = UUID.randomUUID().toString();
        repository.save(new Entitlement(randomEntitlement, false, randomEntitlement));

        assertThat(repository.existsByEntitlement(inactiveEntitlement)).isTrue();
        assertThat(repository.existsByEntitlement(activeEntitlement)).isTrue();
        assertThat(repository.existsByEntitlement(randomEntitlement)).isTrue();
        assertThat(repository.existsByEntitlement(UUID.randomUUID().toString())).isFalse();
    }

    @Nested
    @DisplayName("findByEntitlement")
    class FindByEntitlement {

        @Test
        @DisplayName("returns entity when found")
        void returnsEntity() {
            String activeEntitlement = UUID.randomUUID().toString();
            repository.save(new Entitlement(activeEntitlement, true, activeEntitlement));

            Entitlement e = repository.findByEntitlement(activeEntitlement);
            assertThat(e).isNotNull();
            assertThat(e.getEntitlement()).isEqualTo(activeEntitlement);
            assertThat(e.isActive()).isTrue();
            assertThat(e.getId()).isNotNull();
        }

        @Test
        @DisplayName("returns null when not found")
        void returnsNullWhenMissing() {
            Entitlement e = repository.findByEntitlement(UUID.randomUUID().toString());
            assertThat(e).isNull();
        }
    }
}
