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

/**
 * Finds and counts BugDNA fingerprint IDs in text log files.
 */
public final class LogFileAnalyzer {

    private static final String FINGERPRINT_PREFIX = "BUGDNA-";

    /**
     * Creates a log file analyzer.
     */
    public LogFileAnalyzer() {
    }

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
        int searchFrom = 0;
        int start = fingerprintStart(line, searchFrom);
        while (start >= 0) {
            int end = fingerprintEnd(line, start + FINGERPRINT_PREFIX.length());
            String id = line.substring(start, end).toUpperCase(Locale.ROOT);
            Long current = occurrences.get(id);
            occurrences.put(id, current == null ? 1L : current + 1L);
            searchFrom = end;
            start = fingerprintStart(line, searchFrom);
        }
    }

    private static int fingerprintStart(String line, int searchFrom) {
        int start = indexOfIgnoreCase(line, FINGERPRINT_PREFIX, searchFrom);
        while (start >= 0) {
            int valueStart = start + FINGERPRINT_PREFIX.length();
            if (isBoundary(line, start - 1)
                    && valueStart < line.length()
                    && isHex(line.charAt(valueStart))) {
                return start;
            }
            start = indexOfIgnoreCase(line, FINGERPRINT_PREFIX, start + FINGERPRINT_PREFIX.length());
        }
        return -1;
    }

    private static int fingerprintEnd(String line, int valueStart) {
        int cursor = valueStart;
        while (cursor < line.length() && isHex(line.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static int indexOfIgnoreCase(String line, String value, int searchFrom) {
        int limit = line.length() - value.length();
        for (int index = Math.max(0, searchFrom); index <= limit; index++) {
            if (line.regionMatches(true, index, value, 0, value.length())) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isBoundary(String line, int index) {
        return index < 0 || !isWordCharacter(line.charAt(index));
    }

    private static boolean isWordCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    private static boolean isHex(char value) {
        return (value >= '0' && value <= '9')
                || (value >= 'A' && value <= 'F')
                || (value >= 'a' && value <= 'f');
    }
}
