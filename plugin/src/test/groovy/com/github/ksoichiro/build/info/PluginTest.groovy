package com.github.ksoichiro.build.info

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertTrue

class PluginTest {
    private static final String PLUGIN_ID = 'com.github.ksoichiro.build.info'

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    File rootDir
    Grgit grgit

    @Before
    void setup() {
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

    @Test
    void apply() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: PLUGIN_ID

        assertTrue(project.tasks.generateBuildInfo instanceof GenerateBuildInfoTask)
    }

    @Test
    void generateWithJavaPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(rootDir)build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.evaluate()
        project.tasks.generateBuildInfo.execute()
    }

    @Test
    void generateWithoutJavaPlugin() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: PLUGIN_ID
        project.evaluate()
        project.tasks.generateBuildInfo.execute()
    }

    @Test
    void generateWithJavaPluginAndSpringBootActuator() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.dependencies {
            compile 'org.springframework.boot:spring-boot-starter-actuator:1.3.0.RELEASE'
        }
        project.evaluate()
        project.tasks.generateBuildInfo.execute()
    }

    @Test
    void configureExtension() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: PLUGIN_ID
        project.buildInfo {
            buildDateFormat 'yyyy-MM-dd'
            committerDateFormat 'yyyy-MM-dd'
        }
        project.evaluate()
        project.tasks.generateBuildInfo.execute()
    }
}
