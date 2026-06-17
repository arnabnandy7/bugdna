package io.github.bugdna;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Actionable context attached to a known fingerprint ID.
 */
public final class FingerprintKnowledge {

    private final String id;
    private final Map<String, String> fields;

    FingerprintKnowledge(String id, Map<String, String> fields) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.fields = immutableCopy(fields);
    }

    /**
     * Returns the fingerprint ID this context describes.
     *
     * @return fingerprint ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display title for the fingerprint.
     *
     * @return title, or {@code null} when not supplied
     */
    public String getTitle() {
        return fields.get("title");
    }

    /**
     * Returns the owning team or person.
     *
     * @return owner, or {@code null} when not supplied
     */
    public String getOwner() {
        return fields.get("owner");
    }

    /**
     * Returns a runbook path or URL.
     *
     * @return runbook, or {@code null} when not supplied
     */
    public String getRunbook() {
        return fields.get("runbook");
    }

    /**
     * Returns an arbitrary field from the knowledge base entry.
     *
     * @param name field name
     * @return field value, or {@code null} when missing
     */
    public String get(String name) {
        return fields.get(Objects.requireNonNull(name, "name must not be null"));
    }

    /**
     * Returns all fields attached to the fingerprint.
     *
     * @return immutable field map
     */
    public Map<String, String> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return "FingerprintKnowledge{"
                + "id='" + id + '\''
                + ", fields=" + fields
                + '}';
    }

    private static Map<String, String> immutableCopy(Map<String, String> fields) {
        Map<String, String> validatedFields = Objects.requireNonNull(
                fields,
                "fields must not be null"
        );
        return Collections.unmodifiableMap(new LinkedHashMap<>(validatedFields));
    }
}
