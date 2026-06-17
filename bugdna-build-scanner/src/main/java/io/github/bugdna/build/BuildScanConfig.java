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

    /**
     * Returns source roots scanned for Java files.
     *
     * @return immutable source-root paths
     */
    public List<Path> getSourceRoots() {
        return sourceRoots;
    }

    /**
     * Returns whether test source roots should be included by callers.
     *
     * @return {@code true} when tests are included
     */
    public boolean isIncludeTests() {
        return includeTests;
    }

    /**
     * Creates a mutable builder for scan configuration.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BuildScanConfig}.
     */
    public static final class Builder {

        private final List<Path> sourceRoots = new ArrayList<>();
        private boolean includeTests;

        private Builder() {
        }

        /**
         * Adds one source root to scan.
         *
         * @param sourceRoot source root path
         * @return this builder
         * @throws NullPointerException when {@code sourceRoot} is {@code null}
         */
        public Builder addSourceRoot(Path sourceRoot) {
            sourceRoots.add(Objects.requireNonNull(sourceRoot, "sourceRoot must not be null"));
            return this;
        }

        /**
         * Adds multiple source roots to scan.
         *
         * @param sourceRoots source root paths
         * @return this builder
         * @throws NullPointerException when {@code sourceRoots} or an entry is {@code null}
         */
        public Builder addSourceRoots(Iterable<Path> sourceRoots) {
            Objects.requireNonNull(sourceRoots, "sourceRoots must not be null");
            for (Path sourceRoot : sourceRoots) {
                addSourceRoot(sourceRoot);
            }
            return this;
        }

        /**
         * Sets whether test sources are included by the caller.
         *
         * @param includeTests {@code true} to include test source roots
         * @return this builder
         */
        public Builder includeTests(boolean includeTests) {
            this.includeTests = includeTests;
            return this;
        }

        /**
         * Builds an immutable scan configuration.
         *
         * @return scan configuration
         */
        public BuildScanConfig build() {
            return new BuildScanConfig(this);
        }
    }
}
