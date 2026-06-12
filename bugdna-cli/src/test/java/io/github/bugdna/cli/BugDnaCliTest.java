package io.github.bugdna.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BugDnaCliTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void analyzesALogFileFromTheCommandLine() throws IOException {
        Path logFile = temporaryDirectory.resolve("app.log");
        Files.write(
                logFile,
                Arrays.asList("BUGDNA-001", "BUGDNA-001", "BUGDNA-002"),
                StandardCharsets.UTF_8
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = BugDnaCli.run(
                new String[] {"analyze", logFile.toString()},
                new PrintStream(output),
                new PrintStream(error)
        );

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("Unique Failures: 2"));
        assertTrue(output.toString().contains("BUGDNA-001 : 2"));
        assertEquals("", error.toString());
    }

    @Test
    void reportsUsageAndFileErrors() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int usageExitCode = BugDnaCli.run(
                new String[] {"unknown"},
                new PrintStream(output),
                new PrintStream(error)
        );
        assertEquals(2, usageExitCode);
        assertTrue(error.toString().contains("bugdna analyze <log-file>"));
        assertTrue(error.toString().contains(
                "bugdna compare <old-log-file> <new-log-file>"
        ));

        error.reset();
        int fileExitCode = BugDnaCli.run(
                new String[] {"analyze", temporaryDirectory.resolve("missing.log").toString()},
                new PrintStream(output),
                new PrintStream(error)
        );
        assertEquals(3, fileExitCode);
        assertTrue(error.toString().contains("Unable to read log file"));
    }

    @Test
    void comparesTwoLogFilesFromTheCommandLine() throws IOException {
        Path oldLog = temporaryDirectory.resolve("app-v1.log");
        Path newLog = temporaryDirectory.resolve("app-v2.log");
        Files.write(
                oldLog,
                Arrays.asList("BUGDNA-001", "BUGDNA-002", "BUGDNA-003"),
                StandardCharsets.UTF_8
        );
        Files.write(
                newLog,
                Arrays.asList("BUGDNA-002", "BUGDNA-004"),
                StandardCharsets.UTF_8
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = BugDnaCli.run(
                new String[] {"compare", oldLog.toString(), newLog.toString()},
                new PrintStream(output),
                new PrintStream(error)
        );

        assertEquals(0, exitCode);
        assertEquals(
                "New fingerprints: 1"
                        + System.lineSeparator()
                        + "Resolved fingerprints: 2"
                        + System.lineSeparator()
                        + "Recurring fingerprints: 1"
                        + System.lineSeparator(),
                output.toString()
        );
        assertEquals("", error.toString());
    }

    @Test
    void comparesVersionLabelledDeployments() throws IOException {
        Path oldLog = temporaryDirectory.resolve("app-1.2.0.log");
        Path newLog = temporaryDirectory.resolve("app-1.3.0.log");
        Files.write(
                oldLog,
                Arrays.asList("BUGDNA-001", "BUGDNA-002"),
                StandardCharsets.UTF_8
        );
        Files.write(
                newLog,
                Arrays.asList("BUGDNA-002", "BUGDNA-003"),
                StandardCharsets.UTF_8
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = BugDnaCli.run(
                new String[] {
                        "compare",
                        "1.2.0",
                        oldLog.toString(),
                        "1.3.0",
                        newLog.toString()
                },
                new PrintStream(output),
                new PrintStream(error)
        );

        assertEquals(0, exitCode);
        assertTrue(output.toString().startsWith(
                "Version 1.2.0 -> Version 1.3.0"
        ));
        assertTrue(output.toString().contains("Recurring fingerprints: 1"));
        assertEquals("", error.toString());
    }
}
