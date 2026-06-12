package io.github.bugdna.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and counts BugDNA fingerprint IDs in text log files.
 */
public final class LogFileAnalyzer {

    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile(
            "\\bBUGDNA-([0-9A-F]+)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Analyzes all BugDNA fingerprint occurrences in a UTF-8 log file.
     *
     * @param logFile log file to analyze
     * @return immutable analysis
     * @throws IOException when the file cannot be read
     */
    public LogAnalysis analyze(Path logFile) throws IOException {
        Path requiredLogFile = Objects.requireNonNull(logFile, "logFile must not be null");
        Map<String, Long> occurrences = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(
                requiredLogFile,
                StandardCharsets.UTF_8
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                countFingerprints(line, occurrences);
            }
        }

        return new LogAnalysis(occurrences);
    }

    private static void countFingerprints(String line, Map<String, Long> occurrences) {
        Matcher matcher = FINGERPRINT_PATTERN.matcher(line);
        while (matcher.find()) {
            String id = matcher.group().toUpperCase(Locale.ROOT);
            Long current = occurrences.get(id);
            occurrences.put(id, current == null ? 1L : current + 1L);
        }
    }
}
