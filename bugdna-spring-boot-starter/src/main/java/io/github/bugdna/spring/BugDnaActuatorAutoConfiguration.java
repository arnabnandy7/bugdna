package io.github.bugdna.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator auto-configuration for recent bugdna fingerprints.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
@ConditionalOnProperty(prefix = "bugdna", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BugDnaActuatorAutoConfiguration {

    /**
     * Creates bugdna actuator auto-configuration.
     */
    public BugDnaActuatorAutoConfiguration() {
    }

    /**
     * Exposes recent fingerprints through Spring Boot Actuator when Actuator is present.
     *
     * @param repository recent fingerprint repository
     * @return actuator endpoint
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "bugdna.actuator", name = "enabled", havingValue = "true", matchIfMissing = true)
    public BugDnaEndpoint bugDnaEndpoint(BugDnaFingerprintRepository repository) {
        return new BugDnaEndpoint(repository);
    }
}
