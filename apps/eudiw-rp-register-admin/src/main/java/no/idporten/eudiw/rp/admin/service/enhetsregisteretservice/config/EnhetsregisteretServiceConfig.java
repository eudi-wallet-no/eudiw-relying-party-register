package no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.config;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretService;
import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretServiceResponseErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class EnhetsregisteretServiceConfig {

    private final EnhetsregisteretServiceProperties enhetsregisteretServiceProperties;

    @Bean
    public EnhetsregisteretService enhetsregisteretService() {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(
            enhetsregisteretServiceProperties.restClient().connectTimeoutMillis());
        requestFactory.setReadTimeout(
            enhetsregisteretServiceProperties.restClient().readTimeoutMillis());

        RestClient enhetsregisteretRestClient =
            RestClient.builder()
                      .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                      .baseUrl(enhetsregisteretServiceProperties.enhetsregisteretApiBaseUri())
                      .requestFactory(requestFactory)
                      .defaultStatusHandler(new EnhetsregisteretServiceResponseErrorHandler())
                      .build();

        return new EnhetsregisteretService(
            enhetsregisteretRestClient,
            enhetsregisteretServiceProperties.knownPublicSectorCodes());
    }

    @Bean
    @Profile("dev")
    @Primary
    public EnhetsregisteretService dummyEnhetsregisteretService() {
        return new EnhetsregisteretService(null, null) {
            private final Random rng = new Random();
            @Override
            public boolean getPublicSectorForOrgno(String orgno) {
                return rng.nextBoolean();
            }
        };
    }
}
