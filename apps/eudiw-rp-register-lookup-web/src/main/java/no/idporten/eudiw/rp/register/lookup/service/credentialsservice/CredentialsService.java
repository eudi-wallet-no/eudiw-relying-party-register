package no.idporten.eudiw.rp.register.lookup.service.credentialsservice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.exception.NotFoundException;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialsResource;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class CredentialsService {
    private final RestClient restClient;

    private final Validator validator;

    public CredentialsResource getAvailableCredentials() {
        CredentialsResource credentialsResource =
            Objects.requireNonNull(
                restClient.get()
                          .uri("/credentials")
                          .retrieve()
                          .toEntity(CredentialsResource.class)
                          .getBody());
        List<CredentialResource> validCredentialResources =
            credentialsResource.credentials()
                               .stream()
                               .filter(this::isValidCredentialResource)
                               .toList();
        return new CredentialsResource(validCredentialResources);
    }

    public CredentialResource getCredential(String issuer, String configurationId) {
        return getAvailableCredentials()
                   .credentials()
                   .stream()
                   .filter(credential ->
                       credential.getIssuer().equals(issuer)
                           && credential.getConfigurationId().equals(configurationId)
                   )
                   .findFirst()
                   .orElseThrow(() ->
                       new NotFoundException(
                           "Found no credential with issuer=%s, config ID=%s"
                               .formatted(issuer, configurationId)));
    }

    private boolean isValidCredentialResource(CredentialResource credential) {
        Set<ConstraintViolation<CredentialResource>> validationViolations =
            validator.validate(credential);
        if (!validationViolations.isEmpty()) {
            String validationErrors =
                validationViolations.stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining(", "));
            log.info("Ignoring invalid credential resource for issuer={}, config-id={}: {}",
                     credential.getIssuer(),
                     credential.getConfigurationId(),
                     validationErrors);
            return false;
        }
        return true;
    }
}
