package no.idporten.eudiw.rp.admin.service.credentialsservice.config;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.config.RelyingPartiesServiceProperties;
import no.idporten.eudiw.rp.admin.service.credentialsservice.CredentialsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class CredentialsServiceConfig {
    private final RelyingPartiesServiceProperties rpServiceProperties;

    @Bean
    public CredentialsService credentialsService(
            @Value("${eudiw-admin-web.credential-registry-base-uri}") String baseUri) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) rpServiceProperties.restClient().connectTimeoutMillis());
        requestFactory.setReadTimeout(15000);
        RestClient restClient = RestClient.builder().baseUrl(baseUri)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(rpServiceProperties.registerServiceApi().apiKeyHeaderId(),
                        rpServiceProperties.registerServiceApi().apiKeyValue())
                .requestFactory(requestFactory).build();
        return new CredentialsService(restClient);
    }
}
