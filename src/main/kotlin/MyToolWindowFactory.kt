package com.example

import org.jetbrains.jewel.ui.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import java.awt.datatransfer.StringSelection

class MyToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("Stack Overflow Search", focusOnClickInside = true) {
            StackOverflowSearchPanel(project)
        }
    }
}

@Composable
private fun StackOverflowSearchPanel(project: Project) {
    val service = project.service<StackOverflowSearchService>()
    val queryState = rememberTextFieldState()

    LaunchedEffect(service.currentQuery) {
        val error = service.currentQuery
        if (error.isNotBlank()) {
            queryState.setTextAndPlaceCursorAtEnd(error)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Error message (auto-populated on build failure):")
        TextArea(
            state = queryState,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("e.g. NullPointerException: Cannot invoke method on null object") }
        )
        OutlinedButton(
            onClick = { service.search(queryState.text.toString()) },
            enabled = queryState.text.isNotEmpty() && !service.isLoading
        ) {
            Text(if (service.isLoading) "Searching..." else "Search Stack Overflow")
        }

        service.searchError?.let { err ->
            Text("Error: $err")
        }

        if (!service.isLoading && service.currentQuery.isNotBlank() && service.results.isEmpty() && service.searchError == null) {
            Text("No results found.")
        }

        if (service.results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(service.results) { result ->
                    ResultCard(result, service)
                    Divider(orientation = Orientation.Horizontal, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: SearchResult, service: StackOverflowSearchService) {
    var copied by remember(result.questionId) { mutableStateOf(false) }
    val isLoadingAnswer = service.loadingAnswers.contains(result.questionId)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(result.title)
        Text(result.excerpt)
        Text("Score: ${result.score}  |  Answers: ${result.answerCount}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { BrowserUtil.browse(result.link) }) {
                Text("Open in Browser")
            }
            OutlinedButton(
                onClick = {
                    service.copyTopAnswer(result.questionId) { text ->
                        if (text != null) {
                            CopyPasteManager.getInstance().setContents(StringSelection(text))
                            copied = true
                        }
                    }
                },
                enabled = !isLoadingAnswer
            ) {
                Text(
                    when {
                        isLoadingAnswer -> "Loading..."
                        copied -> "Copied!"
                        else -> "Copy Solution"
                    }
                )
            }
        }
    }
}
