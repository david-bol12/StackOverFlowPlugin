package com.example

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.openapi.project.Project

/**
 * Entry point for CLion build events. Registered in plugin.xml under the
 * [BuildProgressListener] project-listener topic.
 *
 * Delegates entirely to [ClionBuildAdapter] via [IdeBuildAdapterFactory].
 * In IntelliJ IDEA the factory returns [IdeaBuildAdapter], the cast to
 * [ClionBuildAdapter] yields null, and every event is silently ignored.
 */
class ClionBuildProgressListener(private val project: Project) : BuildProgressListener {
    private val adapter = IdeBuildAdapterFactory.create(project) as? ClionBuildAdapter

    override fun onEvent(buildId: Any, event: BuildEvent) {
        adapter?.onEvent(buildId, event)
    }
}
