package org.jetbrains.r.configuration

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.settings.RSettings

class RInterpreterLocationPusher : StartupActivity.DumbAware {
  override fun runActivity(project: Project) {
     if (RSettings.getInstance(project).interpreterLocation == null) {
       runAsync {
         RInterpreterUtil.suggestAllInterpreters(true, true).firstOrNull()?.let { interpreterInfo ->
           invokeLater {
             if (project.isDisposed) return@invokeLater
             RSettings.getInstance(project).interpreterLocation = interpreterInfo.interpreterLocation
           }
         }
       }
     }
  }
}