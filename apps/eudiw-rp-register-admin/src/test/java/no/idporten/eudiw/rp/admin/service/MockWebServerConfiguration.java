package no.idporten.eudiw.rp.admin.service;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import java.io.IOException;

@TestConfiguration
public class MockWebServerConfiguration {
    @Bean(destroyMethod = "shutdown")
    public MockWebServer testMockWebServer() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        return mockWebServer;
    }

    @Bean
    public DynamicPropertyRegistrar mockWebServerProperties(MockWebServer testMockWebServer) {
        return registry -> registry.add("TEST_MOCK_WEB_SERVER_PORT", testMockWebServer::getPort);
    }
}
