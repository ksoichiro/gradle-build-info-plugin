package com.github.ksoichiro.build.info

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

class GenerateBuildInfoTask extends DefaultTask {
    public static final String NAME = 'generateBuildInfo'

    GenerateBuildInfoTask() {
        project.afterEvaluate {
            if (project.plugins.hasPlugin(JavaPlugin)) {
                dependsOn(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
                getClassesTask().dependsOn(NAME)
            }
        }
    }

    @TaskAction
    void exec() {
        if (project.plugins.hasPlugin(JavaPlugin)) {
            if (hasDependency(project, 'org.springframework.boot', 'spring-boot-starter-actuator')) {
                generateGitProperties()
            }
            mergeManifest()
        }
    }

    Task getClassesTask() {
        project.tasks.findByName(JavaPlugin.CLASSES_TASK_NAME)
    }

    ProcessResources getProcessResourcesTask() {
        (ProcessResources) project.tasks.findByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
    }

    void generateGitProperties() {
        def gitInfo = readGitInfo()
        Properties props = new Properties()
        props.putAll([
            'git.branch'     : gitInfo.branch,
            'git.commit.id'  : gitInfo.commit,
            'git.commit.time': gitInfo.committerDate])
        File buildResourcesDir = getProcessResourcesTask().destinationDir
        if (!buildResourcesDir.exists()) {
            buildResourcesDir.mkdirs()
        }
        File propsFile = new File(buildResourcesDir, 'git.properties')
        props.store(propsFile.newWriter(), null)
    }

    void mergeManifest() {
        def attributes = [:] as Map<String, ?>
        def gitInfo = readGitInfo()

        attributes["Git-Branch"] = gitInfo.branch
        attributes["Git-Commit"] = gitInfo.commit
        attributes["Git-Committer-Date"] = gitInfo.committerDate
        attributes["Build-Date"] = new Date().format("yyyy-MM-dd HH:mm:ss Z")

        (project.tasks.jar as Jar).manifest {
            it.attributes(attributes)
        }
    }

    GitInfo readGitInfo() {
        def branch
        def commit
        def committerDate
        try {
            Grgit grgit = Grgit.open(currentDir: project.projectDir)
            branch = grgit.branch.current.name
            Commit head = grgit.head()
            commit = head.abbreviatedId
            committerDate = head.date.format("yyyy-MM-dd HH:mm:ss Z")
        } catch (ignored) {
            branch = "unknown"
            commit = "unknown"
            committerDate = "unknown"
        }
        new GitInfo(branch: branch,
            commit: commit,
            committerDate: committerDate)
    }

    static boolean hasDependency(Project project, String group, String name) {
        project.configurations.compile.incoming.dependencies.any {
            it.group == group && it.name == name
        }
    }
}
