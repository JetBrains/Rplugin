/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.rmarkdown.RMarkdownLanguage
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.text.DefaultFormatter
import org.intellij.lang.annotations.Language as AnLanguage

@Suppress("UnstableApiUsage")
class RReturnHintInlayProvider : InlayHintsProvider<RReturnHintInlayProvider.Settings> {

  override val name: String = RBundle.message("inlay.hints.function.return.expression.name")
  override val key: SettingsKey<Settings> = settingsKey
  override val group: InlayGroup
    get() = InlayGroup.VALUES_GROUP

  override fun getProperty(key: String): String {
    return RBundle.getMessage(key)
  }

  override fun createSettings() = Settings()

  override fun isLanguageSupported(language: Language): Boolean = language in listOf(RLanguage.INSTANCE, RMarkdownLanguage)

  private val lastSeenSettings = Settings()
  private var documentsToForceRepaint = mutableSetOf<Document>()

  override fun getCollectorFor(file: PsiFile, editor: Editor, settings: Settings, sink: InlayHintsSink): InlayHintsCollector =
    object : FactoryInlayHintsCollector(editor) {
      // Information will be painted with org.jetbrains.r.hints.RReturnHintModel$RReturnHintLineExtensionPainter
      override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (element !is PsiFile || element.getUserData(RConsoleView.IS_R_CONSOLE_KEY) == true) return false

        val document = editor.document
        if (lastSeenSettings.differentReturnExpressions != settings.differentReturnExpressions
            || lastSeenSettings.showImplicitReturn != settings.showImplicitReturn) {
          lastSeenSettings.differentReturnExpressions = settings.differentReturnExpressions
          lastSeenSettings.showImplicitReturn = settings.showImplicitReturn

          val allDocuments = RReturnHintsModel.getInstance(element.project).activeDocuments(document)
          val project = element.project
          val passDocuments = RReturnHintPass.FactoryService.getInstance(project)
            .filterAndUpdateDocumentsToForceRepaint(allDocuments, project)
          documentsToForceRepaint.addAll(allDocuments - passDocuments)
        }

        RReturnHintPass(element, editor, document in documentsToForceRepaint, settings).apply {
          doCollectInformation(EmptyProgressIndicator())
          runInEdt {
            doApplyInformationToEditor()
          }
          documentsToForceRepaint.remove(document)
        }
        return false
      }
    }

  override fun createConfigurable(settings: Settings) = object : ImmediateConfigurable {

    private val uniqueTypeCount = JBIntSpinner(1, 1, 10)

    override val mainCheckboxText: String = RBundle.message("inlay.hints.function.return.expression.main.checkbox.text")

    override fun createComponent(listener: ChangeListener): JPanel {
      reset()
      // Workaround to get immediate change, not only when focus is lost. To be changed after moving to polling model
      val formatter = (uniqueTypeCount.editor as JSpinner.NumberEditor).textField.formatter as DefaultFormatter
      formatter.commitsOnValidEdit = true
      uniqueTypeCount.addChangeListener {
        handleChange(listener)
      }
      val panel = panel {
        row {
          label(RBundle.message("inlay.hints.function.return.expression.count.spinner.text"))
          uniqueTypeCount(pushX)
        }
      }
      panel.border = JBUI.Borders.empty(5)
      return panel
    }

    override fun reset() {
      uniqueTypeCount.value = settings.differentReturnExpressions
    }

    private fun handleChange(listener: ChangeListener) {
      settings.differentReturnExpressions = uniqueTypeCount.number
      listener.settingsChanged()
    }

    override val cases: List<ImmediateConfigurable.Case>
      get() = listOf(ImmediateConfigurable.Case(RBundle.message("inlay.hints.function.return.expression.null.checkbox.text"),
                                                "implicit.null.result",
                                                { settings.showImplicitReturn },
                                                { settings.showImplicitReturn = it }))
  }

  override val previewText: String
    @AnLanguage("R")
    get() = """
      single_result <- function() {
        42
      }
      
      implicit_null_result <- function(first, second, third) {
        if (first == second) {
          third
        }
      }

      two_results <- function(a, b) {
        if (a == b) {
          21 + 22
        } else {
          44
        }
      }
  """.trimIndent()

  companion object {
    val settingsKey: SettingsKey<Settings> = SettingsKey("return.values.hints")
  }

  data class Settings(var differentReturnExpressions: Int = 1, var showImplicitReturn: Boolean = true)
}