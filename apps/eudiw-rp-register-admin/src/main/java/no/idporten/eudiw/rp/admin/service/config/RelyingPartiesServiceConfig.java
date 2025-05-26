package no.idporten.eudiw.rp.admin.service.config;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesServiceResponseErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class RelyingPartiesServiceConfig {

    private final RelyingPartiesServiceProperties rpServiceProperties;
    private static final String registerServiceApiRpEndpoint = "/v1/rp";

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) rpServiceProperties.restClient().connectTimeoutMillis());
        requestFactory.setReadTimeout((int) rpServiceProperties.restClient().readTimeoutMillis());
        URI registerServiceRestClientBaseUrl = URI.create(
            rpServiceProperties.registerServiceApi().registerServiceBaseUri()
                + registerServiceApiRpEndpoint);
        return
            RestClient.builder()
                      .defaultHeader("accept", MediaType.APPLICATION_JSON.toString())
                      .defaultHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
                      .defaultHeader(rpServiceProperties.registerServiceApi().apiKeyHeaderId(),
                                     rpServiceProperties.registerServiceApi().apiKeyValue())
                      .baseUrl(registerServiceRestClientBaseUrl)
                      .defaultStatusHandler(new RelyingPartiesServiceResponseErrorHandler())
                      .requestFactory(requestFactory)
                      .build();
    }
}
