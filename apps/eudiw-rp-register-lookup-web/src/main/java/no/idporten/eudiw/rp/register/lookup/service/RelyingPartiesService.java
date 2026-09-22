package no.idporten.eudiw.rp.register.lookup.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.register.lookup.exception.LookupServiceException;
import no.idporten.eudiw.rp.register.lookup.service.exception.NotFoundException;
import no.idporten.eudiw.rp.register.lookup.service.exception.RelyingPartyNotFoundException;
import no.idporten.eudiw.rp.register.lookup.web.resource.PagedResponse;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.entitlement.EntitlementResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.entitlement.EntitlementsResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public class RelyingPartiesService {

    private final RestClient restClient;

    public PagedResponse<RelyingPartyResource> search(SearchRelyingPartyResource searchResource) {
        return restClient.post()
                .uri("/rp/search")
                .body(searchResource)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<PagedResponse<RelyingPartyResource>>() {})
                .getBody();
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
                    .uri("/rp/{id}", id)
                    .retrieve()
                    .toEntity(RelyingPartyResource.class)
                    .getBody();
        } catch (NotFoundException e) {
            throw new RelyingPartyNotFoundException(e.getErrorDescription(), id.toString());
        }
    }

    public List<EntitlementResource> getValidEntitlements() {
        return
            Objects.requireNonNull( // should never fire since the ResponseErrorHandler would fire first.
                       restClient.get()
                                 .uri("/entitlement")
                                 .retrieve()
                                 .toEntity(EntitlementsResource.class)
                                 .getBody())
                   .entitlements()
                   .stream()
                   .filter(EntitlementResource::active)
                   .toList();
    }

    @ExceptionHandler(Exception.class)
    public void wrapUnexpectedRestClientException(Exception e) {
        throw new LookupServiceException("Register service REST client exception", e);
    }
}
