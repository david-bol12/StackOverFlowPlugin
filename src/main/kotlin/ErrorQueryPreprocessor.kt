package com.example

object ErrorQueryPreprocessor {

    private val libraryPrefixes = listOf(
        "java.", "javax.", "jakarta.",
        "kotlin.", "kotlinx.",
        "scala.",
        "org.jetbrains.", "org.intellij.",
        "org.springframework.",
        "com.google.",
        "io.netty.", "io.ktor.",
        "org.apache.",
        "sun.", "jdk.", "com.sun.",
        "android.", "androidx.",
        "org.junit.", "junit.",
    )

    // Matches: "   at com.example.Foo.bar(Foo.kt:42)"
    private val stackFrameRegex = Regex("""^(\s*at\s+)([\w${'$'}.]+)\(([^)]+):(\d+)\)(.*)$""")

    // Matches: "/path/to/File.kt:42:10: error: message" or "File.kt:42: error: message"
    private val compilerErrorRegex = Regex("""^(.+?):(\d+)(?::\d+)?:\s*(error|warning|note):\s*(.+)$""")

    fun preprocess(raw: String): String =
        raw.lines()
            .map { processLine(it.trim()) }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun processLine(line: String): String {
        // Drop "location: ..." lines — they describe the call site, not the error
        if (line.startsWith("location:")) return ""

        // "symbol:   method foo(String)" → "method foo" (strip type signatures)
        val symbolMatch = Regex("^symbol:\\s+(.+)$").matchEntire(line)
        if (symbolMatch != null) {
            return symbolMatch.groupValues[1].replace(Regex("\\(.*\\)"), "").trim()
        }

        stackFrameRegex.matchEntire(line)?.let { m ->
            val prefix = m.groupValues[1]
            val className = m.groupValues[2]
            val fileName = m.groupValues[3]
            val rest = m.groupValues[5]
            return if (isLibraryClass(className)) line
            else "$prefix$className($fileName)$rest"
        }

        compilerErrorRegex.matchEntire(line)?.let { m ->
            val severity = m.groupValues[3]
            val message = m.groupValues[4]
            return "$severity: $message"
        }

        // Strip leading "java: " compiler prefix
        return line.removePrefix("java: ")
    }

    private fun isLibraryClass(className: String) =
        libraryPrefixes.any { className.startsWith(it) }
}
