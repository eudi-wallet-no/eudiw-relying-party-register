package no.eudiw.rp.register;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.*;

public class PropertiesLogger implements ApplicationListener<ApplicationPreparedEvent> {
    private static final Logger log = LoggerFactory.getLogger(PropertiesLogger.class);

    private ConfigurableEnvironment environment;
    private boolean isFirstRun = true;

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        if (isFirstRun) {
            environment = event.getApplicationContext().getEnvironment();
            printActiveProperties(environment);
        }
        isFirstRun = false;
    }

    private void printActiveProperties(ConfigurableEnvironment env) {

        System.out.println("************************* ACTIVE APP PROPERTIES ******************************");

        List<MapPropertySource> propertySources = new ArrayList<>();

        env.getPropertySources().forEach(it -> {
            if (it instanceof MapPropertySource && it.getName().contains("application")) {
                propertySources.add((MapPropertySource) it);
            }
        });

        propertySources.stream()
                .map(propertySource -> propertySource.getSource().keySet())
                .flatMap(Collection::stream)
                .filter(key -> !key.toLowerCase().contains("key") || !key.toLowerCase().contains("password"))
                .distinct()
                .sorted()
                .forEach(key -> {
                    try {
                        System.out.println(key + "=" + env.getProperty(key));
                    } catch (Exception e) {
                        log.warn("{} -> {}", key, e.getMessage());
                    }
                });

        propertySources.stream()
                .map(propertySource -> propertySource.getSource().keySet())
                .flatMap(Collection::stream)
                .filter(key -> key.toLowerCase().contains("key") || key.toLowerCase().contains("password"))
                .distinct()
                .sorted()
                .forEach(key -> {
                    try {
                        if (key != null) {
                            System.out.println(key + "= length: " + env.getProperty(key).length());
                        }
                    } catch (Exception e) {
                        log.warn("Noe gikk galt i print av sensitivt");
                    }
                });

        System.out.println("******************************************************************************");

        Flyway flyway = Flyway.configure()
                .dataSource(env.getProperty("spring.datasource.url"), env.getProperty("spring.datasource.username"), env.getProperty("spring.datasource.password"))
                .cleanDisabled(false)
                .load();

        flyway.clean();
        System.out.println("FLYWAY DONE");
    }
}