package no.idporten.eudiw.rp.admin.integrations.issuermetadata;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class IssuerMetadataConfiguration {

    private static final Timeout SOCKET_TIMEOUT = Timeout.ofSeconds(3);

    @Bean(destroyMethod = "close")
    public CloseableHttpClient issuerMetadataHttpClient() {
        var connections = PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(SOCKET_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build())
            .build();

        return HttpClients.custom()
            .setConnectionManager(connections)
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(SOCKET_TIMEOUT)
                .setResponseTimeout(SOCKET_TIMEOUT)
                .build())
            .disableRedirectHandling()
            .disableAutomaticRetries()
            .disableContentCompression()
            .disableCookieManagement()
            .build();
    }

    @Bean
    public RestClient issuerMetadataRestClient(
        RestClient.Builder builder,
        @Qualifier("issuerMetadataHttpClient") CloseableHttpClient httpClient
    ) {
        return builder.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient)).build();
    }
}
