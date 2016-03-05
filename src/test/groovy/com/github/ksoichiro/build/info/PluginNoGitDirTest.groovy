package com.github.ksoichiro.build.info

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class PluginNoGitDirTest {
    private static final String PLUGIN_ID = 'com.github.ksoichiro.build.info'

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    File rootDir

    @Before
    void setup() {
        rootDir = testProjectDir.root
        if (!rootDir.exists()) {
            rootDir.mkdir()
        }

        def pkg = new File("${rootDir}/src/main/java/hello")
        pkg.mkdirs()
        new File(pkg, "App.java").text = """\
            |package hello;
            |public class App {
            |    public static void main(String[] args) {
            |        System.out.println("Hello!");
            |    }
            |}
            |""".stripMargin().stripIndent()
    }

    @Test
    void setDefaultStringIfGitInfoDoesNotExist() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            gitPropertiesEnabled true
            gitInfoMode BuildInfoExtension.MODE_DEFAULT
        }
        project.evaluate()
        project.tasks.generateBuildInfo.execute()
        assertTrue(project.file("${project.buildDir}/resources/main/git.properties").exists())
    }

    @Test
    void ignoreIfGitInfoDoesNotExist() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            gitPropertiesEnabled true
            gitInfoMode BuildInfoExtension.MODE_IGNORE
        }
        project.evaluate()
        project.tasks.generateBuildInfo.execute()
        assertFalse(project.file("${project.buildDir}/resources/main/git.properties").exists())
    }

    @Test(expected = GradleException)
    void throwExceptionIfGitInfoDoesNotExist() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            gitPropertiesEnabled true
            gitInfoMode BuildInfoExtension.MODE_ERROR
        }
        project.evaluate()
        project.tasks.generateBuildInfo.execute()
    }
}
