package com.github.ksoichiro.build.info

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.jar.JarFile

class FunctionalSpec extends Specification {
    private static final String PLUGIN_ID = 'com.github.ksoichiro.build.info'

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    File rootDir
    File buildFile
    List<File> pluginClasspath
    Grgit grgit

    def setup() {
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

    def generateBuildInfo() {
        setup:
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |archivesBaseName = 'foo'
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        when:
        def result = runBuild()

        then:
        result.task(":build").getOutcome() == TaskOutcome.SUCCESS
        File jarFile = new File("${rootDir}/build/libs/foo.jar")
        jarFile.exists()
        new JarFile(jarFile).manifest.mainAttributes.getValue('Git-Commit') == grgit.head().abbreviatedId
    }

    def gitPropertiesEnabled() {
        setup:
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

        when:
        def result = runBuild()
        File propsFile = new File("${rootDir}/build/resources/main/git.properties")

        then:
        result.task(":build").getOutcome() == TaskOutcome.SUCCESS
        propsFile.exists()

        when:
        Properties props = new Properties()
        props.load(propsFile.newReader())

        then:
        grgit.branch.current.name == props.get('git.branch')
        grgit.head().abbreviatedId == props.get('git.commit.id')
        grgit.head().date.format('yyyy-MM-dd HH:mm:ss Z') == props.get('git.commit.time')
    }

    def gitPropertiesDisabled() {
        setup:
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

        when:
        def result = runBuild()

        then:
        result.task(":build").getOutcome() == TaskOutcome.SUCCESS

        File propsFile = new File("${rootDir}/build/resources/main/git.properties")
        !propsFile.exists()
    }

    def gitPropertiesDisabledAsDefault() {
        setup:
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        when:
        def result = runBuild()

        then:
        result.task(":build").getOutcome() == TaskOutcome.SUCCESS

        File propsFile = new File("${rootDir}/build/resources/main/git.properties")
        !propsFile.exists()
    }

    def gitPropertiesEnabledWhenSpringBootActuatorIsUsed() {
        setup:
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

        when:
        def result = runBuild()

        then:
        result.task(":build").getOutcome() == TaskOutcome.SUCCESS

        File propsFile = new File("${rootDir}/build/resources/main/git.properties")
        propsFile.exists()
    }

    def manifestDisabled() {
        setup:
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

        when:
        def result = runBuild()

        then:
        result.task(":build").getOutcome() == TaskOutcome.SUCCESS

        File jarFile = new File("${rootDir}/build/libs/foo.jar")
        jarFile.exists()
        JarFile jar = new JarFile(jarFile)
        def manifestAttrs = jar.manifest.mainAttributes
        !manifestAttrs.containsKey('Git-Branch')
        !manifestAttrs.containsKey('Git-Commit')
        !manifestAttrs.containsKey('Git-Committer-Date')
        !manifestAttrs.containsKey('Build-Date')
        !manifestAttrs.containsKey('Build-Java-Version')
        !manifestAttrs.containsKey('Build-Java-Vendor')
        !manifestAttrs.containsKey('Build-Os-Name')
        !manifestAttrs.containsKey('Build-Os-Version')
    }

    def upToDateWhenCommitIdDoesNotChange() {
        setup:
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

        when:
        def result = run('1st')

        then:
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.SUCCESS

        when:
        result = run('2nd')

        then:
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.UP_TO_DATE

        when:
        // Removing output causes build
        new File("${rootDir}/build/resources/main/git.properties").delete()
        result = run('3rd')

        then:
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.SUCCESS
    }

    def doNotSkipWhenCommitIdChanges() {
        setup:
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

        when:
        def result = run('1st')

        then:
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.SUCCESS

        when:
        new File(rootDir, "README.md").text = """\
            |# Test
            |""".stripMargin().stripIndent()
        grgit.add(patterns: ['README.md'])
        grgit.commit(message: 'Add readme')
        result = run('2nd')

        then:
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.SUCCESS
    }

    def skipWhenAllOfTheFeaturesAreDisabled() {
        setup:
        def buildFileContent = """\
            |plugins {
            |    id '${PLUGIN_ID}'
            |}
            |apply plugin: 'java'
            |buildInfo {
            |    gitPropertiesEnabled false
            |    manifestEnabled false
            |}
            |""".stripMargin().stripIndent()
        buildFile.text = buildFileContent

        when:
        def result = run('1st')

        then:
        // Throwing StopExecutionException results in SUCCESS (not SKIPPED or UP_TO_DATE)
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.SUCCESS
    }

    def skipWhenGitStatusIsDirtyAndSomethingChanges() {
        setup:
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

        when:
        // Make working copy dirty
        new File(rootDir, "README.md").text = """\
            |# Test
            |""".stripMargin().stripIndent()

        def result = run('1st')

        then:
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.SUCCESS

        when:
        // Update dirty working copy
        new File(rootDir, "README.md").text = """\
            |# Test
            |
            |This is a test.
            |""".stripMargin().stripIndent()

        result = run('2nd')

        then:
        // Even when some files are changed, it is assumed to be up-to-date because the commit does not change
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.UP_TO_DATE

        when:
        // Commit causes build
        grgit.add(patterns: ['README.md'])
        grgit.commit(message: 'Add readme')
        result = run('3rd')

        then:
        // Even when some files are changed, it is assumed to be up-to-date because the commit does not change
        result.task(":${GenerateBuildInfoTask.NAME}").getOutcome() == TaskOutcome.SUCCESS
    }

    BuildResult runBuild() {
        GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments('build')
            .withPluginClasspath(pluginClasspath)
            .build()
    }

    BuildResult run(def label) {
        BuildResult result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments(GenerateBuildInfoTask.NAME)
            .withPluginClasspath(pluginClasspath)
            .build()

        if (label) {
            println label
        }
        File props = new File("${rootDir}/build/resources/main/git.properties")
        if (props.exists()) {
            println props.text
        }
        result
    }
}
