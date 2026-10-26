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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
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
            registrationDetails = restClient.get().uri(registrationPath).retrieve().body(RegistrationDetails.class);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegistrationDetails(UUID id, UUID relyingPartyServiceId, UUID relyingPartyId,
                                      String orgno, String organizationName, String serviceTradeName, boolean isPSB,
                                      List<RelyingPartyEntitlement> relyingPartyEntitlements,
                                      List<RelyingPartyEaaResource> relyingPartyEaas,
                                      List<RelyingPartyCertificateResource> accessCertificates,
                                      List<RelyingPartyCertificateResource> issuerCertificates,
                                      long createdMs, long lastUpdatedMs, boolean active) {
        public RelyingPartyResource toRelyingPartyResource() {
            return new RelyingPartyResource(id, orgno, serviceTradeName, organizationName, isPSB,
                relyingPartyEntitlements.stream().map(relyingPartyEntitlement -> new RelyingPartyEntitlementResource(
                    relyingPartyEntitlement.entitlement(), relyingPartyEntitlement.displayName(),
                    relyingPartyEntitlement.credentialIssuerUrl())).toList(),
                relyingPartyEaas, accessCertificates, issuerCertificates, createdMs, lastUpdatedMs, active);
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
}
