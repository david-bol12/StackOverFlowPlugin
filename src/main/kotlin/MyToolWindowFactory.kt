package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import androidx.compose.foundation.border

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
        val q = service.currentQuery
        if (q.isNotBlank()) queryState.setTextAndPlaceCursorAtEnd(q)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextArea(
            state = queryState,
            modifier = Modifier.fillMaxWidth().height(90.dp),
            placeholder = { Text("Paste an error or select text in editor…") }
        )
        OutlinedButton(
            onClick = { service.search(queryState.text.toString()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = queryState.text.isNotEmpty() && !service.isLoading
        ) {
            Text(if (service.isLoading) "Searching…" else "Search Stack Overflow")
        }

        service.searchError?.let { Text("Error: $it") }

        if (!service.isLoading && service.currentQuery.isNotBlank() && service.results.isEmpty() && service.searchError == null) {
            Text("No results found.")
        }

        service.results.forEach { result ->
            ResultCard(result)
        }
    }
}

@Composable
private fun ResultCard(result: SearchResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.35f)), RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(result.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        val body = result.answerBody
        if (body != null) {
            val preview = if (body.length > 400) body.take(400).trimEnd() + "…" else body
            Text(preview, fontSize = 12.sp)
        } else {
            Text(result.excerpt, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Score: ${result.score}", fontSize = 11.sp)
            Text("Answers: ${result.answerCount}", fontSize = 11.sp)
        }
        OutlinedButton(onClick = { BrowserUtil.browse(result.link) }) {
            Text("View on Stack Overflow")
        }
    }
}
