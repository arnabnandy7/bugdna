package io.github.bugdna.spring;

import io.github.bugdna.FailureTracker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring Boot auto-configuration for bugdna.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BugDnaProperties.class)
@ConditionalOnProperty(prefix = "bugdna", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BugDnaAutoConfiguration {

    /**
     * Creates bugdna core auto-configuration.
     */
    public BugDnaAutoConfiguration() {
      // Default constructor is empty
    }

    /**
     * Creates the application-facing bugdna service.
     *
     * @param repository recent fingerprint repository
     * @param tracker in-memory failure aggregator
     * @return bugdna service
     */
    @Bean
    @ConditionalOnMissingBean
    public BugDnaSpringService bugDnaSpringService(
            BugDnaFingerprintRepository repository,
            FailureTracker tracker,
            List<BugDnaSpanEnricher> spanEnrichers
    ) {
        return new BugDnaSpringService(repository, tracker, spanEnrichers);
    }

    /**
     * Aggregates recurring failures in memory.
     *
     * @return failure tracker
     */
    @Bean
    @ConditionalOnMissingBean
    public FailureTracker bugDnaFailureTracker() {
        return new FailureTracker();
    }

    /**
     * Stores recent fingerprints for optional diagnostics.
     *
     * @param properties bugdna starter properties
     * @return fingerprint repository
     */
    @Bean
    @ConditionalOnMissingBean
    public BugDnaFingerprintRepository bugDnaFingerprintRepository(BugDnaProperties properties) {
        return new BugDnaFingerprintRepository(properties.getRecentLimit());
    }

    /**
     * Adds bugdna fields to the current OpenTelemetry span when one is active.
     *
     * @return OpenTelemetry span enricher
     */
    @Bean
    @ConditionalOnClass(name = "io.opentelemetry.api.trace.Span")
    @ConditionalOnMissingBean(BugDnaSpanEnricher.class)
    @ConditionalOnProperty(prefix = "bugdna", name = "otel-enabled", havingValue = "true", matchIfMissing = true)
    public BugDnaSpanEnricher bugDnaOpenTelemetrySpanEnricher() {
        return new BugDnaOpenTelemetrySpanEnricher();
    }

}
