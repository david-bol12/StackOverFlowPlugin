package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable

data class ErrorLocation(
    val navigatable: Navigatable?,
    val startOffset: Int = -1,
    val endOffset: Int = -1
)

@Service(Service.Level.PROJECT)
class StackOverflowSearchService(private val project: Project) {
    private val LOG = Logger.getInstance(StackOverflowSearchService::class.java)

    var queries = mutableStateListOf<String>()
        private set
    var currentIndex by mutableStateOf(0)
        private set

    private val allResults = mutableStateMapOf<Int, SnapshotStateList<SearchResult>>()
    private val allErrors = mutableStateMapOf<Int, String?>()
    private val loadingSet = mutableStateListOf<Int>()
    private val locationList = mutableListOf<ErrorLocation?>()

    val loadingAnswers = mutableStateListOf<Long>()

    val currentQuery get() = queries.getOrElse(currentIndex) { "" }
    val results: List<SearchResult> get() = allResults[currentIndex] ?: emptyList()
    val isLoading get() = currentIndex in loadingSet
    val searchError get() = allErrors[currentIndex]

    fun search(query: String) = searchAll(listOf(query))

    fun searchAll(newQueries: List<String>, newLocations: List<ErrorLocation?> = emptyList()) {
        if (newQueries.isEmpty()) return
        LOG.info("searchAll() with ${newQueries.size} queries")
        queries.clear()
        allResults.clear()
        allErrors.clear()
        loadingSet.clear()
        locationList.clear()
        currentIndex = 0
        queries.addAll(newQueries)
        repeat(newQueries.size) { i -> locationList.add(newLocations.getOrNull(i)) }
        newQueries.forEachIndexed { i, q -> loadAt(i, q) }
    }

    fun navigateTo(index: Int) {
        currentIndex = index.coerceIn(0, queries.size - 1)
        val loc = locationList.getOrNull(currentIndex) ?: return
        ApplicationManager.getApplication().invokeLater {
            loc.navigatable?.navigate(false)
            if (loc.startOffset >= 0 && loc.endOffset > loc.startOffset) {
                FileEditorManager.getInstance(project).selectedTextEditor?.apply {
                    selectionModel.setSelection(loc.startOffset, loc.endOffset)
                    scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }
        }
    }

    private fun loadAt(index: Int, query: String) {
        loadingSet.add(index)
        allErrors[index] = null
        allResults[index] = mutableStateListOf()
        ApplicationManager.getApplication().executeOnPooledThread {
            LOG.info("loadAt($index) for: $query")
            val outcome = runCatching { StackOverflowApiClient.search(query) }
            ApplicationManager.getApplication().invokeLater {
                loadingSet.remove(index)
                outcome
                    .onSuccess { hits ->
                        LOG.info("loadAt($index) got ${hits.size} results")
                        allResults[index]?.addAll(hits)
                    }
                    .onFailure { err ->
                        LOG.warn("loadAt($index) failed", err)
                        allErrors[index] = err.message ?: "Search failed"
                    }
            }
        }
    }

    fun copyTopAnswer(questionId: Long, onResult: (String?) -> Unit) {
        if (loadingAnswers.contains(questionId)) return
        loadingAnswers.add(questionId)
        ApplicationManager.getApplication().executeOnPooledThread {
            val text = runCatching { StackOverflowApiClient.fetchTopAnswerText(questionId) }.getOrNull()
            ApplicationManager.getApplication().invokeLater {
                loadingAnswers.remove(questionId)
                onResult(text)
            }
        }
    }
}
