package com.example

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

private val GENERIC_LANGUAGE_NAMES = setOf("TEXT", "Plain text", "UNKNOWN", "")

class SearchStackOverflowAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val language = resolveLanguage(e)
        val queries = resolveAllQueries(editor, project, language)

        if (queries.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "Select an error message or place the caret on an error to search Stack Overflow.",
                "Stack Overflow Search"
            )
            return
        }

        project.service<StackOverflowSearchService>().searchAll(queries)
        ToolWindowManager.getInstance(project).getToolWindow("Stack Overflow Search")?.show()
    }

    private fun resolveLanguage(e: AnActionEvent): String? {
        val psiLang = e.getData(CommonDataKeys.PSI_FILE)?.language?.displayName
        if (psiLang != null && psiLang !in GENERIC_LANGUAGE_NAMES) return psiLang
        val fileTypeName = e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType?.name
        return fileTypeName?.takeIf { it !in GENERIC_LANGUAGE_NAMES }
    }

    private fun resolveAllQueries(editor: Editor, project: Project, language: String?): List<String> {
        val selected = editor.selectionModel.selectedText
        if (!selected.isNullOrBlank()) {
            val q = buildQuery(ErrorQueryPreprocessor.preprocess(selected), language)
            return if (q.isNotBlank()) listOf(q) else emptyList()
        }

        val infos = ReadAction.compute<List<com.intellij.codeInsight.daemon.impl.HighlightInfo>, RuntimeException> {
            DaemonCodeAnalyzerImpl.getHighlights(editor.document, HighlightSeverity.WARNING, project)
        }

        return infos
            .mapNotNull { it.description }
            .distinct()
            .map { buildQuery(ErrorQueryPreprocessor.preprocess(it), language) }
            .filter { it.isNotBlank() }
    }

    private fun buildQuery(raw: String, language: String?) =
        if (language != null) "$language $raw" else raw

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
