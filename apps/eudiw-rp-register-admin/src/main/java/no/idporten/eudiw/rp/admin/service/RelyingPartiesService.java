package no.idporten.eudiw.rp.admin.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.*;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.entitlement.EntitlementsResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
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

    public PagedResponse<RelyingPartyResource> search(SearchRelyingPartyResource searchResource) {
        return restClient.post()
            .uri("/rp/search")
            .body(searchResource)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    public RelyingPartiesResource getAllByOrgno(String orgno) {
        SearchRelyingPartyResource searchResource =
            new SearchRelyingPartyResource()
                .withSearchTerm(orgno)
                .withIncludeInactive(true)
                .withPageSize(Integer.MAX_VALUE)
                .withOrdering(RelyingPartyOrdering.CREATED_ASC);
        List<RelyingPartyResource> searchResult =
            this.search(searchResource)
                .content()
                .stream()
                // must filter by orgno since the search is by orgno *and* name.
                .filter(rp -> Objects.equals(rp.orgno(), orgno))
                .toList();
        return new RelyingPartiesResource(searchResult);
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

    public RelyingPartyCertificateResource getAccessCertificate(
        UUID relyingPartyId, UUID certificateId) {
        return restClient.get()
                         .uri("/rp/{rp-id}/certs/access/{cert-id}", relyingPartyId, certificateId)
                         .retrieve()
                         .toEntity(RelyingPartyCertificateResource.class)
                         .getBody();
    }

    public RelyingPartyCertificateResource getIssuerCertificate(UUID relyingPartyId, UUID certificateId) {
        return restClient.get()
            .uri("/rp/{rp-id}/certs/issuer/{cert-id}", relyingPartyId, certificateId)
            .retrieve()
            .toEntity(RelyingPartyCertificateResource.class)
            .getBody();
    }

    public RelyingPartyCertificateResource requestCertificateForRelyingParty(
        UUID id,
        RelyingPartyCsrResource csrResource) {
        return restClient.post()
                .uri("/rp/{id}/certs/access", id)
                .body(csrResource)
                .retrieve()
                .toEntity(RelyingPartyCertificateResource.class)
                .getBody();
    }

    public RelyingPartyCertificateResource requestIssuerCertificateForEntitlement(
        UUID id,
        IssuerCsrResource csrResource
    ) {
        return restClient.post()
            .uri("/rp/{id}/certs/issuer", id)
            .body(csrResource)
            .retrieve()
            .toEntity(RelyingPartyCertificateResource.class)
            .getBody();
    }

    public EntitlementsResource getValidEntitlements() {
        return restClient.get()
                         .uri("/entitlement")
                         .retrieve()
                         .toEntity(EntitlementsResource.class)
                         .getBody();
    }

    public List<RelyingPartyEntitlementFormField> getValidEntitlementOptions() {
        return this.getValidEntitlements()
                   .entitlements()
                   .stream()
                   .map(e -> new RelyingPartyEntitlementFormField(e.entitlement(), null, e.displayName()))
                   .toList();
    }

    public void revokeAccessCertificate(UUID relyingPartyId, UUID certificateId) {
        restClient.patch()
            .uri("/rp/{relying-party-id}/certs/access/{certificate-id}/revoke", relyingPartyId, certificateId)
            .retrieve()
            .onStatus(status -> status.value() == 404, (request, response) -> {
                throw new CertificateNotFoundException(
                        response.getStatusCode() + " Access certificate not found: " + response.getStatusText(),
                        relyingPartyId.toString(),
                        certificateId.toString());
            })
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                throw new BadRequestException(response.getStatusCode() + " Client error: " + response.getStatusText());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new ErrorResponseException(response.getStatusCode().toString(), " Server error: " + response.getStatusText());
            })
            .toBodilessEntity();
    }

    public void revokeIssuerCertificate(UUID relyingPartyId, UUID certificateId) {
        restClient.patch()
            .uri("/rp/{relying-party-id}/certs/issuer/{certificate-id}/revoke", relyingPartyId, certificateId)
            .retrieve()
            .onStatus(status -> status.value() == 404, (request, response) -> {
                throw new CertificateNotFoundException(
                        response.getStatusCode() + " Issuer certificate not found: " + response.getStatusText(),
                        relyingPartyId.toString(),
                        certificateId.toString());
            })
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                throw new BadRequestException(response.getStatusCode() + " Client error: " + response.getStatusText());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new ErrorResponseException(response.getStatusCode().toString(), " Server error: " + response.getStatusText());
            })
            .toBodilessEntity();
    }

    @ExceptionHandler(Exception.class)
    public void wrapUnexpectedRestClientException(Exception e) {
        throw new AdminServiceException("Register service REST client exception", e);
    }
}
