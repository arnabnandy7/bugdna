package io.github.bugdna.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BugDnaBuildScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsExceptionHandlingHazards() throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java");
        Path file = sourceRoot.resolve("example/Sample.java");
        Files.createDirectories(file.getParent());
        Files.write(
                file,
                Arrays.asList(
                        "package example;",
                        "import java.nio.file.Files;",
                        "import java.nio.file.Paths;",
                        "class Sample {",
                        "  void generic() throws Exception {",
                        "    throw new RuntimeException(\"boom\");",
                        "  }",
                        "  void emptyCatch() {",
                        "    try {",
                        "      risky();",
                        "    } catch (Exception exception) {",
                        "    }",
                        "  }",
                        "  void uncheckedIo() {",
                        "    Files.readAllBytes(Paths.get(\"missing.txt\"));",
                        "  }",
                        "  void declaredIo() throws java.io.IOException {",
                        "    Files.readAllBytes(Paths.get(\"missing.txt\"));",
                        "  }",
                        "  void risky() { }",
                        "}"
                ),
                StandardCharsets.UTF_8
        );

        BuildScanResult result = new BugDnaBuildScanner().scan(
                BuildScanConfig.builder().addSourceRoot(sourceRoot).build()
        );

        assertEquals(1, result.count(BuildScanRule.EMPTY_CATCH_BLOCK));
        assertEquals(3, result.count(BuildScanRule.GENERIC_EXCEPTION_USAGE));
        assertEquals(1, result.count(BuildScanRule.UNHANDLED_EXCEPTION));
    }

    @Test
    void ignoresHandledCheckedExceptionApis() throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java");
        Path file = sourceRoot.resolve("example/Handled.java");
        Files.createDirectories(file.getParent());
        Files.write(
                file,
                Arrays.asList(
                        "package example;",
                        "import java.nio.file.Files;",
                        "import java.nio.file.Paths;",
                        "class Handled {",
                        "  void read() {",
                        "    try {",
                        "      Files.readAllBytes(Paths.get(\"ok.txt\"));",
                        "    } catch (java.io.IOException exception) {",
                        "      throw new IllegalStateException(exception);",
                        "    }",
                        "  }",
                        "}"
                ),
                StandardCharsets.UTF_8
        );

        BuildScanResult result = new BugDnaBuildScanner().scan(
                BuildScanConfig.builder().addSourceRoot(sourceRoot).build()
        );

        assertEquals(0, result.count(BuildScanRule.UNHANDLED_EXCEPTION));
        assertEquals(0, result.count(BuildScanRule.EMPTY_CATCH_BLOCK));
    }
}
