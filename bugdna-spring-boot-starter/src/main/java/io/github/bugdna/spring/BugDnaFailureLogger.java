package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;
import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Shared fingerprint logging and MDC lifecycle for Spring web integrations.
 */
final class BugDnaFailureLogger {

    private static final String MESSAGE =
            "[{}] Unhandled exception fingerprinted by bugdna";
    private static final String[] MDC_KEYS = {
            "bugdna",
            "bugdna.id",
            "bugdna.confidence",
            "bugdna.category",
            "bugdna.priority"
    };

    private BugDnaFailureLogger() {
    }

    static void log(
            Logger logger,
            BugDnaProperties properties,
            Fingerprint fingerprint,
            Throwable failure
    ) {
        if (!logger.isErrorEnabled()) {
            return;
        }
        if (properties.isMdcEnabled()) {
            putMdc(fingerprint);
        }
        try {
            if (properties.isIncludeStackTrace()) {
                logger.error(MESSAGE, fingerprint.getId(), failure);
            } else {
                logger.error(MESSAGE, fingerprint.getId());
            }
        } finally {
            if (properties.isMdcEnabled()) {
                clearMdc();
            }
        }
    }

    private static void putMdc(Fingerprint fingerprint) {
        MDC.put(MDC_KEYS[0], fingerprint.getId());
        MDC.put(MDC_KEYS[1], fingerprint.getId());
        MDC.put(MDC_KEYS[2], String.valueOf(fingerprint.getStabilityScore()));
        MDC.put(MDC_KEYS[3], fingerprint.getCategory().name());
        MDC.put(MDC_KEYS[4], fingerprint.getPriority().name());
    }

    private static void clearMdc() {
        for (String key : MDC_KEYS) {
            MDC.remove(key);
        }
    }
}
