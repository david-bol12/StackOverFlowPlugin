package com.example

import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.StartBuildEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import com.intellij.openapi.wm.ToolWindowManager
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Handles CLion CMake/ninja/make build errors via [BuildProgressListener] events.
 *
 * Errors accumulate across the build and are flushed as a batch when
 * [FinishBuildEvent] arrives, mirroring the single-shot behaviour of
 * [IdeaBuildAdapter.onCompilationFinished].
 */
class ClionBuildAdapter(private val project: Project) : IdeBuildAdapter {
    override val isActive: Boolean get() = PlatformUtils.isCLion()

    private val pendingErrors = CopyOnWriteArrayList<String>()

    fun onEvent(buildId: Any, event: BuildEvent) {
        if (!isActive) return
        when (event) {
            is StartBuildEvent  -> pendingErrors.clear()
            is MessageEvent     -> if (event.kind == MessageEvent.Kind.ERROR) {
                val query = ErrorQueryPreprocessor.preprocess(event.message)
                if (query.isNotBlank()) pendingErrors.add(query)
            }
            is FinishBuildEvent -> flushErrors()
        }
    }

    private fun flushErrors() {
        val queries = pendingErrors.distinct()
        pendingErrors.clear()
        if (queries.isEmpty()) return
        ApplicationManager.getApplication().invokeLater {
            project.service<StackOverflowSearchService>().searchAll(queries)
            ToolWindowManager.getInstance(project).getToolWindow("Stack Overflow Search")?.show()
        }
    }
}
