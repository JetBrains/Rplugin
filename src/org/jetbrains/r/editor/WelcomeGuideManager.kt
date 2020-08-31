package org.jetbrains.r.editor

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.r.RPluginUtil

class WelcomeGuideManager(private val project: Project) {
  init {
    if (!(ApplicationManager.getApplication().isUnitTestMode ||
          ApplicationManager.getApplication().isHeadlessEnvironment ||
          RPluginUtil.getPlugin().isBundled)) {
      showWelcomeGuide()
    }
  }

  internal fun showWelcomeGuide() {
    val propertiesComponent = PropertiesComponent.getInstance()
    if (!propertiesComponent.isTrueValue(KEY) && !RPluginUtil.getPlugin().isBundled) {
      propertiesComponent.setValue(KEY, true)
      val welcomeText = String(javaClass.getResourceAsStream("/fileTemplates/internal/welcome.md").readAllBytes())
      val welcomeFile = LightVirtualFile("R plugin - Welcome.md", welcomeText)
      invokeLater {
        TextEditorWithPreview.openPreviewForFile(project, welcomeFile)
      }
    }
  }

  companion object {
    internal const val KEY = "org.jetbrains.r.editor.welcomeGuide"
  }
}