package io.github.bugdna.build;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Source roots and options for a BugDNA build validation scan.
 */
public final class BuildScanConfig {

    private final List<Path> sourceRoots;
    private final boolean includeTests;

    private BuildScanConfig(Builder builder) {
        this.sourceRoots = Collections.unmodifiableList(new ArrayList<>(builder.sourceRoots));
        this.includeTests = builder.includeTests;
    }

    public List<Path> getSourceRoots() {
        return sourceRoots;
    }

    public boolean isIncludeTests() {
        return includeTests;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<Path> sourceRoots = new ArrayList<>();
        private boolean includeTests;

        private Builder() {
        }

        public Builder addSourceRoot(Path sourceRoot) {
            sourceRoots.add(Objects.requireNonNull(sourceRoot, "sourceRoot must not be null"));
            return this;
        }

        public Builder addSourceRoots(Iterable<Path> sourceRoots) {
            Objects.requireNonNull(sourceRoots, "sourceRoots must not be null");
            for (Path sourceRoot : sourceRoots) {
                addSourceRoot(sourceRoot);
            }
            return this;
        }

        public Builder includeTests(boolean includeTests) {
            this.includeTests = includeTests;
            return this;
        }

        public BuildScanConfig build() {
            return new BuildScanConfig(this);
        }
    }
}
