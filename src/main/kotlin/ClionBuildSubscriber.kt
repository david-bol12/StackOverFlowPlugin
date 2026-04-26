package com.example

import com.intellij.build.BuildProgressListener
import com.intellij.build.BuildViewManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.PlatformUtils

class ClionBuildSubscriber : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (!PlatformUtils.isCLion()) return
        val adapter = IdeBuildAdapterFactory.create(project) as? ClionBuildAdapter ?: return
        val buildViewManager = project.getService(BuildViewManager::class.java) ?: return
        buildViewManager.addListener(
            BuildProgressListener { buildId, event -> adapter.onEvent(buildId, event) },
            project
        )
    }
}
