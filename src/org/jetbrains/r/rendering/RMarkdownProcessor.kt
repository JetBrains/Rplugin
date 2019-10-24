/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.packages.*
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import org.jetbrains.r.rmarkdownconsole.RMarkdownConsoleManager
import java.io.File
import java.nio.file.Paths


object RMarkdownProcessor {
  fun openResult(file: File) {
    BrowserLauncher.instance.browse(file)
  }

  fun render(project: Project, file: VirtualFile, onFinished: (() -> Unit)? = null) {
    val requiredPackageInstaller = RequiredPackageInstaller.getInstance(project)
    val requiredPackages = listOf(RequiredPackage("rmarkdown"))
    if (requiredPackageInstaller.getMissingPackages(requiredPackages).isNotEmpty()) {
      val listener = object : RequiredPackageListener {
        override fun onPackagesInstalled() {
          doRender(project, file, onFinished)
        }

        override fun onErrorOccurred(e: InstallationPackageException) {
          onFinished?.invoke()
          Notification(
            RBundle.message("rmarkdown.processor.notification.group.display"),
            RBundle.message("rmarkdown.processor.notification.title"),
            RBundle.message("rmarkdown.processor.notification.content"),
            NotificationType.ERROR
          ).notify(project)
        }
      }
      RequiredPackageInstaller.getInstance(project)
        .installPackagesWithUserPermission(RBundle.message("rmarkdown.processor.notification.utility.name"), requiredPackages, listener)
    }
    else {
      doRender(project, file, onFinished)
    }
  }

  private fun doRender(project: Project,
                       file: VirtualFile,
                       onFinished: (() -> Unit)?) {
    fun getRScriptPath(interpreterPath: String): String {
      return if (SystemInfo.isWindows) {
        val withoutExe = interpreterPath.substring(0, interpreterPath.length - 4)
        withoutExe + "script.exe"
      } else {
        interpreterPath + "script"
      }
    }

    fun getPandocLibraryPath(): String {
      return Paths.get(PathManager.getPluginsPath(), "rplugin", "pandoc").toString().also {
        File(it).mkdir()
      }
    }

    RInterpreterManager.getInterpreter(project)?.let { interpreter ->
      val pathToRscript = getRScriptPath(interpreter.interpreterPath)
      val scriptPath = StringUtil.escapeBackSlashes(R_MARKDOWN_HELPER.absolutePath)
      val filePath = StringUtil.escapeBackSlashes(file.path)
      val renderDirectory = StringUtil.escapeBackSlashes(RMarkdownSettings.getInstance(project).state.getProfileRenderDirectory(file.path))
      val libraryPath = StringUtil.escapeBackSlashes(getPandocLibraryPath())
      val script = arrayListOf<String>(pathToRscript, scriptPath, libraryPath, filePath, renderDirectory)
      val rMarkdownConsoleManager = ServiceManager.getService(project, RMarkdownConsoleManager::class.java)
      rMarkdownConsoleManager.consoleRunner.runRender(script, file.path, onFinished)
    }
  }

  private val R_MARKDOWN_HELPER = RHelpersUtil.findFileInRHelpers("R/render_markdown.R")
}
