package no.idporten.eudiw.rp.register.lookup.service.config;

import no.idporten.eudiw.rp.register.lookup.service.LookupServiceResponseErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LookupServiceConfig {

    private final LookupServiceProperties.RestClientDefaults restClientDefaults;

    public LookupServiceConfig(LookupServiceProperties.RestClientDefaults restClientDefaults) {
        this.restClientDefaults = restClientDefaults;
    }

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) restClientDefaults.connectTimeoutMillis());
        requestFactory.setReadTimeout((int) restClientDefaults.readTimeoutMillis());
        return
            RestClient.builder()
                      .defaultHeader("accept", MediaType.APPLICATION_JSON.toString())
                      .defaultHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
                      .defaultHeader(restClientDefaults.apiKeyHeaderId(),
                                     restClientDefaults.apiKeyValue())
                      .baseUrl(restClientDefaults.registerServiceApiBaseUri())
                      .defaultStatusHandler(new LookupServiceResponseErrorHandler())
                      .requestFactory(requestFactory)
                      .build();
    }
}
