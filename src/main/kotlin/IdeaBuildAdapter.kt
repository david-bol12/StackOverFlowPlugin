package com.example

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import com.intellij.openapi.wm.ToolWindowManager

/** Handles IntelliJ IDEA Java/Kotlin build errors via [CompileContext]. */
class IdeaBuildAdapter(private val project: Project) : IdeBuildAdapter {
    override val isActive: Boolean get() = !PlatformUtils.isCLion()

    fun onCompilationFinished(aborted: Boolean, errors: Int, compileContext: CompileContext) {
        if (!isActive || aborted || errors == 0) return

        data class Entry(val query: String, val location: ErrorLocation?)

        val entries = compileContext.getMessages(CompilerMessageCategory.ERROR)
            .mapNotNull { msg ->
                val q = ErrorQueryPreprocessor.preprocess(msg.message)
                if (q.isBlank()) return@mapNotNull null
                val nav = msg.navigatable
                    ?: msg.virtualFile?.let { OpenFileDescriptor(project, it, 0) }
                Entry(q, if (nav != null) ErrorLocation(nav) else null)
            }
            .distinctBy { it.query }

        if (entries.isEmpty()) return

        ApplicationManager.getApplication().invokeLater {
            project.service<StackOverflowSearchService>().searchAll(
                entries.map { it.query },
                entries.map { it.location }
            )
            ToolWindowManager.getInstance(project).getToolWindow("Stack Overflow Search")?.show()
        }
    }
}
