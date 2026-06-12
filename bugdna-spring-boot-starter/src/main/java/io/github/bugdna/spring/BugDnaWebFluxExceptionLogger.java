package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * WebFlux exception handler that records failures and preserves reactive error handling.
 */
public class BugDnaWebFluxExceptionLogger implements WebExceptionHandler, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            BugDnaWebFluxExceptionLogger.class
    );

    private final BugDnaSpringService service;
    private final BugDnaProperties properties;

    BugDnaWebFluxExceptionLogger(
            BugDnaSpringService service,
            BugDnaProperties properties
    ) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable failure) {
        Fingerprint fingerprint = service.fingerprint(failure);
        BugDnaFailureLogger.log(LOGGER, properties, fingerprint, failure);
        return Mono.error(failure);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
