package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.CreateEntitlementResource;
import no.eudiw.rp.register.api.resource.EntitlementResource;
import no.eudiw.rp.register.api.resource.EntitlementsResource;
import no.eudiw.rp.register.data.entity.Entitlement;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.data.service.exception.AlreadyExistsException;
import no.eudiw.rp.register.data.service.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    private final EntitlementRepository repository = mock(EntitlementRepository.class);
    private final EntitlementService service = new EntitlementService(repository);

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("throws BadRequestException when input is null or empty")
        void invalidInput() {
            assertThatThrownBy(() -> service.register(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid entitlement");

            assertThatThrownBy(() -> service.register(new CreateEntitlementResource("")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid entitlement");

            assertThatThrownBy(() -> service.register(new CreateEntitlementResource(null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid entitlement");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("throws AlreadyExistsException when entitlement already exists")
        void alreadyExists() {
            when(repository.existsByEntitlement("entitlement1")).thenReturn(true);

            assertThatThrownBy(() -> service.register(new CreateEntitlementResource("entitlement1")))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("already exists");

            verify(repository).existsByEntitlement("entitlement1");
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("persists new entitlement and returns converted resource")
        void success() {
            when(repository.existsByEntitlement("entitlement1")).thenReturn(false);

            Entitlement saved = new Entitlement("entitlement1", true);
            when(repository.saveAndFlush(any(Entitlement.class))).thenReturn(saved);

            UUID id = UUID.randomUUID();
            EntitlementResource expectedResource = new EntitlementResource(id, "entitlement1", true);

            try (MockedStatic<Converter> mocked = mockStatic(Converter.class)) {
                mocked.when(() -> Converter.toResource(saved)).thenReturn(expectedResource);

                EntitlementResource returned = service.register(new CreateEntitlementResource("entitlement1"));

                ArgumentCaptor<Entitlement> captor = ArgumentCaptor.forClass(Entitlement.class);
                verify(repository).saveAndFlush(captor.capture());
                Entitlement toSave = captor.getValue();
                assertThat(toSave.getEntitlement()).isEqualTo("entitlement1");
                assertThat(toSave.isActive()).isTrue();

                mocked.verify(() -> Converter.toResource(saved));
                assertThat(returned.id()).isEqualTo(id);
                assertThat(returned.entitlement()).isEqualTo("entitlement1");
                assertThat(returned.active()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("findAllEntitlements")
    class FindAll {

        @Test
        @DisplayName("returns converted list of active entitlements")
        void returnsActiveConverted() {
            List<Entitlement> active = List.of(
                new Entitlement("entitlement1", true),
                new Entitlement("entitlement2", true)
            );
            when(repository.findAllByActive(true)).thenReturn(active);

            EntitlementsResource expected = new EntitlementsResource(
                List.of(
                    new EntitlementResource(UUID.randomUUID(), "entitlement1", true),
                    new EntitlementResource(UUID.randomUUID(), "entitlement2", true)
                )
            );

            try (MockedStatic<Converter> mocked = mockStatic(Converter.class)) {
                mocked.when(() -> Converter.toEntitlementsResource(active)).thenReturn(expected);

                EntitlementsResource result = service.findAllActive();

                verify(repository).findAllByActive(true);
                mocked.verify(() -> Converter.toEntitlementsResource(active));
                assertThat(result.entitlements()).hasSize(2);
                assertThat(result.entitlements())
                    .extracting(EntitlementResource::entitlement)
                    .containsExactlyInAnyOrder("entitlement1", "entitlement2");
            }
        }
    }

    @Nested
    @DisplayName("editEntitlement")
    class EditEntitlement {

        @Test
        @DisplayName("throws BadRequestException for null or empty entitlement")
        void invalidInput() {
            assertThatThrownBy(() -> service.editEntitlement(null, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("should not be null");

            assertThatThrownBy(() -> service.editEntitlement("", true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("should not be null");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("throws BadRequestException when entitlement is not found")
        void notFound() {
            when(repository.findByEntitlement("MISSING")).thenReturn(null);

            assertThatThrownBy(() -> service.editEntitlement("MISSING", false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not found");

            verify(repository).findByEntitlement("MISSING");
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("updates active flag and returns converted resource")
        void success() {
            Entitlement existing = new Entitlement("entitlement1", true);
            when(repository.findByEntitlement("entitlement1")).thenReturn(existing);

            Entitlement afterSave = new Entitlement("entitlement1", false);
            when(repository.saveAndFlush(existing)).thenReturn(afterSave);

            EntitlementResource expected = new EntitlementResource(UUID.randomUUID(), "entitlement1", false);

            try (MockedStatic<Converter> mocked = mockStatic(Converter.class)) {
                mocked.when(() -> Converter.toResource(afterSave)).thenReturn(expected);

                EntitlementResource result = service.editEntitlement("entitlement1", false);

                assertThat(existing.isActive()).isFalse();
                verify(repository).saveAndFlush(existing);

                mocked.verify(() -> Converter.toResource(afterSave));
                assertThat(result.entitlement()).isEqualTo("entitlement1");
                assertThat(result.active()).isFalse();
            }
        }
    }
}
