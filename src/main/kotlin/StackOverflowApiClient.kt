package com.example

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

object StackOverflowApiClient {
    private val LOG = Logger.getInstance(StackOverflowApiClient::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .build()
    private const val BASE = "https://api.stackexchange.com/2.3"

    @Volatile private var bodyFilter: String? = null

    fun search(query: String): List<SearchResult> {
        val q = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val json = get("$BASE/search/excerpts?order=desc&sort=relevance&q=$q&site=stackoverflow")
        LOG.info("SO search response: $json")
        val root = JsonParser.parseString(json).asJsonObject
        val items = root.getAsJsonArray("items") ?: run {
            LOG.warn("SO response missing 'items': $json")
            return emptyList()
        }
        val results = items
            .take(3)
            .mapNotNull { el ->
                runCatching {
                    val obj = el.asJsonObject
                    val qId = obj.get("question_id").asLong
                    SearchResult(
                        questionId = qId,
                        title = obj.get("title").asString.decodeHtmlEntities().stripHtmlTags(),
                        excerpt = obj.get("excerpt").asString.decodeHtmlEntities().stripHtmlTags(),
                        score = obj.get("score")?.asInt ?: 0,
                        answerCount = obj.get("answer_count")?.asInt ?: 0,
                        link = obj.get("link")?.asString ?: "https://stackoverflow.com/q/$qId",
                    )
                }.onFailure { LOG.warn("Failed to parse SO item: $el", it) }.getOrNull()
            }

        if (results.isEmpty()) return results

        val topAnswers = fetchTopAnswersBatch(results.map { it.questionId })
        return results.map { it.copy(answerBody = topAnswers[it.questionId]) }
    }

    private fun fetchTopAnswersBatch(questionIds: List<Long>): Map<Long, String> {
        val ids = questionIds.joinToString(";")
        val filter = getBodyFilter()
        val json = runCatching {
            get("$BASE/questions/$ids/answers?order=desc&sort=votes&site=stackoverflow&filter=$filter")
        }.getOrElse { LOG.warn("Failed to fetch answers batch", it); return emptyMap() }

        val answersByQuestion = mutableMapOf<Long, Pair<Int, String>>()
        JsonParser.parseString(json).asJsonObject
            .getAsJsonArray("items")
            ?.forEach { el ->
                runCatching {
                    val obj = el.asJsonObject
                    val qId = obj.get("question_id").asLong
                    val score = obj.get("score")?.asInt ?: 0
                    val body = obj.get("body")?.asString?.htmlToPlainText() ?: return@runCatching
                    val current = answersByQuestion[qId]
                    if (current == null || score > current.first) {
                        answersByQuestion[qId] = score to body
                    }
                }.onFailure { LOG.warn("Failed to parse answer item: $el", it) }
            }
        return answersByQuestion.mapValues { it.value.second }
    }

    fun fetchTopAnswerText(questionId: Long): String? {
        val filter = getBodyFilter()
        val json = get("$BASE/questions/$questionId/answers?order=desc&sort=votes&site=stackoverflow&filter=$filter")
        return JsonParser.parseString(json).asJsonObject
            .getAsJsonArray("items")
            ?.firstOrNull()
            ?.asJsonObject?.get("body")?.asString
            ?.htmlToPlainText()
    }

    private fun getBodyFilter(): String = bodyFilter ?: synchronized(this) {
        bodyFilter ?: createBodyFilter().also { bodyFilter = it }
    }

    private fun createBodyFilter(): String = runCatching {
        val json = get("$BASE/filters/create?include=answer.body&base=default")
        JsonParser.parseString(json).asJsonObject
            .getAsJsonArray("items")
            ?.firstOrNull()
            ?.asJsonObject?.get("filter")?.asString
    }.getOrNull() ?: "default"

    private fun get(url: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(java.time.Duration.ofSeconds(15))
            .GET()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body()
    }
}

private fun String.stripHtmlTags(): String =
    replace(Regex("<[^>]+>"), " ").replace(Regex("\\s{2,}"), " ").trim()

private fun String.decodeHtmlEntities(): String =
    replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&#34;", "\"")
        .replace("&nbsp;", " ").replace("&#x27;", "'")

private fun String.htmlToPlainText(): String =
    replace(Regex("<pre[^>]*>\\s*<code[^>]*>(.*?)</code>\\s*</pre>", RegexOption.DOT_MATCHES_ALL)) {
        "\n\n```\n${it.groupValues[1].decodeHtmlEntities().trim()}\n```\n\n"
    }
    .replace(Regex("<code[^>]*>(.*?)</code>", RegexOption.DOT_MATCHES_ALL)) { "`${it.groupValues[1]}`" }
    .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "\n- ")
    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("</?p[^>]*>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("</?h[1-6][^>]*>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<[^>]+>"), "")
    .decodeHtmlEntities()
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()
