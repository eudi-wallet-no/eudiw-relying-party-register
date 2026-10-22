package no.idporten.eudiw.rp.admin.integrations.registerservice.config;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClientErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class RelyingPartiesServiceConfig {

    private final RelyingPartiesServiceProperties rpServiceProperties;

    @Bean
    @Primary
    public RestClient restClient() {
        return createRestClient("/v1");
    }

    @Bean
    public RestClient v2RestClient() {
        return createRestClient("/v2");
    }

    private RestClient createRestClient(String apiEndpoint) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectionRequestTimeout((int) rpServiceProperties.restClient().connectTimeoutMillis());
        requestFactory.setReadTimeout((int) rpServiceProperties.restClient().readTimeoutMillis());
        URI registerServiceRestClientBaseUrl = URI.create(
            rpServiceProperties.registerServiceApi().registerServiceBaseUri()
                + apiEndpoint);
        return
            RestClient.builder()
                      .defaultHeader("accept", MediaType.APPLICATION_JSON.toString())
                      .defaultHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
                      .defaultHeader(rpServiceProperties.registerServiceApi().apiKeyHeaderId(),
                                     rpServiceProperties.registerServiceApi().apiKeyValue())
                      .baseUrl(registerServiceRestClientBaseUrl)
                      .defaultStatusHandler(new V2RegisterClientErrorHandler())
                      .requestFactory(requestFactory)
                      .build();
    }
}
