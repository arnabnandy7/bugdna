package io.github.bugdna.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers BugDNA build validation tasks.
 */
public final class BugDnaGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        BugDnaExtension extension = project.getExtensions()
                .create("bugdna", BugDnaExtension.class);

        project.getTasks().register("bugdnaScan", BugDnaScanTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Scans Java sources for BugDNA exception-handling hazards.");
            task.getFailOnIssues().convention(project.provider(extension::isFailOnIssues));
            task.getIncludeTests().convention(project.provider(extension::isIncludeTests));
            task.getConfiguredSourceRoots().set(project.provider(extension::getSourceRoots));
        });
    }
}
