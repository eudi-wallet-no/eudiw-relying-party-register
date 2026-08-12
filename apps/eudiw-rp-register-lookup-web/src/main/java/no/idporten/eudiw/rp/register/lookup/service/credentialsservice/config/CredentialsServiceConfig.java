package no.idporten.eudiw.rp.register.lookup.service.credentialsservice.config;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsServiceResponseErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class CredentialsServiceConfig {

    private final CredentialsServiceProperties credentialsServiceProperties;
    private static final String credentialsApiEndpoint = "/v1";

    private RestClient credentialsServiceRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) credentialsServiceProperties.restClient().connectTimeoutMillis());
        requestFactory.setReadTimeout((int) credentialsServiceProperties.restClient().readTimeoutMillis());
        URI credentialsRegistryRestClientBaseUrl = URI.create(
            credentialsServiceProperties.credentialsRegistryApi().credentialsRegistryBaseUri()
                + credentialsApiEndpoint);

        return
            RestClient.builder()
                      .defaultHeader("accept", MediaType.APPLICATION_JSON.toString())
                      .baseUrl(credentialsRegistryRestClientBaseUrl)
                      .defaultStatusHandler(new CredentialsServiceResponseErrorHandler())
                      .requestFactory(requestFactory)
                      .build();
    }

    @Bean
    public CredentialsService credentialsService(Validator validator) {
        return new CredentialsService(credentialsServiceRestClient(), validator);
    }
}
