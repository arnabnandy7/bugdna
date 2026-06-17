package io.github.bugdna.build;

import java.nio.file.Path;
import java.util.Objects;

/**
 * One source validation finding.
 */
public final class BuildScanIssue {

    private final BuildScanRule rule;
    private final BuildScanSeverity severity;
    private final Path file;
    private final int line;
    private final String message;
    private final String snippet;

    public BuildScanIssue(
            BuildScanRule rule,
            BuildScanSeverity severity,
            Path file,
            int line,
            String message,
            String snippet
    ) {
        this.rule = Objects.requireNonNull(rule, "rule must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.file = Objects.requireNonNull(file, "file must not be null");
        this.line = line;
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.snippet = snippet == null ? "" : snippet;
    }

    public BuildScanRule getRule() {
        return rule;
    }

    public BuildScanSeverity getSeverity() {
        return severity;
    }

    public Path getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }

    public String getSnippet() {
        return snippet;
    }

    @Override
    public String toString() {
        return file + ":" + line + " [" + severity + "] " + rule + " - " + message;
    }
}
