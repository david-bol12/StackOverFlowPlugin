package com.example

data class SearchResult(
    val questionId: Long,
    val title: String,
    val excerpt: String,
    val score: Int,
    val answerCount: Int,
    val link: String,
    val answerBody: String? = null,
)
