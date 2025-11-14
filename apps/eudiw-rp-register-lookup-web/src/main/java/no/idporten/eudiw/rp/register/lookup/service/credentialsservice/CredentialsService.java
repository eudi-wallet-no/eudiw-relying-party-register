package no.idporten.eudiw.rp.register.lookup.service.credentialsservice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.register.lookup.service.exception.NotFoundException;
import no.idporten.eudiw.rp.register.lookup.service.exception.ResponseValidationException;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialsResource;
import org.springframework.web.client.RestClient;

import java.util.Set;

@RequiredArgsConstructor
public class CredentialsService {
    private final RestClient restClient;

    private final Validator validator;

    public CredentialsResource getAvailableCredentials() {
        return doValidate(
            restClient.get()
                      .uri("/credentials")
                      .retrieve()
                      .toEntity(CredentialsResource.class)
                      .getBody()
        );
    }

    public CredentialResource getCredential(String issuer, String configurationId) {
        return getAvailableCredentials()
                   .credentials()
                   .stream()
                   .filter(credential ->
                       credential.issuer().equals(issuer)
                           && credential.configurationId().equals(configurationId)
                   )
                   .findFirst()
                   .orElseThrow(() ->
                       new NotFoundException(
                           "Found no credential with issuer=%s, config ID=%s"
                               .formatted(issuer, configurationId)));
    }

    private <T> T doValidate(T value) {
        Set<ConstraintViolation<T>> validationViolations = validator.validate(value);
        if (!validationViolations.isEmpty()) {
            throw new ResponseValidationException(
                "Credential service response validation failed", validationViolations);
        }
        return value;
    }
}
