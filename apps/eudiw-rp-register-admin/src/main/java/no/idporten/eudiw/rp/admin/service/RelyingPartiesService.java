package no.idporten.eudiw.rp.admin.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificatesResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartiesService {

    private final RestClient restClient;

    public RelyingPartyResource create(CreateRelyingPartyResource createResource) {
        return restClient.post()
                .body(createResource)
                .retrieve()
                .toEntity(RelyingPartyResource.class)
                .getBody();
    }

    public RelyingPartyResource edit(UUID id, EditRelyingPartyResource editResource) {
        return restClient.post()
                .uri("/" + id)
                .body(editResource)
                .retrieve()
                .toEntity(RelyingPartyResource.class)
                .getBody();
    }

    public RelyingPartiesResource search(SearchRelyingPartyResource searchResource) {
        return restClient.post()
                .uri("/search")
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

    public RelyingPartyResource get(UUID id) {
        return restClient.get()
                .uri("/" + id)
                .retrieve()
                .toEntity(RelyingPartyResource.class)
                .getBody();
    }

    public void delete(UUID id) {
        restClient.delete().uri("/" + id).retrieve().toBodilessEntity();
    }

    public RelyingPartyAccessCertificatesResource getCertificatesForRelyingParty(
        UUID id) {
        return restClient.get()
                   .uri("/%s/certs".formatted(id))
                   .retrieve()
                   .toEntity(RelyingPartyAccessCertificatesResource.class)
                   .getBody();
    }
}
