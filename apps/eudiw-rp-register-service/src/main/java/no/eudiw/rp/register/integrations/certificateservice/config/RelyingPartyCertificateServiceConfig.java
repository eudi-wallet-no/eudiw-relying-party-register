package no.eudiw.rp.register.integrations.certificateservice.config;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.integrations.certificateservice.RelyingPartyCertificateServiceResponseErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class RelyingPartyCertificateServiceConfig {

    private final RelyingPartyCertificateServiceProperties certServiceProperties;

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
                      .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                      .defaultHeader(certServiceProperties.caServiceApi().apiKeyHeaderId(),
                                     certServiceProperties.caServiceApi().apiKeyValue())
                      .baseUrl(caServiceCertsApiBaseUrl)
                      .defaultStatusHandler(new RelyingPartyCertificateServiceResponseErrorHandler())
                      .requestFactory(requestFactory)
                      .build();
    }
}
