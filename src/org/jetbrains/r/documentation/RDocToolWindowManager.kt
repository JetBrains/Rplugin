// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.r.documentation

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.documentation.AbstractDocToolWindowManager
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.psi.PsiElement
import com.intellij.ui.content.Content
import org.jetbrains.r.RLanguage
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import java.util.function.Supplier

@Deprecated("V2 implementation doesn't allow customization of the tool window in plugins")
class RDocToolWindowManager : AbstractDocToolWindowManager() {

  override fun createToolWindow(element: PsiElement, originalElement: PsiElement?, documentationManager: DocumentationManager): ToolWindow {
    assert(element.language == RLanguage.INSTANCE)
    val project = element.project
    return ToolWindowManager.getInstance(project).getToolWindow(RToolWindowFactory.ID)!!
  }

  override fun setToolWindowDefaultState(toolWindow: ToolWindow, documentationManager: DocumentationManager) {
    // toolwindow exist always
  }

  override fun prepareForShowDocumentation(toolWindow: ToolWindow, documentationManager: DocumentationManager) {
    val content = getDocumentationContent(toolWindow, documentationManager)
    toolWindow.contentManager.setSelectedContent(content)
  }

  override fun installToolWindowActions(toolWindow: ToolWindow, documentationManager: DocumentationManager) {
    val documentationComponent = getDocumentationComponent(toolWindow, documentationManager)
    val restorePopupAction = object : DumbAwareAction(CodeInsightBundle.messagePointer("action.AnActionButton.text.open.as.popup"),
                                                      Supplier { documentationManager.restorePopupDescription },
                                                      null) {
      override fun actionPerformed(event: AnActionEvent) {
        documentationManager.restorePopupBehavior()
      }

      override fun update(event: AnActionEvent) {
        // Show this action only on "Documentation" tab
        event.presentation.isEnabledAndVisible = documentationComponent.isDisplayable
      }
    }
    (toolWindow as ToolWindowEx).setAdditionalGearActions(DefaultActionGroup(restorePopupAction))
    documentationManager.registerQuickDocShortcutSet(documentationComponent, restorePopupAction)
  }

  override fun getDocumentationContent(toolWindow: ToolWindow, documentationManager: DocumentationManager): Content {
    assert(toolWindow.id == RToolWindowFactory.ID)
    return toolWindow.contentManager.findContent(RToolWindowFactory.HELP)
  }

  override fun getDocumentationComponent(toolWindow: ToolWindow, documentationManager: DocumentationManager): DocumentationComponent {
    return getDocumentationContent(toolWindow, documentationManager).component as DocumentationComponent
  }

  override fun updateToolWindowDocumentationTabName(toolWindow: ToolWindow, element: PsiElement, documentationManager: DocumentationManager) {
    // "Documentation" tab should always be called as "Documentation"
  }

  override fun disposeToolWindow(toolWindow: ToolWindow, documentationManager: DocumentationManager) {
    // Remove "Open as popup" action from gear actions, but toolwindow should exist always
    (toolWindow as ToolWindowEx).setAdditionalGearActions(null)
  }

  override fun isAutoUpdateAvailable(): Boolean {
    return false
  }
}
