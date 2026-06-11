package io.github.bugdna.spring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer auto-configuration for bugdna failure metrics.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
@ConditionalOnBean(type = "io.micrometer.core.instrument.MeterRegistry")
@ConditionalOnProperty(prefix = "bugdna", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BugDnaMetricsAutoConfiguration {

    /**
     * Creates bugdna Micrometer metrics.
     */
    public BugDnaMetricsAutoConfiguration() {
    }

    /**
     * Registers total and unique failure gauges.
     *
     * @param registry Micrometer registry
     * @param repository fingerprint repository
     * @return registered bugdna metrics
     */
    @Bean
    @ConditionalOnMissingBean
    public BugDnaMetrics bugDnaMetrics(
            MeterRegistry registry,
            BugDnaFingerprintRepository repository
    ) {
        return new BugDnaMetrics(registry, repository);
    }

    /**
     * Holder for registered bugdna meters.
     */
    public static final class BugDnaMetrics {

        private BugDnaMetrics(MeterRegistry registry, BugDnaFingerprintRepository repository) {
            Gauge.builder("bugdna.failures.total", repository, BugDnaFingerprintRepository::totalCount)
                    .description("Total number of failures fingerprinted by bugdna")
                    .register(registry);
            Gauge.builder("bugdna.unique.failures", repository, BugDnaFingerprintRepository::uniqueCount)
                    .description("Number of unique failure fingerprints observed by bugdna")
                    .register(registry);
        }
    }
}
