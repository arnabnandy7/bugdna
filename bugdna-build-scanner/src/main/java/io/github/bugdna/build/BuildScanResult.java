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

    public List<BuildScanIssue> getIssues() {
        return issues;
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    public int count(BuildScanRule rule) {
        int count = 0;
        for (BuildScanIssue issue : issues) {
            if (issue.getRule() == rule) {
                count++;
            }
        }
        return count;
    }

    public Map<BuildScanRule, Integer> countsByRule() {
        Map<BuildScanRule, Integer> counts = new EnumMap<>(BuildScanRule.class);
        for (BuildScanRule rule : BuildScanRule.values()) {
            counts.put(rule, Integer.valueOf(count(rule)));
        }
        return Collections.unmodifiableMap(counts);
    }
}
