package no.idporten.eudiw.rp.admin.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificatesResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyCsrResource;
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
        return restClient.put()
                .uri("/{id}", id)
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
                .uri("/{id}", id)
                .retrieve()
                .toEntity(RelyingPartyResource.class)
                .getBody();
    }

    public void delete(UUID id) {
        restClient.delete()
                  .uri("/{id}", id)
                  .retrieve()
                  .toBodilessEntity();
    }

    public RelyingPartyAccessCertificatesResource getCertificatesForRelyingParty(
        UUID id) {
        return restClient.get()
                .uri("/{id}/certs", id)
                .retrieve()
                .toEntity(RelyingPartyAccessCertificatesResource.class)
                .getBody();
    }

    public RelyingPartyAccessCertificateResource getCertificate(
        UUID relyingPartyId, UUID certificateId) {
        return restClient.get()
                         .uri("/{rp-id}/certs/{cert-id}", relyingPartyId, certificateId)
                         .retrieve()
                         .toEntity(RelyingPartyAccessCertificateResource.class)
                         .getBody();
    }

    public RelyingPartyAccessCertificateResource requestCertificateForRelyingParty(
        UUID id,
        RelyingPartyCsrResource csrResource) {
        return restClient.post()
                .uri("/{id}/certs/access", id)
                .body(csrResource)
                .retrieve()
                .toEntity(RelyingPartyAccessCertificateResource.class)
                .getBody();
    }
}
