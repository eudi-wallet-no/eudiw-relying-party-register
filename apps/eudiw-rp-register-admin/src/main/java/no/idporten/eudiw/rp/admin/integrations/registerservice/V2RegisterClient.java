package no.idporten.eudiw.rp.admin.integrations.registerservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEaaResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.RelyingPartyNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.PagedResponse;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class V2RegisterClient {

    private final RestClient restClient;

    public V2RegisterClient(@Qualifier("v2RestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Registration> getRegistrations(String orgno) {
        List<Registration> registrations;
        try {
            registrations = restClient.get()
                .uri(builder -> builder.path("/registrations").queryParam("orgno", orgno).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to retrieve registrations", e);
        }
        if (registrations == null) {
            throw new UnrecognizedErrorResponseException(
                "Unable to retrieve registrations from register service: response body is empty");
        }
        return registrations;
    }

    public RelyingPartyResource getRelyingParty(UUID relyingPartyInstanceId) {
        String registrationPath = "/registrations/" + relyingPartyInstanceId;
        RegistrationDetails registrationDetails;

        try {
            registrationDetails = restClient.get()
                .uri(registrationPath)
                .retrieve()
                .body(RegistrationDetails.class);
        } catch (NotFoundException e) {
            var notFound = new RelyingPartyNotFoundException(e.getErrorDescription(), relyingPartyInstanceId.toString());
            notFound.initCause(e);
            throw notFound;
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to retrieve " + registrationPath, e);
        }

        if (registrationDetails == null) {
            throw new UnrecognizedErrorResponseException(
                "Unable to retrieve " + registrationPath + " from register service: response body is empty");
        }

        return registrationDetails.toRelyingPartyResource();
    }

    public PagedResponse<Instance> searchInstances(SearchRelyingPartyResource searchResource) {
        PagedResponse<Instance> instances;
        try {
            instances = restClient.get()
                .uri(builder -> {
                    var parameters = new ArrayList<Object>();
                    parameters.add(searchResource.getSearchTerm());
                    builder.path("/instances")
                        .queryParam("searchTerm", "{searchTerm}")
                        .queryParam("includeInactive", searchResource.isIncludeInactive())
                        .queryParam("page", searchResource.getPage())
                        .queryParam("pageSize", searchResource.getPageSize())
                        .queryParam("sort", "{sort}");
                    parameters.add(searchResource.getOrdering());
                    searchResource.getRequiredEntitlements().forEach(entitlement -> {
                        builder.queryParam("requiredEntitlements", "{entitlement" + parameters.size() + "}");
                        parameters.add(entitlement);
                    });
                    return builder.build(parameters.toArray());
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to search instances", e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read instance search response from register service", e);
        }

        return Optional.ofNullable(instances)
            .filter(V2RegisterClient::hasRequiredSearchData)
            .orElseThrow(() -> new UnrecognizedErrorResponseException(
                "Unable to search instances in register service: response is missing search data"));
    }

    public List<Entitlement> getEntitlements(boolean includeInactive) {
        Entitlements catalog;
        try {
            catalog = restClient.get()
                .uri(builder -> builder.path("/entitlements").queryParam("includeInactive", includeInactive).build())
                .retrieve()
                .body(Entitlements.class);
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to retrieve entitlements", e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read entitlement catalog from register service", e);
        }

        return Optional.ofNullable(catalog)
            .map(Entitlements::entitlements)
            .filter(entitlements -> entitlements.stream().allMatch(V2RegisterClient::hasRequiredEntitlementData))
            .orElseThrow(() -> new UnrecognizedErrorResponseException(
                "Unable to retrieve entitlements from register service: response is missing the catalog"));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Instance(UUID id, String orgno, String organizationName, String serviceTradeName,
                           List<RelyingPartyEntitlement> relyingPartyEntitlements,
                           long createdMs, long lastUpdatedMs,
                           List<UUID> accessCertificateIds, List<UUID> issuerCertificateIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entitlement(UUID id, String entitlement, boolean active, String displayName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entitlements(List<Entitlement> entitlements) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegistrationDetails(
        UUID id,
        UUID relyingPartyServiceId,
        UUID relyingPartyId,
        String orgno,
        String organizationName,
        String serviceTradeName,
        boolean isPSB,
        List<RelyingPartyEntitlement> relyingPartyEntitlements,
        List<RelyingPartyEaaResource> relyingPartyEaas,
        List<RelyingPartyCertificateResource> accessCertificates,
        List<RelyingPartyCertificateResource> issuerCertificates,
        long createdMs,
        long lastUpdatedMs,
        boolean active
    ) {
        public RelyingPartyResource toRelyingPartyResource() {
            return new RelyingPartyResource(
                id,
                orgno,
                serviceTradeName,
                organizationName,
                isPSB,
                relyingPartyEntitlements.stream()
                    .map(relyingPartyEntitlement -> new RelyingPartyEntitlementResource(
                        relyingPartyEntitlement.entitlement(),
                        relyingPartyEntitlement.displayName(),
                        relyingPartyEntitlement.credentialIssuerUrl()))
                    .toList(),
                relyingPartyEaas,
                accessCertificates,
                issuerCertificates,
                createdMs,
                lastUpdatedMs,
                active);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelyingPartyEntitlement(String entitlement, String displayName, String credentialIssuerUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Registration(UUID id, String serviceTradeName, List<Role> relyingPartyEntitlements,
                               Long createdMs, Long lastUpdatedMs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Role(String displayName) {
    }

    private static boolean hasRequiredSearchData(PagedResponse<Instance> searchResponse) {
        return searchResponse.content() != null
            && searchResponse.page() != null
            && searchResponse.content().stream().allMatch(V2RegisterClient::hasRequiredInstanceData);
    }

    private static boolean hasRequiredInstanceData(Instance relyingPartyInstance) {
        return relyingPartyInstance != null
            && relyingPartyInstance.id() != null
            && relyingPartyInstance.orgno() != null
            && relyingPartyInstance.relyingPartyEntitlements() != null
            && relyingPartyInstance.accessCertificateIds() != null
            && relyingPartyInstance.issuerCertificateIds() != null;
    }

    private static boolean hasRequiredEntitlementData(Entitlement entitlement) {
        return entitlement != null && entitlement.entitlement() != null;
    }
}
