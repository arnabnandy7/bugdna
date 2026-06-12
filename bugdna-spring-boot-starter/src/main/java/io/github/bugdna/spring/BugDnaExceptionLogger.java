package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        BugDnaFailureLogger.log(LOGGER, properties, fingerprint, exception);
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
