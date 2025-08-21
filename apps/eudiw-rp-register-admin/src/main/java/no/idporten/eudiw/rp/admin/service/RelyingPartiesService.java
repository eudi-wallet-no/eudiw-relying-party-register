package no.idporten.eudiw.rp.admin.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.RelyingPartyNotFoundException;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificatesResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.entitlement.EntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.entitlement.EntitlementsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartiesService {

    private final RestClient restClient;

    public RelyingPartyResource create(CreateRelyingPartyResource createResource) {
        return restClient.post()
                .uri("/rp")
                .body(createResource)
                .retrieve()
                .toEntity(RelyingPartyResource.class)
                .getBody();
    }

    public RelyingPartyResource edit(UUID id, EditRelyingPartyResource editResource) {
        return restClient.put()
                .uri("/rp/{id}", id)
                .body(editResource)
                .retrieve()
                .toEntity(RelyingPartyResource.class)
                .getBody();
    }

    public RelyingPartiesResource search(SearchRelyingPartyResource searchResource) {
        return restClient.post()
                .uri("/rp/search")
                .body(searchResource)
                .retrieve()
                .toEntity(RelyingPartiesResource.class)
                .getBody();
    }

    public RelyingPartiesResource getAll() {
        return restClient.get()
                .uri("/rp")
                .retrieve()
                .toEntity(RelyingPartiesResource.class)
                .getBody();
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

    public void delete(UUID id) {
        restClient.delete()
                  .uri("/rp/{id}", id)
                  .retrieve()
                  .toBodilessEntity();
    }

    public RelyingPartyAccessCertificatesResource getCertificatesForRelyingParty(
        UUID id) {
        return restClient.get()
                .uri("/rp/{id}/certs", id)
                .retrieve()
                .toEntity(RelyingPartyAccessCertificatesResource.class)
                .getBody();
    }

    public RelyingPartyAccessCertificateResource getCertificate(
        UUID relyingPartyId, UUID certificateId) {
        return restClient.get()
                         .uri("/rp/{rp-id}/certs/{cert-id}", relyingPartyId, certificateId)
                         .retrieve()
                         .toEntity(RelyingPartyAccessCertificateResource.class)
                         .getBody();
    }

    public RelyingPartyAccessCertificateResource requestCertificateForRelyingParty(
        UUID id,
        RelyingPartyCsrResource csrResource) {
        return restClient.post()
                .uri("/rp/{id}/certs/access", id)
                .body(csrResource)
                .retrieve()
                .toEntity(RelyingPartyAccessCertificateResource.class)
                .getBody();
    }

    public EntitlementsResource getValidEntitlements() {
        return new EntitlementsResource(
            Objects.requireNonNull( // should never fire since the ResponseErrorHandler would fire first.
                       restClient.get()
                                 .uri("/entitlement")
                                 .retrieve()
                                 .toEntity(EntitlementsResource.class)
                                 .getBody())
                   .entitlements()
                   .stream()
                   .filter(EntitlementResource::active)
                   .toList()
        );
    }
    public List<String> getValidEntitlementValues() {
        return this.getValidEntitlements()
                   .entitlements()
                   .stream()
                   .map(EntitlementResource::entitlement)
                   .toList();
    }

    @ExceptionHandler(Exception.class)
    public void wrapUnexpectedRestClientException(Exception e) {
        throw new AdminServiceException("Register service REST client exception", e);
    }
}
