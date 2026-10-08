package no.idporten.eudiw.rp.admin.service.credentialsservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CredentialsService {
    private final RestClient restClient;

    public record CredentialValidationError(String name, List<String> errors) { }

    public List<CredentialValidationError> getCredentialErrors(String issuer) {
        try {
            var errors = restClient.post().uri("/v1/credential-errors")
                    .body(Map.of("issuer", issuer)).retrieve()
                    .body(new ParameterizedTypeReference<List<CredentialValidationError>>() { });
            if (errors == null) throw new RestClientException("Empty credential errors response");
            return errors;
        } catch (RestClientException e) {
            log.warn("Failed fetching credential errors for issuer {}", issuer, e);
            return List.of(new CredentialValidationError("Utstedarmetadata",
                    List.of("Kunne ikkje hente valideringsfeil frå bevisregisteret.")));
        }
    }
}
