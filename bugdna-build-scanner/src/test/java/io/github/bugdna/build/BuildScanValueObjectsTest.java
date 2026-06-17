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
        assertThrows(UnsupportedOperationException.class, () -> config.getSourceRoots().clear());
    }

    @Test
    void configRejectsNullRoots() {
        BuildScanConfig.Builder builder = BuildScanConfig.builder();

        assertThrows(NullPointerException.class, () -> builder.addSourceRoot(null));
        assertThrows(NullPointerException.class, () -> builder.addSourceRoots(null));
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
        Path file = Paths.get("Sample.java");

        assertThrows(NullPointerException.class, () -> new BuildScanIssue(
                null,
                BuildScanSeverity.WARNING,
                file,
                1,
                "message",
                "snippet"
        ));
        assertThrows(NullPointerException.class, () -> new BuildScanIssue(
                BuildScanRule.EMPTY_CATCH_BLOCK,
                null,
                file,
                1,
                "message",
                "snippet"
        ));
        assertThrows(NullPointerException.class, () -> new BuildScanIssue(
                BuildScanRule.EMPTY_CATCH_BLOCK,
                BuildScanSeverity.WARNING,
                null,
                1,
                "message",
                "snippet"
        ));
        assertThrows(NullPointerException.class, () -> new BuildScanIssue(
                BuildScanRule.EMPTY_CATCH_BLOCK,
                BuildScanSeverity.WARNING,
                file,
                1,
                null,
                "snippet"
        ));
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
        assertThrows(UnsupportedOperationException.class, () -> result.getIssues().clear());
        assertThrows(UnsupportedOperationException.class, () -> counts.clear());
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
                Paths.get("Sample.java"),
                1,
                "message",
                "snippet"
        );
    }
}
