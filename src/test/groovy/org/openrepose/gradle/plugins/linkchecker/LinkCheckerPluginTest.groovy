package org.openrepose.gradle.plugins.linkchecker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue

class LinkCheckerPluginTest {
    @Test
    public void demo_plugin_should_add_task_to_project() {
        Project project = ProjectBuilder.builder().build()
        project.getPlugins().apply 'org.openrepose.gradle.plugins.linkchecker'

        assertTrue(project.tasks.linkchecker instanceof LinkCheckerPluginTask)
    }
}
