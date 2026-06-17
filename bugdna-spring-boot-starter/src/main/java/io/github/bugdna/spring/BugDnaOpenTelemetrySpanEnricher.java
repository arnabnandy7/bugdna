package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;
import io.opentelemetry.api.trace.Span;

/**
 * Adds bugdna fingerprint attributes to the current OpenTelemetry span.
 */
final class BugDnaOpenTelemetrySpanEnricher implements BugDnaSpanEnricher {

    @Override
    public void enrich(Fingerprint fingerprint) {
        Span span = Span.current();
        if (!span.getSpanContext().isValid()) {
            return;
        }
        span.setAttribute("bugdna", fingerprint.getId());
        span.setAttribute("bugdna.id", fingerprint.getId());
        span.setAttribute("bugdna.confidence", fingerprint.getStabilityScore());
        span.setAttribute("bugdna.category", fingerprint.getCategory().name());
        span.setAttribute("bugdna.family", fingerprint.getFamily().name());
        span.setAttribute("bugdna.priority", fingerprint.getPriority().name());
    }
}
