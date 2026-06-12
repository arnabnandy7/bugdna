package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
        log(fingerprint, failure);
        return Mono.error(failure);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void log(Fingerprint fingerprint, Throwable failure) {
        if (!LOGGER.isErrorEnabled()) {
            return;
        }
        if (properties.isMdcEnabled()) {
            putMdc(fingerprint);
        }
        try {
            if (properties.isIncludeStackTrace()) {
                LOGGER.error("[{}] Unhandled exception fingerprinted by bugdna",
                        fingerprint.getId(),
                        failure);
            } else {
                LOGGER.error("[{}] Unhandled exception fingerprinted by bugdna",
                        fingerprint.getId());
            }
        } finally {
            if (properties.isMdcEnabled()) {
                clearMdc();
            }
        }
    }

    private static void putMdc(Fingerprint fingerprint) {
        MDC.put("bugdna", fingerprint.getId());
        MDC.put("bugdna.id", fingerprint.getId());
        MDC.put("bugdna.confidence", String.valueOf(fingerprint.getStabilityScore()));
        MDC.put("bugdna.category", fingerprint.getCategory().name());
        MDC.put("bugdna.priority", fingerprint.getPriority().name());
    }

    private static void clearMdc() {
        MDC.remove("bugdna");
        MDC.remove("bugdna.id");
        MDC.remove("bugdna.confidence");
        MDC.remove("bugdna.category");
        MDC.remove("bugdna.priority");
    }
}
