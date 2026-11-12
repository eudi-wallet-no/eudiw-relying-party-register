package no.idporten.eudiw.rp.admin.integrations.registerservice.config;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClientErrorHandler;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class V2CertificateClientConfig {

    private final RelyingPartiesServiceProperties properties;

    @Bean(destroyMethod = "close")
    public CloseableHttpClient v2CertificateHttpClient() {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(properties.restClient().connectTimeoutMillis()))
                .build())
            .build();
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .disableAutomaticRetries()
            .disableRedirectHandling()
            .build();
    }

    @Bean
    public RestClient v2CertificateRestClient(RestClient.Builder builder,
        @Qualifier("v2CertificateHttpClient") CloseableHttpClient httpClient) {
        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectionRequestTimeout((int) properties.restClient().connectTimeoutMillis());
        requestFactory.setReadTimeout((int) properties.restClient().readTimeoutMillis());

        return builder
            .baseUrl(properties.registerServiceApi().registerServiceBaseUri() + "/v2")
            .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(properties.registerServiceApi().apiKeyHeaderId(),
                properties.registerServiceApi().apiKeyValue())
            .defaultStatusHandler(new V2RegisterClientErrorHandler())
            .requestFactory(requestFactory)
            .build();
    }
}
