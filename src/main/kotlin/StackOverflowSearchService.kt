package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class StackOverflowSearchService {
    var lastBuildError by mutableStateOf("")
        private set

    fun updateError(error: String) {
        lastBuildError = error
    }
}
