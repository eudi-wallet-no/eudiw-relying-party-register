package no.idporten.eudiw.rp.register.lookup.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.service.config.LookupServiceProperties;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Service
public class LookupService {

    private final LookupServiceProperties lookupServiceProperties;
    private final RestClient restClient;

    public RelyingPartiesResource search(SearchRelyingPartyResource searchResource) {
        return restClient.post()
                     .uri(lookupServiceProperties.apiSearchEndpoint())
                     .body(searchResource)
                     .retrieve()
                     .toEntity(RelyingPartiesResource.class)
                     .getBody();
    }

    public RelyingPartiesResource getAll() {
        return restClient.get()
                         .retrieve()
                         .toEntity(RelyingPartiesResource.class)
                         .getBody();
    }
}
