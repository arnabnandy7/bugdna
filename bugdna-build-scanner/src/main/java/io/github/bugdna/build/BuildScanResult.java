package io.github.bugdna.build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable result of a build-time validation scan.
 */
public final class BuildScanResult {

    private final List<BuildScanIssue> issues;

    BuildScanResult(List<BuildScanIssue> issues) {
        this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
    }

    /**
     * Returns all validation issues found during the scan.
     *
     * @return immutable issue list
     */
    public List<BuildScanIssue> getIssues() {
        return issues;
    }

    /**
     * Returns whether the scan found at least one issue.
     *
     * @return {@code true} when issues exist
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * Counts issues for one validation rule.
     *
     * @param rule rule to count
     * @return number of matching issues
     */
    public int count(BuildScanRule rule) {
        int count = 0;
        for (BuildScanIssue issue : issues) {
            if (issue.getRule() == rule) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts issues for every supported validation rule.
     *
     * @return immutable count map keyed by rule
     */
    public Map<BuildScanRule, Integer> countsByRule() {
        Map<BuildScanRule, Integer> counts = new EnumMap<>(BuildScanRule.class);
        for (BuildScanRule rule : BuildScanRule.values()) {
            counts.put(rule, Integer.valueOf(count(rule)));
        }
        return Collections.unmodifiableMap(counts);
    }
}
