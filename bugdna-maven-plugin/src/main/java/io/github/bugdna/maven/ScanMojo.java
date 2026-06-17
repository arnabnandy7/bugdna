package io.github.bugdna.maven;

import io.github.bugdna.build.BuildScanConfig;
import io.github.bugdna.build.BuildScanIssue;
import io.github.bugdna.build.BuildScanResult;
import io.github.bugdna.build.BugDnaBuildScanner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs BugDNA build-time validation against Java source roots.
 */
@Mojo(name = "scan", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public final class ScanMojo extends AbstractMojo {

    /**
     * Current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Fails the build when findings are present.
     */
    @Parameter(property = "bugdna.failOnIssues", defaultValue = "true")
    private boolean failOnIssues;

    /**
     * Include test source roots in the scan.
     */
    @Parameter(property = "bugdna.includeTests", defaultValue = "false")
    private boolean includeTests;

    /**
     * Skip BugDNA build validation.
     */
    @Parameter(property = "bugdna.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("BugDNA build scan skipped.");
            return;
        }

        BuildScanConfig config = BuildScanConfig.builder()
                .addSourceRoots(sourceRoots())
                .includeTests(includeTests)
                .build();

        BuildScanResult result;
        try {
            result = new BugDnaBuildScanner().scan(config);
        } catch (IOException exception) {
            throw new MojoExecutionException("BugDNA build scan failed.", exception);
        }

        if (!result.hasIssues()) {
            getLog().info("BugDNA build scan found no exception-handling issues.");
            return;
        }

        getLog().warn("BugDNA build scan found " + result.getIssues().size() + " issue(s):");
        for (BuildScanIssue issue : result.getIssues()) {
            getLog().warn(format(issue));
        }

        if (failOnIssues) {
            throw new MojoExecutionException(
                    "BugDNA build scan failed with " + result.getIssues().size()
                            + " issue(s). Set -Dbugdna.failOnIssues=false to warn only."
            );
        }
    }

    private List<Path> sourceRoots() {
        List<Path> roots = new ArrayList<>();
        for (String sourceRoot : project.getCompileSourceRoots()) {
            roots.add(Paths.get(sourceRoot));
        }
        if (includeTests) {
            for (String sourceRoot : project.getTestCompileSourceRoots()) {
                roots.add(Paths.get(sourceRoot));
            }
        }
        return roots;
    }

    private String format(BuildScanIssue issue) {
        return issue.getFile() + ":" + issue.getLine()
                + " [" + issue.getRule() + "] "
                + issue.getMessage()
                + " `" + issue.getSnippet() + "`";
    }
}
