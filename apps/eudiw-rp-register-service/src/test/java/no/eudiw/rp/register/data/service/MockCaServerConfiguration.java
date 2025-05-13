package no.eudiw.rp.register.data.service;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@TestConfiguration
public class MockCaServerConfiguration {
    @Bean
    public MockWebServer testMockCaServer(
        @Value("${TEST_MOCK_CA_SERVER_PORT}") int mockCaServerPort)
        throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start(mockCaServerPort);
        return mockWebServer;
    }
}
