package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * MVC exception resolver that logs bugdna fingerprints and lets Spring continue handling the error.
 */
public class BugDnaExceptionLogger implements HandlerExceptionResolver, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(BugDnaExceptionLogger.class);

    private final BugDnaSpringService service;
    private final BugDnaProperties properties;

    BugDnaExceptionLogger(BugDnaSpringService service, BugDnaProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        Fingerprint fingerprint = service.fingerprint(exception);
        log(fingerprint, exception);
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void log(Fingerprint fingerprint, Exception exception) {
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
                        exception);
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
