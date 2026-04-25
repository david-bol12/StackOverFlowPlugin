# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Stack Overflow Error Search** is an IntelliJ IDEA plugin that lets developers search Stack Overflow for errors without leaving the IDE. It provides:

- An editor action (**Search Stack Overflow**, `Ctrl+Shift+F1`) that extracts the selected text or the error/warning under the caret and opens an SO search in the browser.
- A **Stack Overflow Search** tool window (bottom bar) with a text field for manual error paste + search.

## Build & Run

```bash
./gradlew buildPlugin          # Build distributable plugin ZIP → build/distributions/
./gradlew runIde               # Launch a sandboxed IntelliJ instance with the plugin loaded
./gradlew verifyPlugin         # Validate plugin descriptor and API compatibility
./gradlew test                 # Run tests
./gradlew test --tests "com.example.MyTest"  # Run a single test class
```

Config values (versions, build numbers) live in `gradle.properties`. The version catalog is at `gradle/libs.versions.toml`.

## Stack

| Layer | Technology |
|---|---|
| Build | Gradle 9.2.1, IntelliJ Platform Gradle Plugin 2.10.5 |
| Language | Kotlin 2.2.20 + Java 21 |
| UI | Jewel (JetBrains Compose for IntelliJ) |
| Target IDE | IntelliJ IDEA 2025.3.1, build 253+ |

## Architecture

```
src/main/kotlin/
  SearchStackOverflowAction.kt   # AnAction: extracts error → opens SO in browser
  MyToolWindowFactory.kt         # ToolWindowFactory: Compose search panel (bottom tool window)
  MyMessageBundle.kt             # i18n message bundle helper

src/main/resources/
  META-INF/plugin.xml            # Plugin descriptor: action + tool window registrations
  messages/MyMessageBundle.properties
```

### How the editor action works

`SearchStackOverflowAction.resolveQuery()` tries two sources in order:
1. **Selected text** — if the user has highlighted text, use it as-is.
2. **Error at caret** — calls `DaemonCodeAnalyzerImpl.getHighlights()` (internal API, read-action guarded) to find WARNING+ highlights that contain the caret offset, then joins their descriptions.

The resolved query is URL-encoded and opened via `BrowserUtil.browse()`.

### Key IntelliJ Platform APIs used

- `DaemonCodeAnalyzerImpl.getHighlights()` — fetch in-editor error/warning annotations (`@ApiStatus.Internal`; must be called under a read action)
- `BrowserUtil.browse()` — cross-platform browser launch
- `org.jetbrains.jewel.bridge.addComposeTab` — add a Compose-based tab to a tool window
