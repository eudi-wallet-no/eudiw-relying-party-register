package no.idporten.eudiw.credential.registry.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@EnableScheduling
@Configuration
public class CredentialRegisterConfiguration {

    private final ConfigProperties configProperties;


    public CredentialRegisterConfiguration(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    @Bean
    public RestClient restClientRpService(RestClient.Builder clientBuilder) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(configProperties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(configProperties.readTimeout());
        return clientBuilder
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(configProperties.apiKeyHeaderId(), configProperties.apiKeyValue())
                .baseUrl(configProperties.rpRegisterServiceUrl())
                .build();
    }

    @Bean
    public RestClient restClientExternalApi(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(configProperties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(configProperties.readTimeout());
        return restClientBuilder
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
