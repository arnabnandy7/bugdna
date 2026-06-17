package io.github.bugdna.build;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scans Java source files for exception-handling hazards related to production failures.
 */
public final class BugDnaBuildScanner {

    /**
     * Creates a source scanner.
     */
    public BugDnaBuildScanner() {
        // Stateless scanner; no constructor initialization is required.
    }

    /**
     * Scans configured source roots for build validation issues.
     *
     * @param config scan source roots and options
     * @return immutable scan result
     * @throws IOException when a source root cannot be walked or read
     * @throws NullPointerException when {@code config} is {@code null}
     */
    public BuildScanResult scan(BuildScanConfig config) throws IOException {
        Objects.requireNonNull(config, "config must not be null");
        final List<BuildScanIssue> issues = new ArrayList<>();

        for (Path sourceRoot : config.getSourceRoots()) {
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            Files.walkFileTree(sourceRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().endsWith(".java")) {
                        issues.addAll(new JavaSourceScanner(file).scan());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return new BuildScanResult(issues);
    }
}
