package no.idporten.eudiw.rp.admin.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2CertificateClient;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.entitlement.EntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.entitlement.EntitlementsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RelyingPartiesService {

    private final RestClient restClient;
    private final V2RegisterClient v2RegisterClient;
    private final V2CertificateClient v2CertificateClient;

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

    public PagedResponse<RegistrationSearchResult> search(SearchRelyingPartyResource searchResource) {
        var instances = v2RegisterClient.searchInstances(searchResource);
        if (instances.content().isEmpty()) {
            return new PagedResponse<>(List.of(), instances.page());
        }

        var entitlementCatalog = v2RegisterClient.getEntitlements(true).stream()
            .collect(Collectors.toMap(V2RegisterClient.Entitlement::entitlement, Function.identity()));
        var results = instances.content().stream()
            .map(instance -> toSearchResult(instance, entitlementCatalog))
            .toList();
        return new PagedResponse<>(results, instances.page());
    }

    public RelyingPartyResource get(UUID id) {
        return v2RegisterClient.getRelyingParty(id);
    }

    public RelyingPartyCertificateResource getAccessCertificate(
        UUID relyingPartyId, UUID certificateId) {
        return v2CertificateClient.getAccessCertificate(relyingPartyId, certificateId);
    }

    public RelyingPartyCertificateResource getIssuerCertificate(UUID relyingPartyId, UUID certificateId) {
        return v2CertificateClient.getIssuerCertificate(relyingPartyId, certificateId);
    }

    public RelyingPartyCertificateResource requestCertificateForRelyingParty(
        UUID id,
        RelyingPartyCsrResource csrResource) {
        return v2CertificateClient.requestAccessCertificate(id, csrResource);
    }

    public RelyingPartyCertificateResource requestIssuerCertificateForEntitlement(
        UUID id,
        IssuerCsrResource csrResource
    ) {
        return v2CertificateClient.requestIssuerCertificate(id, csrResource);
    }

    public EntitlementsResource getValidEntitlements() {
        return new EntitlementsResource(v2RegisterClient.getEntitlements(false).stream()
            .map(entitlement -> new EntitlementResource(
                entitlement.id(), entitlement.entitlement(), entitlement.displayName(), entitlement.active()))
            .toList());
    }

    public List<RelyingPartyEntitlementFormField> getValidEntitlementOptions() {
        return this.getValidEntitlements()
                   .entitlements()
                   .stream()
                   .map(e -> new RelyingPartyEntitlementFormField(e.entitlement(), null, e.displayName()))
                   .toList();
    }

    public void revokeAccessCertificate(UUID relyingPartyId, UUID certificateId) {
        v2CertificateClient.revokeAccessCertificate(relyingPartyId, certificateId);
    }

    public void revokeIssuerCertificate(UUID relyingPartyId, UUID certificateId) {
        v2CertificateClient.revokeIssuerCertificate(relyingPartyId, certificateId);
    }

    @ExceptionHandler(Exception.class)
    public void wrapUnexpectedRestClientException(Exception e) {
        throw new AdminServiceException("Register service REST client exception", e);
    }

    private RegistrationSearchResult toSearchResult(
        V2RegisterClient.Instance instance, Map<String, V2RegisterClient.Entitlement> entitlementCatalog) {
        var entitlements = instance.relyingPartyEntitlements().stream()
            .map(role -> {
                var catalogEntry = entitlementCatalog.get(role.entitlement());
                return new RelyingPartyEntitlementResource(
                    role.entitlement(), catalogEntry == null ? role.entitlement() : catalogEntry.displayName(),
                    role.credentialIssuerUrl());
            })
            .toList();

        return new RegistrationSearchResult(
            instance.id(), instance.orgno(), instance.organizationName(), instance.serviceTradeName(), entitlements,
            instance.createdMs(), instance.lastUpdatedMs(),
            instance.accessCertificateIds().size(), instance.issuerCertificateIds().size());
    }
}
