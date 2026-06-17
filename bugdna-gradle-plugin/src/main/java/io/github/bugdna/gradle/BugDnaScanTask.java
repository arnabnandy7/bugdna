package io.github.bugdna.gradle;

import io.github.bugdna.build.BuildScanConfig;
import io.github.bugdna.build.BuildScanIssue;
import io.github.bugdna.build.BuildScanResult;
import io.github.bugdna.build.BugDnaBuildScanner;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle task for BugDNA source validation.
 */
public abstract class BugDnaScanTask extends DefaultTask {

    @Input
    public abstract Property<Boolean> getFailOnIssues();

    @Input
    public abstract Property<Boolean> getIncludeTests();

    @Input
    public abstract ListProperty<String> getConfiguredSourceRoots();

    @TaskAction
    public void scan() {
        BuildScanConfig.Builder builder = BuildScanConfig.builder()
                .addSourceRoots(defaultSourceRoots());

        for (String sourceRoot : getConfiguredSourceRoots().get()) {
            builder.addSourceRoot(getProject().file(sourceRoot).toPath());
        }

        BuildScanResult result;
        try {
            result = new BugDnaBuildScanner().scan(builder.build());
        } catch (IOException exception) {
            throw new GradleException("BugDNA build scan failed.", exception);
        }

        if (!result.hasIssues()) {
            getLogger().lifecycle("BugDNA build scan found no exception-handling issues.");
            return;
        }

        getLogger().warn("BugDNA build scan found {} issue(s):", result.getIssues().size());
        for (BuildScanIssue issue : result.getIssues()) {
            getLogger().warn(format(issue));
        }

        if (getFailOnIssues().get()) {
            throw new GradleException(
                    "BugDNA build scan failed with " + result.getIssues().size()
                            + " issue(s). Set bugdna.failOnIssues = false to warn only."
            );
        }
    }

    private List<Path> defaultSourceRoots() {
        List<Path> roots = new ArrayList<>();
        JavaPluginExtension java = getProject().getExtensions().findByType(JavaPluginExtension.class);
        if (java == null) {
            return roots;
        }
        java.getSourceSets().getByName("main").getAllJava().getSourceDirectories()
                .getElements().get().forEach(directory -> roots.add(path(directory)));
        if (getIncludeTests().get()) {
            java.getSourceSets().getByName("test").getAllJava().getSourceDirectories()
                    .getElements().get().forEach(directory -> roots.add(path(directory)));
        }
        return roots;
    }

    private Path path(Directory directory) {
        return directory.getAsFile().toPath();
    }

    private String format(BuildScanIssue issue) {
        return issue.getFile() + ":" + issue.getLine()
                + " [" + issue.getRule() + "] "
                + issue.getMessage()
                + " `" + issue.getSnippet() + "`";
    }
}
