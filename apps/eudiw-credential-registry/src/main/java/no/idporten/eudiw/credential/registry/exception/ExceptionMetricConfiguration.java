package no.idporten.eudiw.credential.registry.exception;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.idporten.metric.constants.MetricCategories;
import no.idporten.metric.constants.MetricDescriptions;
import no.idporten.metric.constants.MetricNames;
import no.idporten.metric.constants.MetricValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExceptionMetricConfiguration {

    @Bean(name = "connectExternalApiExceptionCounter")
    public Counter connectExternalApiExceptionCounter(MeterRegistry meterRegistry) {
        return Counter
                .builder(MetricNames.APP_EXCEPTION_NAME)
                .tag(MetricCategories.EXTERNAL_SYSTEM, MetricValues.EXTERNAL_SYSTEM_EXTERNAL_API)
                .tag(MetricCategories.EXCEPTION_TYPE, MetricValues.EXCEPTION_TYPE_CONNECT)
                .description(MetricDescriptions.APP_EXCEPTION_EXTERNAL_API_DESCRIPTION)
                .register(meterRegistry);
    }

    @Bean(name = "connectInternalApiExceptionCounter")
    public Counter connectInternalApiExceptionCounter(MeterRegistry meterRegistry) {
        return Counter
                .builder(MetricNames.APP_EXCEPTION_NAME)
                .tag(MetricCategories.EXTERNAL_SYSTEM, MetricValues.EXTERNAL_SYSTEM_INTERNAL_API)
                .tag(MetricCategories.EXCEPTION_TYPE, MetricValues.EXCEPTION_TYPE_CONNECT)
                .description(MetricDescriptions.APP_EXCEPTION_INTERNAL_API_DESCRIPTION)
                .register(meterRegistry);
    }
}
