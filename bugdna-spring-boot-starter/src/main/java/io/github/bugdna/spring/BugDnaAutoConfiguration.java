package io.github.bugdna.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     * @return bugdna service
     */
    @Bean
    @ConditionalOnMissingBean
    public BugDnaSpringService bugDnaSpringService(BugDnaFingerprintRepository repository) {
        return new BugDnaSpringService(repository);
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

}
