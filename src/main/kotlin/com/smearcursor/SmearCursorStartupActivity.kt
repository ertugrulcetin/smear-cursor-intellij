package com.smearcursor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * Startup activity that initializes the smear cursor service when IDE starts.
 */
class SmearCursorStartupActivity : StartupActivity.DumbAware {

    override fun runActivity(project: Project) {
        // Initialize the smear cursor service on the EDT
        ApplicationManager.getApplication().invokeLater {
            SmearCursorService.getInstance().initialize()
        }
    }
}
