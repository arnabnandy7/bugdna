package io.github.bugdna.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScanMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void skipsWhenConfigured() throws Exception {
        ScanMojo mojo = new ScanMojo();
        set(mojo, "skip", Boolean.TRUE);

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void succeedsWhenNoIssuesAreFound() throws Exception {
        Path sourceRoot = sourceRoot("main");
        writeJava(sourceRoot, "Clean.java",
                "class Clean {",
                "  void run() {",
                "    int value = 1;",
                "  }",
                "}"
        );

        ScanMojo mojo = mojo(sourceRoot, null, true, false);

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void warnsOnlyWhenIssuesAreFoundAndFailingIsDisabled() throws Exception {
        Path sourceRoot = sourceRoot("main");
        writeJava(sourceRoot, "Sample.java",
                "class Sample {",
                "  void run() {",
                "    try {",
                "      risky();",
                "    } catch (Exception exception) {",
                "    }",
                "  }",
                "  void risky() { }",
                "}"
        );

        ScanMojo mojo = mojo(sourceRoot, null, false, false);

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void failsWhenIssuesAreFoundAndFailingIsEnabled() throws Exception {
        Path sourceRoot = sourceRoot("main");
        writeJava(sourceRoot, "Sample.java",
                "class Sample {",
                "  void run() throws Exception {",
                "  }",
                "}"
        );

        ScanMojo mojo = mojo(sourceRoot, null, true, false);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void includesTestRootsWhenConfigured() throws Exception {
        Path mainRoot = sourceRoot("main");
        Path testRoot = sourceRoot("test");
        writeJava(mainRoot, "Clean.java",
                "class Clean {",
                "  void run() { }",
                "}"
        );
        writeJava(testRoot, "SampleTest.java",
                "class SampleTest {",
                "  void run() throws Exception {",
                "  }",
                "}"
        );

        ScanMojo mojo = mojo(mainRoot, testRoot, true, true);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    private ScanMojo mojo(
            Path sourceRoot,
            Path testSourceRoot,
            boolean failOnIssues,
            boolean includeTests
    ) throws Exception {
        MavenProject project = new MavenProject();
        project.addCompileSourceRoot(sourceRoot.toString());
        if (testSourceRoot != null) {
            project.addTestCompileSourceRoot(testSourceRoot.toString());
        }

        ScanMojo mojo = new ScanMojo();
        set(mojo, "project", project);
        set(mojo, "failOnIssues", Boolean.valueOf(failOnIssues));
        set(mojo, "includeTests", Boolean.valueOf(includeTests));
        return mojo;
    }

    private Path sourceRoot(String name) throws Exception {
        Path sourceRoot = tempDir.resolve(name).resolve("java");
        Files.createDirectories(sourceRoot);
        return sourceRoot;
    }

    private void writeJava(Path sourceRoot, String fileName, String... lines) throws Exception {
        Files.write(
                sourceRoot.resolve(fileName),
                Arrays.asList(lines),
                StandardCharsets.UTF_8
        );
    }

    private void set(ScanMojo mojo, String fieldName, Object value) throws Exception {
        Field field = ScanMojo.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(mojo, value);
    }
}
