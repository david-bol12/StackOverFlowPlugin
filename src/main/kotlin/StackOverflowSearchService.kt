package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger

@Service(Service.Level.PROJECT)
class StackOverflowSearchService {
    private val LOG = Logger.getInstance(StackOverflowSearchService::class.java)
    var currentQuery by mutableStateOf("")
        private set
    var results = mutableStateListOf<SearchResult>()
    var isLoading by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)
    val loadingAnswers = mutableStateListOf<Long>()

    fun search(query: String) {
        if (query.isBlank()) {
            LOG.warn("search() called with blank query, ignoring")
            return
        }
        LOG.info("search() starting for query: $query")
        currentQuery = query
        isLoading = true
        searchError = null
        results.clear()

        ApplicationManager.getApplication().executeOnPooledThread {
            LOG.info("search() executing on pooled thread")
            val outcome = runCatching { StackOverflowApiClient.search(query) }
            LOG.info("search() API call finished, success=${outcome.isSuccess}")
            ApplicationManager.getApplication().invokeLater {
                outcome
                    .onSuccess { hits ->
                        LOG.info("search() got ${hits.size} results")
                        results.addAll(hits)
                        isLoading = false
                    }
                    .onFailure { err ->
                        LOG.warn("search() failed", err)
                        searchError = err.message ?: "Search failed"
                        isLoading = false
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
