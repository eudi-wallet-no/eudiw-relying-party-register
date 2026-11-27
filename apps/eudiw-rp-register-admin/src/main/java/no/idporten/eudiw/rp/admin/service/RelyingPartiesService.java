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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RelyingPartiesService {

    private final V2RegisterClient registerClient;
    private final V2CertificateClient certificateClient;

    public UUID createInstance(V2RegisterClient.CreateInstanceRequest request) {
        return registerClient.createInstance(request);
    }

    public PagedResponse<RegistrationSearchResult> search(SearchRelyingPartyResource searchResource) {
        var instances = registerClient.searchInstances(searchResource);
        if (instances.content().isEmpty()) {
            return new PagedResponse<>(List.of(), instances.page());
        }

        var entitlementCatalog = registerClient.getEntitlements(true).stream()
            .collect(Collectors.toMap(V2RegisterClient.Entitlement::entitlement, Function.identity()));
        var results = instances.content().stream()
            .map(instance -> toSearchResult(instance, entitlementCatalog))
            .toList();
        return new PagedResponse<>(results, instances.page());
    }

    public RelyingPartyResource get(UUID id) {
        return registerClient.getRelyingParty(id);
    }

    public void createIntendedUse(UUID serviceId, V2RegisterClient.CreateIntendedUseRequest request) {
        // TODO: Legg inn kontroll mot revokert/inaktiv status før oppretting av føremål.
        registerClient.createIntendedUse(serviceId, request);
    }

    public void updateInstanceStatus(UUID instanceId, boolean active) {
        registerClient.updateInstanceStatus(instanceId, active);
    }

    public void revokeRelyingParty(UUID relyingPartyId) {
        registerClient.revokeRelyingParty(relyingPartyId);
    }

    public void updateServiceInformation(UUID serviceId, V2RegisterClient.ServiceInformation information) {
        registerClient.updateServiceInformation(serviceId, information);
    }

    public void updateInstanceRoles(UUID instanceId, V2RegisterClient.InstanceRolesRequest request) {
        registerClient.updateInstanceRoles(instanceId, request);
    }

    public List<V2RegisterClient.ServiceCredentialIssuerUrl> getServiceCredentialIssuerUrls(UUID serviceId) {
        return registerClient.getServiceCredentialIssuerUrls(serviceId);
    }

    public List<String> getServiceEntitlements(UUID serviceId) {
        return registerClient.getServiceEntitlements(serviceId);
    }

    public void updateServiceCredentialIssuerUrls(UUID serviceId,
                                                  List<V2RegisterClient.ServiceCredentialIssuerUrl> credentialIssuerUrls) {
        registerClient.updateServiceCredentialIssuerUrls(serviceId, credentialIssuerUrls);
    }

    public void updateServiceEntitlements(UUID serviceId, V2RegisterClient.ServiceEntitlementsRequest request) {
        registerClient.updateServiceEntitlements(serviceId, request);
    }

    public Map<String, String> getEntitlementDisplayNames() {
        return registerClient.getEntitlements(true).stream()
            .filter(entitlement -> entitlement.displayName() != null)
            .collect(Collectors.toMap(V2RegisterClient.Entitlement::entitlement, V2RegisterClient.Entitlement::displayName));
    }

    public void updateInstanceEaas(UUID instanceId, V2RegisterClient.InstanceEaasRequest request) {
        registerClient.updateInstanceEaas(instanceId, request);
    }

    public V2RegisterClient.RelyingParty getRelyingPartyInformation(UUID relyingPartyId) {
        return registerClient.getRelyingPartyInformation(relyingPartyId);
    }

    public void updateRelyingPartyInformation(UUID relyingPartyId, V2RegisterClient.RelyingPartyInformation information) {
        registerClient.updateRelyingPartyInformation(relyingPartyId, information);
    }

    public RelyingPartyCertificateResource getAccessCertificate(
        UUID relyingPartyId, UUID certificateId) {
        return certificateClient.getAccessCertificate(relyingPartyId, certificateId);
    }

    public RelyingPartyCertificateResource getIssuerCertificate(UUID relyingPartyId, UUID certificateId) {
        return certificateClient.getIssuerCertificate(relyingPartyId, certificateId);
    }

    public RelyingPartyCertificateResource requestCertificateForRelyingParty(
        UUID id,
        RelyingPartyCsrResource csrResource) {
        return certificateClient.requestAccessCertificate(id, csrResource);
    }

    public RelyingPartyCertificateResource requestIssuerCertificateForEntitlement(
        UUID id,
        IssuerCsrResource csrResource
    ) {
        return certificateClient.requestIssuerCertificate(id, csrResource);
    }

    public EntitlementsResource getValidEntitlements() {
        return new EntitlementsResource(registerClient.getEntitlements(false).stream()
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
        certificateClient.revokeAccessCertificate(relyingPartyId, certificateId);
    }

    public void revokeIssuerCertificate(UUID relyingPartyId, UUID certificateId) {
        certificateClient.revokeIssuerCertificate(relyingPartyId, certificateId);
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
