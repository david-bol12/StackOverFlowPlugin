package com.example

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

class BuildErrorListener(private val project: Project) : CompilationStatusListener {
    override fun compilationFinished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
        if (aborted || errors == 0) return

        val firstError = compileContext.getMessages(CompilerMessageCategory.ERROR)
            .firstOrNull()?.message ?: return

        ApplicationManager.getApplication().invokeLater {
            project.service<StackOverflowSearchService>().updateError(firstError)
            ToolWindowManager.getInstance(project).getToolWindow("Stack Overflow Search")?.show()
        }
    }
}
