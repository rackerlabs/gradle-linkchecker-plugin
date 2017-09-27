package org.openrepose.gradle.plugins.linkchecker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue

class LinkCheckerPluginTaskTest {
    @Test
    public void should_be_able_to_add_task_to_project() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('linkchecker', type: LinkCheckerPluginTask)
        assertTrue(task instanceof LinkCheckerPluginTask)
    }
}
