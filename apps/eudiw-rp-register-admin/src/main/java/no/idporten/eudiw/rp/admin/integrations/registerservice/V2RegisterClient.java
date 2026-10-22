package no.idporten.eudiw.rp.admin.integrations.registerservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Registration(UUID id, String serviceTradeName, List<Role> relyingPartyEntitlements,
                               Long createdMs, Long lastUpdatedMs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Role(String displayName) {
    }
}
