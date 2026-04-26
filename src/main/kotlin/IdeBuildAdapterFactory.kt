package com.example

import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils

/**
 * Returns the [IdeBuildAdapter] appropriate for the running IDE.
 *
 * Adding a new IDE requires exactly one line here plus a new adapter class —
 * no other file needs to change.
 */
object IdeBuildAdapterFactory {
    fun create(project: Project): IdeBuildAdapter = when {
        PlatformUtils.isCLion()   -> ClionBuildAdapter(project)
        PlatformUtils.isPyCharm() -> PyCharmBuildAdapter(project)
        else                      -> IdeaBuildAdapter(project)
    }
}
