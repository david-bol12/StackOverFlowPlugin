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

@Service(Service.Level.PROJECT)
class StackOverflowSearchService {
    private val LOG = Logger.getInstance(StackOverflowSearchService::class.java)

    var queries = mutableStateListOf<String>()
        private set
    var currentIndex by mutableStateOf(0)
        private set

    private val allResults = mutableStateMapOf<Int, SnapshotStateList<SearchResult>>()
    private val allErrors = mutableStateMapOf<Int, String?>()
    private val loadingSet = mutableStateListOf<Int>()

    val loadingAnswers = mutableStateListOf<Long>()

    val currentQuery get() = queries.getOrElse(currentIndex) { "" }
    val results: List<SearchResult> get() = allResults[currentIndex] ?: emptyList()
    val isLoading get() = currentIndex in loadingSet
    val searchError get() = allErrors[currentIndex]

    fun search(query: String) = searchAll(listOf(query))

    fun searchAll(newQueries: List<String>) {
        if (newQueries.isEmpty()) return
        LOG.info("searchAll() with ${newQueries.size} queries")
        queries.clear()
        allResults.clear()
        allErrors.clear()
        loadingSet.clear()
        currentIndex = 0
        queries.addAll(newQueries)
        newQueries.forEachIndexed { i, q -> loadAt(i, q) }
    }

    fun navigateTo(index: Int) {
        currentIndex = index.coerceIn(0, queries.size - 1)
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
