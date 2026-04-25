package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    LaunchedEffect(service.lastBuildError) {
        val error = service.lastBuildError
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
            onClick = {
                val query = queryState.text.toString()
                if (query.isNotBlank()) {
                    val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
                    BrowserUtil.browse("https://stackoverflow.com/search?q=$encoded")
                }
            },
            enabled = queryState.text.isNotEmpty()
        ) {
            Text("Search Stack Overflow")
        }
    }
}
