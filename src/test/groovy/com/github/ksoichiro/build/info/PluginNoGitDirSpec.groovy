package com.github.ksoichiro.build.info

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class PluginNoGitDirSpec extends Specification {
    private static final String PLUGIN_ID = 'com.github.ksoichiro.build.info'

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    File rootDir

    def setup() {
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

    def setDefaultStringIfGitInfoDoesNotExist() {
        setup:
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            gitPropertiesEnabled true
            gitInfoMode BuildInfoExtension.MODE_DEFAULT
        }
        project.evaluate()

        when:
        project.tasks.generateBuildInfo.execute()

        then:
        project.file("${project.buildDir}/resources/main/git.properties").exists()
    }

    def ignoreIfGitInfoDoesNotExist() {
        setup:
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            gitPropertiesEnabled true
            gitInfoMode BuildInfoExtension.MODE_IGNORE
        }
        project.evaluate()

        when:
        project.tasks.generateBuildInfo.execute()

        then:
        !project.file("${project.buildDir}/resources/main/git.properties").exists()
    }

    def throwExceptionIfGitInfoDoesNotExist() {
        setup:
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            gitPropertiesEnabled true
            gitInfoMode BuildInfoExtension.MODE_ERROR
        }
        project.evaluate()

        when:
        project.tasks.generateBuildInfo.execute()

        then:
        thrown(GradleException)
    }
}
