package com.example

import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.project.Project

/**
 * Entry point for IntelliJ IDEA compilation events. Registered in
 * plugin-idea.xml (loaded only when com.intellij.java is present).
 *
 * Delegates entirely to [IdeaBuildAdapter] via [IdeBuildAdapterFactory].
 * In CLion the factory returns [ClionBuildAdapter], the cast to
 * [IdeaBuildAdapter] yields null, and every event is silently ignored.
 */
class BuildErrorListener(private val project: Project) : CompilationStatusListener {
    private val adapter = IdeBuildAdapterFactory.create(project) as? IdeaBuildAdapter

    override fun compilationFinished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
        adapter?.onCompilationFinished(aborted, errors, compileContext)
    }
}
