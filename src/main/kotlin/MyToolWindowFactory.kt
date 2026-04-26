package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.UIManager as SwingUIManager
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class MyToolWindowFactory : ToolWindowFactory {
    private var jcefBrowser: JBCefBrowser? = null

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val openUrl: (String) -> Unit = { url -> openInBuiltinBrowser(toolWindow, url) }
        toolWindow.addComposeTab("Stack Overflow Search", focusOnClickInside = true) {
            StackOverflowSearchPanel(project, openUrl)
        }
    }

    private fun openInBuiltinBrowser(toolWindow: ToolWindow, url: String) {
        if (!JBCefApp.isSupported()) {
            BrowserUtil.browse(url)
            return
        }
        val cm = toolWindow.contentManager
        val existing = cm.findContent("Browser")
        if (existing != null) {
            jcefBrowser?.loadURL(url)
            cm.setSelectedContent(existing)
            return
        }

        val browser = JBCefBrowser(url)
        jcefBrowser = browser

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(cefBrowser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain != true) return
                val bg = SwingUIManager.getColor("Panel.background")
                val isDark = bg != null &&
                    (bg.red * 299 + bg.green * 587 + bg.blue * 114) < 128_000
                val scheme = if (isDark) "dark" else "light"
                cefBrowser?.executeJavaScript(
                    "document.documentElement.setAttribute('data-color-scheme','$scheme');",
                    cefBrowser.url, 0
                )
            }
        }, browser.cefBrowser)

        val wrapper = JPanel(BorderLayout())
        val backBtn = JButton("← Back to Results").apply {
            addActionListener {
                cm.findContent("Stack Overflow Search")?.let { cm.setSelectedContent(it) }
            }
        }
        wrapper.add(backBtn, BorderLayout.NORTH)
        wrapper.add(browser.component, BorderLayout.CENTER)

        val content = cm.factory.createContent(wrapper, "Browser", false)
        content.isCloseable = true
        cm.addContent(content)
        cm.setSelectedContent(content)
    }
}

@Composable
private fun StackOverflowSearchPanel(project: Project, openUrl: (String) -> Unit) {
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
        if (service.queries.size > 1) {
            ErrorNavigationBar(service)
        }

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
            ResultCard(result, openUrl)
        }
    }
}

@Composable
private fun ErrorNavigationBar(service: StackOverflowSearchService) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { service.navigateTo(service.currentIndex - 1) },
            enabled = service.currentIndex > 0
        ) { Text("←") }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val base = JewelTheme.defaultTextStyle.fontSize
            Text(
                "Error ${service.currentIndex + 1} / ${service.queries.size}",
                fontSize = base,
                fontWeight = FontWeight.SemiBold
            )
            if (service.isLoading) Text("Loading…", fontSize = base * 0.85f)
        }

        OutlinedButton(
            onClick = { service.navigateTo(service.currentIndex + 1) },
            enabled = service.currentIndex < service.queries.size - 1
        ) { Text("→") }
    }
}

@Composable
private fun ResultCard(result: SearchResult, openUrl: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.35f)), RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val base = JewelTheme.defaultTextStyle.fontSize
        Text(result.title, fontWeight = FontWeight.SemiBold, fontSize = base)
        val body = result.answerBody
        if (body != null) {
            val preview = if (body.length > 400) body.take(400).trimEnd() + "…" else body
            Text(preview, fontSize = base * 0.92f)
        } else {
            Text(result.excerpt, fontSize = base * 0.92f)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Score: ${result.score}", fontSize = base * 0.85f)
            Text("Answers: ${result.answerCount}", fontSize = base * 0.85f)
        }
        OutlinedButton(onClick = { openUrl(result.link) }) {
            Text("View on Stack Overflow")
        }
    }
}
