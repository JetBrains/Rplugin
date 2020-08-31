package org.jetbrains.r.editor

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import org.intellij.plugins.markdown.ui.preview.MarkdownSplitEditor
import org.intellij.plugins.markdown.ui.split.SplitFileEditor
import org.jetbrains.r.RPluginUtil

class WelcomeGuideManager(private val project: Project) {
  init {
    if (!(ApplicationManager.getApplication().isUnitTestMode || ApplicationManager.getApplication().isHeadlessEnvironment)) {
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
        val openFile = FileEditorManager.getInstance(project).openFile(welcomeFile, true)
        if (openFile.isNotEmpty() && openFile[0] is MarkdownSplitEditor) {
          val markdownSplitEditor = openFile[0] as MarkdownSplitEditor
          markdownSplitEditor.triggerLayoutChange(SplitFileEditor.SplitEditorLayout.SECOND, true)
        }
      }
    }
  }

  companion object {
    internal const val KEY = "org.jetbrains.r.editor.welcomeGuide"
  }
}