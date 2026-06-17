package io.github.bugdna.gradle;

import java.util.ArrayList;
import java.util.List;

/**
 * Gradle configuration for BugDNA build validation.
 */
public final class BugDnaExtension {

    private boolean failOnIssues = true;
    private boolean includeTests;
    private final List<String> sourceRoots = new ArrayList<>();

    public boolean isFailOnIssues() {
        return failOnIssues;
    }

    public void setFailOnIssues(boolean failOnIssues) {
        this.failOnIssues = failOnIssues;
    }

    public boolean isIncludeTests() {
        return includeTests;
    }

    public void setIncludeTests(boolean includeTests) {
        this.includeTests = includeTests;
    }

    public List<String> getSourceRoots() {
        return sourceRoots;
    }
}
