package com.github.ksoichiro.build.info

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class MultiProjectFunctionalTest {
    private static final String PLUGIN_ID = 'com.github.ksoichiro.build.info'

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    File rootDir
    File libDir
    File appDir
    File rootBuildFile
    File settingsFile
    File libBuildFile
    File appBuildFile
    List<File> pluginClasspath
    Grgit grgit

    @Before
    void setup() {
        rootDir = testProjectDir.root
        rootDir.mkdirs()
        libDir = new File(rootDir, "library")
        libDir.mkdirs()
        appDir = new File(rootDir, "app")
        appDir.mkdirs()
        rootBuildFile = new File(rootDir, "build.gradle")
        settingsFile = new File(rootDir, "settings.gradle")
        libBuildFile = new File(libDir, "build.gradle")
        appBuildFile = new File(appDir, "build.gradle")

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines()
            .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
            .collect { new File(it) }

        new File(appDir, ".gitignore").text = """\
            |.gradle/
            |build/
            |""".stripMargin().stripIndent()
        def pkg = new File("${appDir}/src/main/java/hello")
        pkg.mkdirs()
        new File(pkg, "App.java").text = """\
            |package hello;
            |public class App {
            |    public static void main(String[] args) {
            |        System.out.println("Hello!");
            |    }
            |}
            |""".stripMargin().stripIndent()

        settingsFile.text = """\
            |include ':library', ':app'
            |""".stripMargin().stripIndent()
        rootBuildFile.text = """\
            |allprojects {
            |    repositories {
            |        mavenCentral()
            |    }
            |    apply plugin: 'java'
            |}
            |""".stripMargin().stripIndent()
        libBuildFile.text = """\
            |dependencies {
            |    compile 'org.springframework.boot:spring-boot-starter-actuator:1.3.0.RELEASE'
            |}
            |""".stripMargin().stripIndent()
        appBuildFile.text = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |dependencies {
            |    compile project(':library')
            |}
            |""".stripMargin().stripIndent()

        grgit = Grgit.init(dir: rootDir.path)
        grgit.add(patterns: [
            '.gitignore',
            'settings.gradle',
            'library/build.gradle',
            'app/build.gradle',
            'app/src/main/java/hello/App.java'])
        grgit.commit(message: 'Initial commit.')
    }

    @Test
    void springBootActuatorInTransitiveDependency() {
        def result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments("build")
            .withPluginClasspath(pluginClasspath)
            .build()

        assertEquals(result.task(":build").getOutcome(), TaskOutcome.SUCCESS)

        File propsFile = new File("${appDir}/build/resources/main/git.properties")
        assertTrue(propsFile.exists())
    }
}
