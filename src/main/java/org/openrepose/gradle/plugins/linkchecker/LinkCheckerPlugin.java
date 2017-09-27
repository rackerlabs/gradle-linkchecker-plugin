package org.openrepose.gradle.plugins.linkchecker;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Defines the Link Checker Plugin.
 */
public class LinkCheckerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("linkchecker", LinkCheckerPluginExtension.class);
        project.getTasks().create("linkchecker", LinkCheckerPluginTask.class);
    }
}
