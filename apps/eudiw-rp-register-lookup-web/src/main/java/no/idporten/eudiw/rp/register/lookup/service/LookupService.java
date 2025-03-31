package no.idporten.eudiw.rp.register.lookup.service;

import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.service.config.LookupServiceProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LookupService {

    private final LookupServiceProperties lookupServiceProperties;
    private final RestClient restClient;

    public LookupService(LookupServiceProperties lookupServiceProperties,
                         RestClient restClient) {
        this.lookupServiceProperties = lookupServiceProperties;
        this.restClient = restClient;
    }

    public RelyingPartiesResource search(SearchRelyingPartyResource searchResource) {
        return restClient.post()
                     .uri(lookupServiceProperties.apiSearchEndpoint())
                     .body(searchResource)
                     .retrieve()
                     .toEntity(RelyingPartiesResource.class)
                     .getBody();
    }
}
