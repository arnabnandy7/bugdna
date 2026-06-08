package io.github.bugdna.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Web auto-configuration for automatic bugdna exception logging.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.web.servlet.HandlerExceptionResolver")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "bugdna", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BugDnaWebAutoConfiguration {

    /**
     * Creates bugdna web auto-configuration.
     */
    public BugDnaWebAutoConfiguration() {
    }

    /**
     * Logs unhandled MVC exceptions without replacing Spring's exception handling.
     *
     * @param service bugdna service
     * @param properties bugdna starter properties
     * @return exception resolver
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "bugdna", name = "log-enabled", havingValue = "true", matchIfMissing = true)
    public HandlerExceptionResolver bugDnaExceptionLogger(
            BugDnaSpringService service,
            BugDnaProperties properties
    ) {
        return new BugDnaExceptionLogger(service, properties);
    }
}
