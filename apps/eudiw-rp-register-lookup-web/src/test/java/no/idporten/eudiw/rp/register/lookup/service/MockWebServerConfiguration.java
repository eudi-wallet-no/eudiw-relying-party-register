package no.idporten.eudiw.rp.register.lookup.service;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@TestConfiguration
public class MockWebServerConfiguration {
    @Bean
    public MockWebServer testMockWebServer(
        @Value("${TEST_MOCK_WEB_SERVER_PORT}") int mockWebServerPort)
        throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start(mockWebServerPort);
        return mockWebServer;
    }
}
