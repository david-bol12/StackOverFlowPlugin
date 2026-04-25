# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Stack Overflow Error Search** is an IntelliJ IDEA + CLion plugin that lets developers search Stack Overflow for errors without leaving the IDE. It provides:

- An editor action (**Search Stack Overflow**, `Ctrl+Shift+F1`) that extracts the selected text or the error/warning under the caret and opens an SO search in the browser.
- A **Stack Overflow Search** tool window with a text field for manual error paste + search, and arrow navigation between multiple errors.
- Automatic population of the tool window from build/compile errors.

## Build & Run

```bash
./gradlew buildPlugin                       # Build distributable plugin ZIP → build/distributions/
./gradlew buildPlugin -x buildSearchableOptions  # Build while IntelliJ is open (skips headless IDE launch)
./gradlew runIde                            # Launch a sandboxed IntelliJ instance with the plugin loaded
./gradlew verifyPlugin                      # Validate plugin descriptor and API compatibility
./gradlew test                              # Run tests
./gradlew test --tests "com.example.MyTest" # Run a single test class
```

> **Note:** `buildSearchableOptions` launches a headless IDE instance and fails if IntelliJ is already running.
> Use `-x buildSearchableOptions` when building with the IDE open.

Config values (versions, build numbers) live in `gradle.properties`. The version catalog is at `gradle/libs.versions.toml`.

## Stack

| Layer | Technology |
|---|---|
| Build | Gradle 9.2.1, IntelliJ Platform Gradle Plugin 2.10.5 |
| Language | Kotlin 2.2.20 + Java 21 |
| UI | Jewel (JetBrains Compose for IntelliJ) |
| Target IDEs | IntelliJ IDEA 2025.3.1 (build 253+), CLion 2025.3.1 |

## Architecture

```
src/main/kotlin/
  SearchStackOverflowAction.kt     # AnAction: extracts error → opens SO in browser
  MyToolWindowFactory.kt           # ToolWindowFactory: Compose search panel + error navigation
  MyMessageBundle.kt               # i18n message bundle helper
  StackOverflowSearchService.kt    # Project service: holds queries/results per error, navigation state
  StackOverflowApiClient.kt        # HTTP client for Stack Overflow API
  StackOverflowResult.kt           # Search result data class
  ErrorQueryPreprocessor.kt        # Cleans raw error text before searching (strategy pattern)

  # IDE-specific build error adapter pattern (see below)
  IdeBuildAdapter.kt               # Interface + documentation for the pattern
  IdeBuildAdapterFactory.kt        # Factory: picks IdeaBuildAdapter or ClionBuildAdapter at runtime
  IdeaBuildAdapter.kt              # IDEA: reads errors from CompileContext
  ClionBuildAdapter.kt             # CLion: accumulates errors from BuildProgressListener events
  BuildErrorListener.kt            # IDEA entry point (CompilationStatusListener → IdeaBuildAdapter)
  ClionBuildProgressListener.kt    # CLion entry point (BuildProgressListener → ClionBuildAdapter)

src/main/resources/
  META-INF/plugin.xml              # Base descriptor: Compose dep, CLion listener, action
  META-INF/plugin-idea.xml         # Optional IDEA-only descriptor: CompilationStatusListener
  messages/MyMessageBundle.properties
```

## IDE-Specific Build Error Adapter Pattern

The plugin runs in both IntelliJ IDEA and CLion from a single ZIP. Build error listening is handled by the **adapter pattern** gated on `PlatformUtils` at runtime.

### How it works

```
plugin.xml  ─────────────────────────────────────────────────────────
  (always loaded)         ClionBuildProgressListener  ──► ClionBuildAdapter
                                                               │ PlatformUtils.isCLion()
plugin-idea.xml           BuildErrorListener          ──► IdeaBuildAdapter
  (loaded only when                                         │ !PlatformUtils.isCLion()
   com.intellij.java                                        │
   is present)            IdeBuildAdapterFactory ──► picks one at runtime
```

- **`IdeBuildAdapter`** — marker interface; each adapter also implements its IDE's listener interface.
- **`IdeBuildAdapterFactory.create(project)`** — single `when` block; returns the right adapter.
- **`BuildErrorListener`** — thin `CompilationStatusListener`; casts factory result to `IdeaBuildAdapter`, ignores events in CLion (cast → null).
- **`ClionBuildProgressListener`** — thin `BuildProgressListener`; casts factory result to `ClionBuildAdapter`, ignores events in IDEA (cast → null).
- **`IdeaBuildAdapter`** — all original IDEA logic, unchanged. Reads errors from `CompileContext`.
- **`ClionBuildAdapter`** — accumulates `MessageEvent(kind=ERROR)` across the build, flushes on `FinishBuildEvent`.

### plugin.xml layout

- **`plugin.xml`** — always loaded. Declares `com.intellij.java` as **optional** (`config-file="plugin-idea.xml"`). Registers `ClionBuildProgressListener`.
- **`plugin-idea.xml`** — loaded only when `com.intellij.java` is present. Registers `BuildErrorListener`.

### Adding support for a new IDE

1. **Create `NewIdeBuildAdapter.kt`** — implement `IdeBuildAdapter`; add a `fun onSomeEvent(...)` method that contains the IDE-specific extraction logic. Guard the body with `if (!isActive) return`.
2. **Add one line to `IdeBuildAdapterFactory`** — e.g. `PlatformUtils.isRider() -> RiderBuildAdapter(project)`.
3. **Create `NewIdeBuildListener.kt`** — implement the IDE's listener interface; cast the factory result to `NewIdeBuildAdapter` and call its method.
4. **Register in `plugin.xml`** — add one `<listener>` entry under the `<!-- ADD NEW IDE -->` comment.

That is all. `IdeBuildAdapter.kt`, `IdeBuildAdapterFactory.kt`, `ErrorQueryPreprocessor.kt`, `StackOverflowSearchService.kt`, and all existing adapters stay untouched.

## ErrorQueryPreprocessor — Strategy Pattern

`ErrorQueryPreprocessor.preprocess(raw)` picks a `PreprocessorStrategy` at runtime:

| Strategy | Active when | Rules |
|---|---|---|
| `JavaKotlinStrategy` | `!PlatformUtils.isCLion()` | Stack frame normalisation, compiler error extraction, `location:`/`symbol:` handling |
| `CppStrategy` | `PlatformUtils.isCLion()` | Drop `note:` lines, strip `0x…` addresses, strip template arguments |

To add a new language: add a `private object NewLangStrategy : PreprocessorStrategy` block, add a branch in the `strategy` getter. No existing strategy is modified.

## Key IntelliJ Platform APIs used

- `DaemonCodeAnalyzerImpl.getHighlights()` — fetch in-editor error/warning annotations (`@ApiStatus.Internal`; must be called under a read action)
- `BrowserUtil.browse()` — cross-platform browser launch
- `org.jetbrains.jewel.bridge.addComposeTab` — add a Compose-based tab to a tool window
- `com.intellij.util.PlatformUtils` — runtime IDE detection (`isCLion()`, etc.)
- `com.intellij.build.BuildProgressListener` / `MessageEvent` — base-platform build events (IDEA + CLion)
- `com.intellij.openapi.compiler.CompilationStatusListener` — Java-plugin build events (IDEA only)
