package io.github.bugdna.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogFileAnalyzerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void countsAndSortsFingerprintOccurrences() throws IOException {
        Path logFile = writeLog(
                "ERROR [BUGDNA-002] second",
                "ERROR [BUGDNA-001] first BUGDNA-001",
                "INFO ignored",
                "ERROR [bugdna-002] second",
                "ERROR [BUGDNA-001] first"
        );

        LogAnalysis analysis = new LogFileAnalyzer().analyze(logFile);

        assertEquals(2, analysis.getUniqueFailures());
        assertEquals("BUGDNA-001", analysis.getFailures().get(0).getId());
        assertEquals(3, analysis.getFailures().get(0).getOccurrences());
        assertEquals("BUGDNA-002", analysis.getFailures().get(1).getId());
        assertEquals(2, analysis.getFailures().get(1).getOccurrences());
        assertEquals(
                "Unique Failures: 2"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "BUGDNA-001 : 3"
                        + System.lineSeparator()
                        + "BUGDNA-002 : 2",
                analysis.report()
        );
    }

    @Test
    void returnsAnEmptyAnalysisWhenNoFingerprintsExist() throws IOException {
        LogAnalysis analysis = new LogFileAnalyzer().analyze(writeLog("INFO healthy"));

        assertEquals(0, analysis.getUniqueFailures());
        assertEquals("Unique Failures: 0", analysis.report());
    }

    @Test
    void rejectsNullAndMissingFiles() {
        assertThrows(
                NullPointerException.class,
                () -> new LogFileAnalyzer().analyze(null)
        );
        assertThrows(
                IOException.class,
                () -> new LogFileAnalyzer().analyze(temporaryDirectory.resolve("missing.log"))
        );
    }

    private Path writeLog(String... lines) throws IOException {
        Path logFile = temporaryDirectory.resolve("app.log");
        Files.write(logFile, java.util.Arrays.asList(lines), StandardCharsets.UTF_8);
        return logFile;
    }
}
