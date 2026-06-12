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
        assertTrue(error.toString().contains("Usage: bugdna analyze <log-file>"));

        error.reset();
        int fileExitCode = BugDnaCli.run(
                new String[] {"analyze", temporaryDirectory.resolve("missing.log").toString()},
                new PrintStream(output),
                new PrintStream(error)
        );
        assertEquals(3, fileExitCode);
        assertTrue(error.toString().contains("Unable to read log file"));
    }
}
