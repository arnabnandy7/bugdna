package io.github.bugdna.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * BugDNA command-line entry point.
 */
public final class BugDnaCli {

    private static final int SUCCESS = 0;
    private static final int USAGE_ERROR = 2;
    private static final int FILE_ERROR = 3;

    private BugDnaCli() {
    }

    /**
     * Runs the BugDNA CLI.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != SUCCESS) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream error) {
        if (args == null || args.length != 2 || !"analyze".equals(args[0])) {
            error.println("Usage: bugdna analyze <log-file>");
            return USAGE_ERROR;
        }

        Path logFile;
        try {
            logFile = Paths.get(args[1]);
        } catch (InvalidPathException exception) {
            error.println("Invalid log file path: " + args[1]);
            return FILE_ERROR;
        }

        try {
            LogAnalysis analysis = new LogFileAnalyzer().analyze(logFile);
            out.println(analysis.report());
            return SUCCESS;
        } catch (IOException exception) {
            error.println("Unable to read log file: " + logFile);
            return FILE_ERROR;
        }
    }
}
