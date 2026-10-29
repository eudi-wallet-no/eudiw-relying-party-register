package no.eudiw.rp.register.api.v2.wrp;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
import no.eudiw.rp.register.config.WrpApiProperties;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WrpSearchCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrpLookupService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int CURSOR_FETCH_SIZE = 128;

    private final WalletRelyingPartyRepository repository;
    private final SupervisoryAuthorityProperties supervisoryAuthorityProperties;
    private final WrpApiProperties wrpApiProperties;

    @Transactional(readOnly = true)
    public Optional<WalletRelyingPartyResource> findByIdentifier(String identifier) {
        List<WalletRelyingParty> matches = repository.findAllByRegisteredIdentifier(identifier);
        if (matches.size() > 1) {
            throw new IllegalStateException("Registered WRP identifier matches more than one party");
        }
        return matches.stream().findFirst()
            .map(party -> toResource(party, activeEntitlements(List.of(party.getId()))));
    }

    @Transactional(readOnly = true)
    public CollectionPage findAll(
        int limit,
        String cursorToken,
        WrpCollectionFilters filters
    ) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new BadRequestException("limit must be between 1 and " + MAX_PAGE_SIZE);
        }

        Cursor cursor = decodeCursor(cursorToken);
        List<WalletRelyingParty> candidates = repository.findActivePage(
            new WrpSearchCriteria(
                filters.identifier(),
                filters.legalName(),
                filters.serviceIdentifier(),
                filters.tradeName(),
                filters.policy(),
                filters.entitlement()
            ),
            cursor == null ? null : cursor.createdMs(),
            cursor == null ? null : cursor.id(),
            limit + 1
        );

        boolean hasNextPage = candidates.size() > limit;
        List<WalletRelyingParty> parties = hasNextPage
            ? candidates.subList(0, limit)
            : candidates;
        Map<UUID, List<String>> entitlementsByService = activeEntitlements(
            parties.stream().map(WalletRelyingParty::getId).toList()
        );
        List<WalletRelyingPartyResource> data = parties.stream()
            .map(party -> toResource(party, entitlementsByService))
            .toList();
        String nextCursor = hasNextPage
            ? encodeCursor(parties.getLast().getCreatedMs(), parties.getLast().getId())
            : null;
        return new CollectionPage(data, nextCursor, hasNextPage);
    }

    private WalletRelyingPartyResource toResource(
        WalletRelyingParty party,
        Map<UUID, List<String>> entitlementsByService
    ) {
        return WalletRelyingPartyResource.from(
            party, entitlementsByService, supervisoryAuthorityProperties, wrpApiProperties);
    }

    private Map<UUID, List<String>> activeEntitlements(List<UUID> partyIds) {
        if (partyIds.isEmpty()) {
            return Map.of();
        }
        return repository.findActiveInstanceEntitlementsByPartyIds(partyIds)
            .stream()
            .collect(Collectors.groupingBy(
                WalletRelyingPartyRepository.ServiceEntitlementProjection::getServiceId,
                Collectors.mapping(
                    WalletRelyingPartyRepository.ServiceEntitlementProjection::getEntitlementValue,
                    Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                    )
                )
            ));
    }

    private static String encodeCursor(long createdMs, UUID id) {
        String value = "v1:" + createdMs + ":" + id;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decodeCursor(String token) {
        if (token == null) {
            return null;
        }
        try {
            if (token.isBlank() || token.length() > CURSOR_FETCH_SIZE) {
                throw new IllegalArgumentException();
            }
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new IllegalArgumentException();
            }
            long createdMs = Long.parseLong(parts[1]);
            UUID id = UUID.fromString(parts[2]);
            if (createdMs < 0 || !id.toString().equals(parts[2])
                || !encodeCursor(createdMs, id).equals(token)) {
                throw new IllegalArgumentException();
            }
            return new Cursor(createdMs, id);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid WRP cursor");
        }
    }

    public record CollectionPage(
        List<WalletRelyingPartyResource> data,
        String nextCursor,
        boolean hasNextPage
    ) {
        public CollectionPage {
            data = List.copyOf(data);
        }
    }

    private record Cursor(long createdMs, UUID id) {
    }
}
