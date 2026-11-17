package no.idporten.eudiw.rp.register.lookup.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.register.lookup.exception.LookupServiceException;
import no.idporten.eudiw.rp.register.lookup.service.exception.NotFoundException;
import no.idporten.eudiw.rp.register.lookup.service.exception.RelyingPartyNotFoundException;
import no.idporten.eudiw.rp.register.lookup.web.resource.PagedResponse;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.entitlement.EntitlementResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.entitlement.EntitlementsResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RelyingPartiesService {

    private final RestClient restClient;

    public PagedResponse<RelyingPartyResource> search(SearchRelyingPartyResource searchResource) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/instances")
                        .queryParam("searchTerm", searchResource.searchTerm())
                        .queryParam("hideSyntheticOrgnos", searchResource.hideSyntheticOrgnos())
                        .queryParam("page", searchResource.page())
                        .queryParam("pageSize", searchResource.pageSize())
                        .queryParam("sort", searchResource.ordering());
                    searchResource.requiredEntitlements()
                        .forEach(entitlement -> builder.queryParam("requiredEntitlements", entitlement));
                    return builder.build();
                })
                .retrieve()
                .toEntity(new ParameterizedTypeReference<PagedResponse<RelyingPartyResource>>() {})
                .getBody();
    }

    public PagedResponse<RelyingPartyResource> searchWithEntitlementDisplayNames(
        SearchRelyingPartyResource searchResource
    ) {
        PagedResponse<RelyingPartyResource> page = search(searchResource);
        if (page.content().isEmpty()) {
            return page;
        }

        Map<String, String> displayNames = getEntitlements(true).stream()
            .collect(Collectors.toMap(
                EntitlementResource::entitlement,
                EntitlementResource::displayName,
                (first, _) -> first));
        List<RelyingPartyResource> content = page.content().stream()
            .map(relyingParty -> relyingParty.withRelyingPartyEntitlements(
                relyingParty.relyingPartyEntitlements().stream()
                    .map(entitlement -> new RelyingPartyEntitlementResource(
                        entitlement.entitlement(),
                        displayNames.getOrDefault(entitlement.entitlement(), entitlement.entitlement())))
                    .toList()))
            .toList();

        return new PagedResponse<>(content, page.page());
    }

    public long count() {
        PagedResponse<RelyingPartyResource> page =
            search(new SearchRelyingPartyResource()
                .withPage(0)
                .withPageSize(1));
        return page.page().totalElements();
    }

    public RelyingPartyResource get(UUID id) {
        try {
            return restClient.get()
                    .uri("/registrations/{id}", id)
                    .retrieve()
                    .toEntity(RelyingPartyResource.class)
                    .getBody();
        } catch (NotFoundException e) {
            throw new RelyingPartyNotFoundException(e.getErrorDescription(), id.toString());
        }
    }

    public List<EntitlementResource> getValidEntitlements() {
        return getEntitlements(false).stream()
            .filter(EntitlementResource::active)
            .toList();
    }

    private List<EntitlementResource> getEntitlements(boolean includeInactive) {
        return Objects.requireNonNull( // should never fire since the ResponseErrorHandler would fire first.
                restClient.get()
                    .uri("/entitlements?includeInactive={includeInactive}", includeInactive)
                    .retrieve()
                    .toEntity(EntitlementsResource.class)
                    .getBody())
            .entitlements();
    }

    @ExceptionHandler(Exception.class)
    public void wrapUnexpectedRestClientException(Exception e) {
        throw new LookupServiceException("Register service REST client exception", e);
    }
}
