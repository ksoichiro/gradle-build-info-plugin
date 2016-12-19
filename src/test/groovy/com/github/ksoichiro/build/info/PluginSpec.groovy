package com.github.ksoichiro.build.info

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class PluginSpec extends Specification {
    private static final String PLUGIN_ID = 'com.github.ksoichiro.build.info'

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File rootDir
    Grgit grgit

    def setup() {
        rootDir = testProjectDir.root
        if (!rootDir.exists()) {
            rootDir.mkdir()
        }

        new File(rootDir, ".gitignore").text = """\
            |.gradle/
            |/build/
            |""".stripMargin().stripIndent()
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
        grgit = Grgit.init(dir: rootDir.path)
        grgit.add(patterns: ['.gitignore', 'build.gradle', 'src/main/java/hello/App.java'])
        grgit.commit(message: 'Initial commit.')
    }

    def apply() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.apply plugin: PLUGIN_ID

        then:
        project.tasks.generateBuildInfo instanceof GenerateBuildInfoTask
    }

    def generateWithJavaPlugin() {
        setup:
        Project project = ProjectBuilder.builder().withProjectDir(rootDir).build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.evaluate()

        when:
        project.tasks.generateBuildInfo.execute()

        then:
        !project.file("${project.buildDir}/resources/main/git.properties").exists()
    }

    def generateWithJavaPluginAndEnabled() {
        setup:
        Project project = ProjectBuilder.builder().withProjectDir(rootDir).build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            gitPropertiesEnabled true
        }
        project.evaluate()

        when:
        project.tasks.generateBuildInfo.execute()

        then:
        project.file("${project.buildDir}/resources/main/git.properties").exists()
    }

    def generateWithoutJavaPlugin() {
        setup:
        Project project = ProjectBuilder.builder().withProjectDir(rootDir).build()
        project.apply plugin: PLUGIN_ID

        when:
        project.evaluate()

        then:
        thrown(ProjectConfigurationException)
    }

    def generateWithoutJavaPluginAndEnabledAndSetAnotherDestination() {
        setup:
        Project project = ProjectBuilder.builder().withProjectDir(rootDir).build()
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            gitPropertiesEnabled true
            destinationDir project.file("${project.buildDir}/foo/bar/")
        }
        project.evaluate()

        when:
        project.tasks.generateBuildInfo.execute()

        then:
        !project.file("${project.buildDir}/resources/main/git.properties").exists()
        project.file("${project.buildDir}/foo/bar/git.properties").exists()
    }

    def generateWithJavaPluginAndSpringBootActuator() {
        setup:
        Project project = ProjectBuilder.builder().withProjectDir(rootDir).build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.repositories {
            mavenCentral()
        }
        project.dependencies {
            compile 'org.springframework.boot:spring-boot-starter-actuator:1.3.0.RELEASE'
        }
        project.evaluate()

        when:
        project.tasks.generateBuildInfo.execute()

        then:
        project.file("${project.buildDir}/resources/main/git.properties").exists()
    }

    def configureExtension() {
        setup:
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            buildDateFormat 'yyyy-MM-dd'
            committerDateFormat 'yyyy-MM-dd'
        }
        project.evaluate()

        when:
        project.tasks.generateBuildInfo.execute()

        then:
        !project.file("${project.buildDir}/resources/main/git.properties").exists()
    }

    def transitiveDependency() {
        setup:
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.repositories {
            mavenCentral()
        }
        project.dependencies {
            compile 'org.springframework.boot:spring-boot-starter-actuator:1.3.0.RELEASE'
        }

        when:
        project.evaluate()

        then:
        GenerateBuildInfoTask.hasDependency(project, 'org.springframework', 'spring-core')
    }
}
