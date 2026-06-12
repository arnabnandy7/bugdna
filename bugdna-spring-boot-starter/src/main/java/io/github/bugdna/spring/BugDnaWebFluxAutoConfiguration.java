package io.github.bugdna.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebExceptionHandler;

/**
 * WebFlux auto-configuration for automatic bugdna exception logging.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.web.server.WebExceptionHandler")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "bugdna", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BugDnaWebFluxAutoConfiguration {

    /**
     * Creates bugdna WebFlux auto-configuration.
     */
    public BugDnaWebFluxAutoConfiguration() {
        // Default constructor is empty
    }

    /**
     * Logs unhandled WebFlux exceptions and re-emits them for normal handling.
     *
     * @param service bugdna service
     * @param properties bugdna starter properties
     * @return reactive exception handler
     */
    @Bean
    @ConditionalOnMissingBean(name = "bugDnaWebFluxExceptionLogger")
    @ConditionalOnProperty(prefix = "bugdna", name = "log-enabled", havingValue = "true", matchIfMissing = true)
    public WebExceptionHandler bugDnaWebFluxExceptionLogger(
            BugDnaSpringService service,
            BugDnaProperties properties
    ) {
        return new BugDnaWebFluxExceptionLogger(service, properties);
    }
}
