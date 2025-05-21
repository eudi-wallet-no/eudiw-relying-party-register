package no.eudiw.rp.register.data.service.accesscertificates;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class RelyingPartyCertificatesServiceConfig {

    private final RelyingPartyCertificatesServiceProperties certServiceProperties;

    private static final String caServiceApiCertsEndpoint = "/v1/certs";

    @Bean("caRestClient")
    public RestClient caRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) certServiceProperties.restClient().connectTimeoutMillis());
        requestFactory.setReadTimeout((int) certServiceProperties.restClient().readTimeoutMillis());
        URI caServiceCertsApiBaseUrl = URI.create(
            certServiceProperties.caServiceApi().caServiceBaseUri() + caServiceApiCertsEndpoint);
        return
            RestClient.builder()
                      .defaultHeader("accept", "application/x-pem-file", MediaType.APPLICATION_JSON_VALUE)
                      .defaultHeader("Content-Type", "application/x-pem-file")
                      .defaultHeader(certServiceProperties.caServiceApi().apiKeyHeaderId(),
                                     certServiceProperties.caServiceApi().apiKeyValue())
                      .baseUrl(caServiceCertsApiBaseUrl)
                      .defaultStatusHandler(new RelyingPartyCertificatesServiceResponseErrorHandler())
                      .requestFactory(requestFactory)
                      .build();
    }
}
