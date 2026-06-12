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
        if (args == null || args.length == 0) {
            printUsage(error);
            return USAGE_ERROR;
        }

        if ("analyze".equals(args[0]) && args.length == 2) {
            return analyze(args[1], out, error);
        }
        if ("compare".equals(args[0]) && args.length == 3) {
            return compare(null, args[1], null, args[2], out, error);
        }
        if ("compare".equals(args[0]) && args.length == 5) {
            return compare(args[1], args[2], args[3], args[4], out, error);
        }

        printUsage(error);
        return USAGE_ERROR;
    }

    private static int analyze(String path, PrintStream out, PrintStream error) {
        Path logFile = parsePath(path, error);
        if (logFile == null) {
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

    private static int compare(
            String oldVersion,
            String oldPath,
            String newVersion,
            String newPath,
            PrintStream out,
            PrintStream error
    ) {
        Path oldLogFile = parsePath(oldPath, error);
        Path newLogFile = parsePath(newPath, error);
        if (oldLogFile == null || newLogFile == null) {
            return FILE_ERROR;
        }

        LogFileAnalyzer analyzer = new LogFileAnalyzer();
        try {
            LogComparison comparison = new LogComparator().compare(
                    oldVersion,
                    analyzer.analyze(oldLogFile),
                    newVersion,
                    analyzer.analyze(newLogFile)
            );
            out.println(comparison.report());
            return SUCCESS;
        } catch (IOException exception) {
            error.println("Unable to read one or more log files: "
                    + oldLogFile
                    + ", "
                    + newLogFile);
            return FILE_ERROR;
        }
    }

    private static Path parsePath(String path, PrintStream error) {
        try {
            return Paths.get(path);
        } catch (InvalidPathException exception) {
            error.println("Invalid log file path: " + path);
            return null;
        }
    }

    private static void printUsage(PrintStream error) {
        error.println("Usage:");
        error.println("  bugdna analyze <log-file>");
        error.println("  bugdna compare <old-log-file> <new-log-file>");
        error.println(
                "  bugdna compare <old-version> <old-log-file> <new-version> <new-log-file>"
        );
    }
}
