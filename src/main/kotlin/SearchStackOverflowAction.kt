package com.example

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.ide.BrowserUtil
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SearchStackOverflowAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val query = resolveQuery(editor, e.project)

        if (query.isNullOrBlank()) {
            Messages.showInfoMessage(
                e.project,
                "Select an error message or place the caret on an error to search Stack Overflow.",
                "Stack Overflow Search"
            )
            return
        }

        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        BrowserUtil.browse("https://stackoverflow.com/search?q=$encoded")
    }

    private fun resolveQuery(editor: Editor, project: Project?): String? {
        val selected = editor.selectionModel.selectedText
        if (!selected.isNullOrBlank()) return selected.trim()

        project ?: return null

        val offset = editor.caretModel.offset
        val infos = ReadAction.compute<List<com.intellij.codeInsight.daemon.impl.HighlightInfo>, RuntimeException> {
            DaemonCodeAnalyzerImpl.getHighlights(editor.document, HighlightSeverity.WARNING, project)
        }

        return infos
            .filter { it.startOffset <= offset && offset <= it.endOffset }
            .mapNotNull { it.description }
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
