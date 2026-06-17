package io.github.bugdna.build;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildScanValueObjectsTest {

    private static final Path SAMPLE_FILE = Paths.get("Sample.java");

    @Test
    void configCollectsRootsDefensively() {
        Path main = Paths.get("src/main/java");
        Path test = Paths.get("src/test/java");
        List<Path> roots = Arrays.asList(main, test);

        BuildScanConfig config = BuildScanConfig.builder()
                .addSourceRoots(roots)
                .includeTests(true)
                .build();

        assertEquals(roots, config.getSourceRoots());
        assertTrue(config.isIncludeTests());
        List<Path> configuredRoots = config.getSourceRoots();
        assertThrows(UnsupportedOperationException.class, configuredRoots::clear);
    }

    @Test
    void configRejectsNullRoots() {
        BuildScanConfig.Builder builder = BuildScanConfig.builder();
        NullRootCalls nullRootCalls = new NullRootCalls(builder);

        assertThrows(NullPointerException.class, nullRootCalls::addSourceRoot);
        assertThrows(NullPointerException.class, nullRootCalls::addSourceRoots);
    }

    @Test
    void issueExposesFieldsAndDefaultsNullSnippet() {
        Path file = Paths.get("Sample.java");

        BuildScanIssue issue = new BuildScanIssue(
                BuildScanRule.EMPTY_CATCH_BLOCK,
                BuildScanSeverity.ERROR,
                file,
                42,
                "Catch block is empty.",
                null
        );

        assertEquals(BuildScanRule.EMPTY_CATCH_BLOCK, issue.getRule());
        assertEquals(BuildScanSeverity.ERROR, issue.getSeverity());
        assertEquals(file, issue.getFile());
        assertEquals(42, issue.getLine());
        assertEquals("Catch block is empty.", issue.getMessage());
        assertEquals("", issue.getSnippet());
        assertTrue(issue.toString().contains("EMPTY_CATCH_BLOCK"));
    }

    @Test
    void issueRejectsRequiredNulls() {
        assertThrows(NullPointerException.class, this::issueWithNullRule);
        assertThrows(NullPointerException.class, this::issueWithNullSeverity);
        assertThrows(NullPointerException.class, this::issueWithNullFile);
        assertThrows(NullPointerException.class, this::issueWithNullMessage);
    }

    @Test
    void resultCountsIssuesByRuleAndIsImmutable() {
        BuildScanIssue emptyCatch = issue(BuildScanRule.EMPTY_CATCH_BLOCK);
        BuildScanIssue generic = issue(BuildScanRule.GENERIC_EXCEPTION_USAGE);
        BuildScanResult result = new BuildScanResult(Arrays.asList(emptyCatch, generic, generic));

        assertTrue(result.hasIssues());
        assertEquals(1, result.count(BuildScanRule.EMPTY_CATCH_BLOCK));
        assertEquals(2, result.count(BuildScanRule.GENERIC_EXCEPTION_USAGE));
        assertEquals(0, result.count(BuildScanRule.UNHANDLED_EXCEPTION));

        Map<BuildScanRule, Integer> counts = result.countsByRule();
        assertEquals(Integer.valueOf(1), counts.get(BuildScanRule.EMPTY_CATCH_BLOCK));
        assertEquals(Integer.valueOf(2), counts.get(BuildScanRule.GENERIC_EXCEPTION_USAGE));
        assertEquals(Integer.valueOf(0), counts.get(BuildScanRule.UNHANDLED_EXCEPTION));
        List<BuildScanIssue> issues = result.getIssues();
        assertThrows(UnsupportedOperationException.class, issues::clear);
        assertThrows(UnsupportedOperationException.class, counts::clear);
    }

    @Test
    void emptyResultHasNoIssues() {
        BuildScanResult result = new BuildScanResult(Arrays.asList());

        assertFalse(result.hasIssues());
    }

    private BuildScanIssue issue(BuildScanRule rule) {
        return new BuildScanIssue(
                rule,
                BuildScanSeverity.WARNING,
                SAMPLE_FILE,
                1,
                "message",
                "snippet"
        );
    }

    private BuildScanIssue issueWithNullRule() {
        return new BuildScanIssue(
                null,
                BuildScanSeverity.WARNING,
                SAMPLE_FILE,
                1,
                "message",
                "snippet"
        );
    }

    private BuildScanIssue issueWithNullSeverity() {
        return new BuildScanIssue(
                BuildScanRule.EMPTY_CATCH_BLOCK,
                null,
                SAMPLE_FILE,
                1,
                "message",
                "snippet"
        );
    }

    private BuildScanIssue issueWithNullFile() {
        return new BuildScanIssue(
                BuildScanRule.EMPTY_CATCH_BLOCK,
                BuildScanSeverity.WARNING,
                null,
                1,
                "message",
                "snippet"
        );
    }

    private BuildScanIssue issueWithNullMessage() {
        return new BuildScanIssue(
                BuildScanRule.EMPTY_CATCH_BLOCK,
                BuildScanSeverity.WARNING,
                SAMPLE_FILE,
                1,
                null,
                "snippet"
        );
    }

    private static final class NullRootCalls {
        private final BuildScanConfig.Builder builder;

        private NullRootCalls(BuildScanConfig.Builder builder) {
            this.builder = builder;
        }

        private void addSourceRoot() {
            builder.addSourceRoot(null);
        }

        private void addSourceRoots() {
            builder.addSourceRoots(null);
        }
    }
}
