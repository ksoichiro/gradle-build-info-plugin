package com.github.ksoichiro.build.info

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

class BuildInfoPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        // Check if java plugin is applied. If not, we do nothing but print warning.
        target.afterEvaluate { Project project ->
            if (project.plugins.hasPlugin('java')) {
                mergeManifest(project)
            } else {
                println "'java' plugin is not applied. Please check your gradle script. The plugin com.github.ksoichiro.build.info only works when 'java' plugin is applied."
            }
        }
    }

    static void mergeManifest(Project project) {
        def attributes = [:] as Map<String, ?>
        def commit = null
        def committedAt = null
        try {
            commit = "git rev-parse --short HEAD".execute().text.trim()
            committedAt = "git log -n 1 --format=\"%ci\"".execute().text.trim().drop(1)[0..-2]
        } catch (ignored) {
        }

        attributes["Git-Commit"] = commit
        attributes["Committed-At"] = committedAt
        attributes["Built-At"] = new Date().format("yyyy-MM-dd HH:mm:ss Z")

        (project.tasks.jar as Jar).manifest {
            it.attributes(attributes)
        }
    }
}
