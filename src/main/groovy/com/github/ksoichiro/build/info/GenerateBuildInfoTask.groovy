package com.github.ksoichiro.build.info

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

class GenerateBuildInfoTask extends DefaultTask {
    public static final String NAME = 'generateBuildInfo'
    BuildInfoExtension extension
    GitInfo gitInfo
    File propsFile
    boolean hasDependency

    GenerateBuildInfoTask() {
        project.afterEvaluate {
            extension = project.extensions.buildInfo
            if (project.plugins.hasPlugin(JavaPlugin)) {
                dependsOn(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
                getClassesTask().dependsOn(NAME)
            }

            // Should detect...
            //   changes of git commit ids: input=commit-id, output=file
            //   changes of existence of .git directory: input=existence-flag, output=file
            // Should ignore...
            //   changes when git status is dirty: input=commit-id, output=file
            gitInfo = readGitInfo()
            File buildResourcesDir = getBuildResourcesDir()
            propsFile = new File(buildResourcesDir, 'git.properties')
            getInputs().property('valid', gitInfo.valid)
            getInputs().property('commit', gitInfo.commit)
            getOutputs().file(propsFile)
        }
    }

    @TaskAction
    void exec() {
        hasDependency = hasDependency(project, 'org.springframework.boot', 'spring-boot-starter-actuator')
        validate()
        generateGitProperties()
        mergeManifest()
    }

    Task getClassesTask() {
        project.tasks.findByName(JavaPlugin.CLASSES_TASK_NAME)
    }

    File getBuildResourcesDir() {
        ProcessResources processResources = getProcessResourcesTask()
        if (processResources) {
            return processResources.destinationDir
        } else if (extension.destinationDir) {
            return extension.destinationDir
        } else {
            // Cannot determine destination
            throw new GradleException("Could not determine destination directory. You must apply Java plugin or set buildInfo.destinationDir.")
        }
    }

    ProcessResources getProcessResourcesTask() {
        (ProcessResources) project.tasks.findByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
    }

    void generateGitProperties() {
        if (!extension.gitPropertiesEnabled && !hasDependency) {
            return
        }
        if (gitInfo.missing && extension.warnIfGitDirectoryIsMissing) {
            logger.warn "Could not read .git directory. git.properties will not be generated or will include invalid values."
        }
        if (!gitInfo.valid) {
            return
        }
        if (!propsFile.parentFile.exists()) {
            propsFile.parentFile.mkdirs()
        }
        def map = [
            'git.branch'     : gitInfo.branch,
            'git.commit.id'  : gitInfo.commit,
            'git.commit.time': gitInfo.committerDate]

        // Content of git.properties should always be the same
        // if it's generated for the same commit.
        // However, Properties class uses Hashtable internally and output timestamp,
        // which produces inconsistent content for the same commit.
        // To avoid this, we can just write file by ourselves, but Properties also
        // has "escape" feature and it's provided as private method.
        // With Groovy, private methods can be accessed from outside of a class,
        // but it's not a feature but just an unresolved issue,
        // so this behaviour might be changed in the future.
        // https://issues.apache.org/jira/browse/GROOVY-1875
        // Therefore we write the properties file with Properties class first
        // to create "valid" .properties file, then read it, manipulate it, and save it again.
        def props = new Properties()
        props.putAll(map)
        props.store(propsFile.newWriter(), null)
        def buffer = []
        propsFile.eachLine {
            // Timestamp line should be discarded
            if (!it.startsWith("#")) {
                buffer << it
            }
        }
        buffer.sort(true)
        propsFile.withWriter { w ->
            buffer.each {
                w.println it
            }
        }
    }

    void mergeManifest() {
        if (!project.plugins.hasPlugin(JavaPlugin) || !extension.manifestEnabled) {
            return
        }

        def attributes = [:] as Map<String, ?>

        extension.with {
            if (gitInfo.missing && extension.warnIfGitDirectoryIsMissing) {
                logger.warn "Could not read .git directory. Git info will not be included in the manifest or will be replaced to invalid values."
            }
            if (gitInfo.valid) {
                if (attributeGitBranchEnabled) {
                    attributes["Git-Branch"] = gitInfo.branch
                }
                if (attributeGitCommitEnabled) {
                    attributes["Git-Commit"] = gitInfo.commit
                }
                if (attributeGitCommitterDateEnabled) {
                    attributes["Git-Committer-Date"] = gitInfo.committerDate
                }
            }
            if (attributeBuildDateEnabled) {
                attributes["Build-Date"] = new Date().format(extension.buildDateFormat)
            }
            if (attributeBuildJavaVersionEnabled) {
                attributes["Build-Java-Version"] = System.properties['java.version']
            }
            if (attributeBuildJavaVendorEnabled) {
                attributes["Build-Java-Vendor"] = System.properties['java.vendor']
            }
            if (attributeBuildOsNameEnabled) {
                attributes["Build-Os-Name"] = System.properties['os.name']
            }
            if (attributeBuildOsVersionEnabled) {
                attributes["Build-Os-Version"] = System.properties['os.version']
            }
        }

        (project.tasks.jar as Jar).manifest {
            it.attributes(attributes)
        }
    }

    GitInfo readGitInfo() {
        def missing = false
        def valid = true
        def branch
        def commit
        def committerDate
        try {
            Grgit grgit = Grgit.open(currentDir: project.projectDir)
            branch = grgit.branch.current.name
            Commit head = grgit.head()
            commit = head.abbreviatedId
            committerDate = head.date.format(extension.committerDateFormat)
        } catch (ignored) {
            missing = true
            // When MODE_IGNORE is used, we skip outputting related info
            valid = extension.gitInfoMode != BuildInfoExtension.MODE_IGNORE
            branch = "unknown"
            commit = "unknown"
            committerDate = "unknown"
        }
        new GitInfo(missing: missing,
            valid: valid,
            branch: branch,
            commit: commit,
            committerDate: committerDate)
    }

    def validate() {
        // If all options are disabled, skip this task not to cache the result
        if (!extension.gitPropertiesEnabled && !hasDependency && !extension.manifestEnabled) {
            throw new StopExecutionException()
        }
        if (gitInfo.missing) {
            switch (extension.gitInfoMode) {
                case BuildInfoExtension.MODE_ERROR:
                    throw new GradleException("Cannot read .git directory.")
                case BuildInfoExtension.MODE_IGNORE:
                case BuildInfoExtension.MODE_DEFAULT:
                default:
                    break
            }
        }
    }

    static boolean hasDependency(Project project, String group, String name) {
        if (!project.plugins.hasPlugin(JavaPlugin)) {
            return false
        }
        project.configurations.compile.dependencies.any {
            it.group == group && it.name == name
        } || project.configurations.compile.incoming.resolutionResult.allComponents.findAll {
            it.getId() instanceof ModuleComponentIdentifier
        }.collect {
            it.getId() as ModuleComponentIdentifier
        }.any {
            it.group == group && it.module == name
        }
    }
}
