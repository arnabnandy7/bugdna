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

    /**
     * Creates a source validation finding.
     *
     * @param rule validation rule that produced the issue
     * @param severity issue severity
     * @param file source file path
     * @param line source line number
     * @param message human-readable issue message
     * @param snippet source snippet associated with the issue, or {@code null}
     * @throws NullPointerException when required arguments are {@code null}
     */
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

    /**
     * Returns the validation rule that produced this issue.
     *
     * @return scan rule
     */
    public BuildScanRule getRule() {
        return rule;
    }

    /**
     * Returns the issue severity.
     *
     * @return severity
     */
    public BuildScanSeverity getSeverity() {
        return severity;
    }

    /**
     * Returns the source file containing the issue.
     *
     * @return source file path
     */
    public Path getFile() {
        return file;
    }

    /**
     * Returns the source line number containing the issue.
     *
     * @return line number
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the human-readable issue message.
     *
     * @return issue message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the source snippet associated with the issue.
     *
     * @return source snippet, or an empty string when none was supplied
     */
    public String getSnippet() {
        return snippet;
    }

    @Override
    public String toString() {
        return file + ":" + line + " [" + severity + "] " + rule + " - " + message;
    }
}
