package com.github.ksoichiro.build.info

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue

class PluginTest {
    private static final String PLUGIN_ID = 'com.github.ksoichiro.build.info'

    @Test
    void apply() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: PLUGIN_ID

        assertTrue(project.tasks.generateBuildInfo instanceof GenerateBuildInfoTask)
    }
}
