package no.eudiw.rp.register.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryUriStartupTest {

    @Configuration
    @EnableConfigurationProperties(RegistryProperties.class)
    static class TestConfig { }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

    @Test
    void startupFailsWhenRegistryUriIsEmpty() {
        runner.withPropertyValues("eudiw-rp-register-service.registry-uri=")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("registry-uri (REGISTRY_URI) must be configured");
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {"/wrp", "https://registry.example.test/w rp"})
    void startupFailsWhenRegistryUriIsNotAnAbsoluteUrl(String registryUri) {
        runner.withPropertyValues("eudiw-rp-register-service.registry-uri=" + registryUri)
            .run(context -> assertThat(context).hasFailed());
    }
}