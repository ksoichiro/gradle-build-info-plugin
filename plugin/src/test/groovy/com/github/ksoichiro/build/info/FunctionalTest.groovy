package com.github.ksoichiro.build.info

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.jar.JarFile

import static org.junit.Assert.*

class FunctionalTest {
    private static final String PLUGIN_ID = 'com.github.ksoichiro.build.info'

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    File rootDir
    File buildFile
    List<File> pluginClasspath
    Grgit grgit

    @Before
    void setup() {
        rootDir = testProjectDir.root
        if (!rootDir.exists()) {
            rootDir.mkdir()
        }
        buildFile = new File(rootDir, "build.gradle")

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines()
            .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
            .collect { new File(it) }

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

    @Test
    public void generateBuildInfo() {
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |archivesBaseName = 'foo'
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        def result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments("build")
            .withPluginClasspath(pluginClasspath)
            .build()

        assertEquals(result.task(":build").getOutcome(), TaskOutcome.SUCCESS)

        File jarFile = new File("${rootDir}/build/libs/foo.jar")
        assertTrue(jarFile.exists())
        JarFile jar = new JarFile(jarFile)
        def manifestAttrs = jar.manifest.mainAttributes
        assertEquals(grgit.head().abbreviatedId, manifestAttrs.getValue('Git-Commit'))
    }

    @Test
    public void gitPropertiesEnabled() {
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |buildInfo {
            |    gitPropertiesEnabled true
            |}
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        def result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments("build")
            .withPluginClasspath(pluginClasspath)
            .build()

        assertEquals(result.task(":build").getOutcome(), TaskOutcome.SUCCESS)

        File propsFile = new File("${rootDir}/build/resources/main/git.properties")
        assertTrue(propsFile.exists())
        Properties props = new Properties()
        props.load(propsFile.newReader())
        assertEquals(grgit.branch.current.name, props.get('git.branch'))
        assertEquals(grgit.head().abbreviatedId, props.get('git.commit.id'))
        assertEquals(grgit.head().date.format('yyyy-MM-dd HH:mm:ss Z'), props.get('git.commit.time'))
    }

    @Test
    public void gitPropertiesDisabled() {
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |buildInfo {
            |    gitPropertiesEnabled false
            |}
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        def result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments("build")
            .withPluginClasspath(pluginClasspath)
            .build()

        assertEquals(result.task(":build").getOutcome(), TaskOutcome.SUCCESS)

        File propsFile = new File("${rootDir}/build/resources/main/git.properties")
        assertFalse(propsFile.exists())
    }

    @Test
    public void gitPropertiesDisabledAsDefault() {
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        def result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments("build")
            .withPluginClasspath(pluginClasspath)
            .build()

        assertEquals(result.task(":build").getOutcome(), TaskOutcome.SUCCESS)

        File propsFile = new File("${rootDir}/build/resources/main/git.properties")
        assertFalse(propsFile.exists())
    }

    @Test
    public void gitPropertiesEnabledWhenSpringBootActuatorIsUsed() {
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |repositories {
            |    mavenCentral()
            |}
            |dependencies {
            |    compile 'org.springframework.boot:spring-boot-starter-actuator:1.3.0.RELEASE'
            |}
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        def result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments("build")
            .withPluginClasspath(pluginClasspath)
            .build()

        assertEquals(result.task(":build").getOutcome(), TaskOutcome.SUCCESS)

        File propsFile = new File("${rootDir}/build/resources/main/git.properties")
        assertTrue(propsFile.exists())
    }

    @Test
    public void manifestDisabled() {
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |archivesBaseName = 'foo'
            |buildInfo {
            |    manifestEnabled false
            |}
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        def result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments("build")
            .withPluginClasspath(pluginClasspath)
            .build()

        assertEquals(result.task(":build").getOutcome(), TaskOutcome.SUCCESS)

        File jarFile = new File("${rootDir}/build/libs/foo.jar")
        assertTrue(jarFile.exists())
        JarFile jar = new JarFile(jarFile)
        def manifestAttrs = jar.manifest.mainAttributes
        assertFalse(manifestAttrs.containsKey('Git-Branch'))
        assertFalse(manifestAttrs.containsKey('Git-Commit'))
        assertFalse(manifestAttrs.containsKey('Git-Committer-Date'))
        assertFalse(manifestAttrs.containsKey('Build-Date'))
    }
}
