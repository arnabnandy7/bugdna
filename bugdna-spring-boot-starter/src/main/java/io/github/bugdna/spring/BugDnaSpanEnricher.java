package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;

/**
 * Enriches the active observability span with a generated bugdna fingerprint.
 */
public interface BugDnaSpanEnricher {

    /**
     * Adds fingerprint fields to the current span or equivalent observability context.
     *
     * @param fingerprint generated fingerprint
     */
    void enrich(Fingerprint fingerprint);
}
