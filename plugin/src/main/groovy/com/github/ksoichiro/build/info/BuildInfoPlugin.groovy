package com.github.ksoichiro.build.info

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
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
        def branch
        def commit
        def committerDate
        try {
            Grgit grgit = Grgit.open()
            branch = grgit.branch.current.name
            Commit head = grgit.log(maxCommits: 1)[0]
            commit = head.abbreviatedId
            committerDate = head.date.format("yyyy-MM-dd HH:mm:ss Z")
        } catch (ignored) {
            branch = "unknown"
            commit = "unknown"
            committerDate = "unknown"
        }

        attributes["Git-Branch"] = branch
        attributes["Git-Commit"] = commit
        attributes["Git-Committer-Date"] = committerDate
        attributes["Build-Date"] = new Date().format("yyyy-MM-dd HH:mm:ss Z")

        (project.tasks.jar as Jar).manifest {
            it.attributes(attributes)
        }
    }
}
