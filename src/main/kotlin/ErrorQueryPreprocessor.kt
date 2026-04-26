package com.example

import com.intellij.util.PlatformUtils

// ---------------------------------------------------------------------------
// Strategy interface — one implementation per language / platform family.
// To add a new language: add a new private object below and one branch in
// ErrorQueryPreprocessor.strategy. Nothing else needs to change.
// ---------------------------------------------------------------------------

private interface PreprocessorStrategy {
    fun processLine(line: String): String
}

// ---------------------------------------------------------------------------
// Java / Kotlin strategy (IntelliJ IDEA)
// All original logic lives here, completely unchanged.
// ---------------------------------------------------------------------------

private object JavaKotlinStrategy : PreprocessorStrategy {

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

    override fun processLine(line: String): String {
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

// ---------------------------------------------------------------------------
// C / C++ strategy (CLion)
// ---------------------------------------------------------------------------

private object CppStrategy : PreprocessorStrategy {
    private val hexAddressRegex  = Regex("""0x[0-9a-fA-F]+""")
    // Strips simple (non-nested) template argument lists, e.g. std::vector<int>
    private val templateArgsRegex = Regex("""<[^<>]*>""")

    override fun processLine(line: String): String {
        // Drop "note:" lines — they describe related context, not the error itself
        if (line.startsWith("note:")) return ""
        return line
            .replace(hexAddressRegex, "")    // strip memory addresses
            .replace(templateArgsRegex, "")  // strip template arguments
            .trim()
    }
}

// ---------------------------------------------------------------------------
// Python strategy (PyCharm)
// ---------------------------------------------------------------------------

private object PythonStrategy : PreprocessorStrategy {
    // "  File "/path/to/foo.py", line 42, in bar" — drop; exception line is more useful
    private val fileLineRegex = Regex("""^\s*File ".+", line \d+""")
    private val ansiRegex     = Regex("\\[[0-9;]*m")
    // pytest "E   SomeError: message" — strip the E prefix
    private val pytestERegex  = Regex("""^E\s{3}(.+)$""")

    override fun processLine(line: String): String {
        val clean = ansiRegex.replace(line.trim(), "")
        if (clean == "Traceback (most recent call last):") return ""
        if (clean.startsWith("During handling of the above exception")) return ""
        if (clean.startsWith("The above exception was the direct cause")) return ""
        if (fileLineRegex.containsMatchIn(clean)) return ""
        // Drop indented source-code context lines that carry no exception info
        if (line.startsWith("    ") && !clean.contains(": ")) return ""
        pytestERegex.matchEntire(clean)?.let { m ->
            val content = m.groupValues[1]
            // Keep only lines that look like ExceptionType: message
            return if (content.contains(": ")) content else ""
        }
        return clean
    }
}

// ---------------------------------------------------------------------------
// Public façade — picks strategy at runtime; signature is unchanged.
// ---------------------------------------------------------------------------

object ErrorQueryPreprocessor {

    private val strategy: PreprocessorStrategy
        get() = when {
            PlatformUtils.isCLion()   -> CppStrategy
            PlatformUtils.isPyCharm() -> PythonStrategy
            else                      -> JavaKotlinStrategy
        }

    fun preprocess(raw: String): String =
        raw.lines()
            .map { strategy.processLine(it.trim()) }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
