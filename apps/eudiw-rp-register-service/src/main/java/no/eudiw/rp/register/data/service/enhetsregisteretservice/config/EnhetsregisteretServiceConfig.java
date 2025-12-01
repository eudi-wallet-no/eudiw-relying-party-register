package no.eudiw.rp.register.data.service.enhetsregisteretservice.config;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.data.service.enhetsregisteretservice.EnhetsregisteretService;
import no.eudiw.rp.register.data.service.enhetsregisteretservice.EnhetsregisteretServiceResponseErrorHandler;
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
    public EnhetsregisteretService enhetsregisteretService(Validator validator) {

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
            enhetsregisteretServiceProperties.knownPublicSectorCodes(),
            validator);
    }

    @Bean
    @Profile("dev | junit")
    @Primary
    public EnhetsregisteretService dummyEnhetsregisteretService() {
        return new EnhetsregisteretService(null, null, null) {
            private final Random rng = new Random();
            @Override
            public EnhetsregisteretResponse queryOrgno(String orgno) {
                String name = "RP-%s".formatted(orgno);
                return new EnhetsregisteretResponse(name, rng.nextBoolean());
            }
        };
    }
}
