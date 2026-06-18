package io.github.bugdna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable dependency graph for fingerprints found in one causal chain.
 */
public final class FailureDependencyGraph {

    private final List<Fingerprint> fingerprints;

    FailureDependencyGraph(List<Fingerprint> fingerprints) {
        Objects.requireNonNull(fingerprints, "fingerprints must not be null");
        if (fingerprints.isEmpty()) {
            throw new IllegalArgumentException("fingerprints must not be empty");
        }
        this.fingerprints = Collections.unmodifiableList(new ArrayList<>(fingerprints));
    }

    /**
     * Returns the outermost failure fingerprint.
     *
     * @return root fingerprint
     */
    public Fingerprint getRoot() {
        return fingerprints.get(0);
    }

    /**
     * Returns all fingerprints in causal order from outer failure to deepest cause.
     *
     * @return immutable fingerprint list
     */
    public List<Fingerprint> getFingerprints() {
        return fingerprints;
    }

    /**
     * Returns fingerprints that were caused by the root fingerprint.
     *
     * @return immutable dependency list
     */
    public List<Fingerprint> getDependencies() {
        return Collections.unmodifiableList(fingerprints.subList(1, fingerprints.size()));
    }

    /**
     * Returns the number of fingerprints in the graph.
     *
     * @return graph depth
     */
    public int getDepth() {
        return fingerprints.size();
    }

    /**
     * Formats the causal dependency graph as a compact tree.
     *
     * @return dependency graph report
     */
    public String report() {
        StringBuilder report = new StringBuilder();
        for (int i = 0; i < fingerprints.size(); i++) {
            if (i > 0) {
                report.append(System.lineSeparator())
                        .append(indent(i))
                        .append("└─ ");
            }
            report.append(fingerprints.get(i).getId());
        }
        return report.toString();
    }

    @Override
    public String toString() {
        return "FailureDependencyGraph{"
                + "depth=" + fingerprints.size()
                + ", root='" + getRoot().getId() + '\''
                + '}';
    }

    private static String indent(int depth) {
        StringBuilder indent = new StringBuilder(" ");
        for (int i = 1; i < depth; i++) {
            indent.append("     ");
        }
        return indent.toString();
    }
}
