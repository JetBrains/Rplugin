/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.toolwindow

import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.editor.colors.EditorColors.*
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.AnActionButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.LightColors
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.event.DocumentEvent
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent
import javax.swing.text.Position

class RDocumentationComponent(project: Project) : DocumentationComponent(DocumentationManager.getInstance(project)) {
  override fun needsToolbar(): Boolean = true
  private val searchModel = SearchModel(component as JEditorPane)
  private val textSearchFieldAction = TextSearchFieldAction()

  init {
    Disposer.register(project, this)

    // UI hacky hacks
    UIUtil.findComponentOfType(this, ActionToolbarImpl::class.java)?.let { actionToolbarImpl ->
      updateActionGroup(actionToolbarImpl.actionGroup as DefaultActionGroup)
      addSearchTextFieldListeners()
    }
  }

  private fun updateActionGroup(actionGroup: DefaultActionGroup) {
    actionGroup.remove(actionGroup.childActionsOrStubs[4]) // remove Restore Size
    actionGroup.remove(actionGroup.childActionsOrStubs[3]) // remove Show Toolbar
    actionGroup.remove(actionGroup.childActionsOrStubs[1]) // remove Open as Tool Window
    val innerActionGroup = actionGroup.childActionsOrStubs[0] as DefaultActionGroup // action group with back and forward buttons
    innerActionGroup.remove(innerActionGroup.childActionsOrStubs[2]) // remove edit action
    addActions(innerActionGroup)
  }

  private fun addActions(innerActionGroup: DefaultActionGroup) {
    val searchTextField = textSearchFieldAction.myField
    innerActionGroup.add(Separator(), Constraints.FIRST)
    val nextOccurrenceAction = createNextOccurrenceAction()
    nextOccurrenceAction.registerCustomShortcutSet(nextOccurrenceAction.shortcutSet, searchTextField)
    nextOccurrenceAction.registerCustomShortcutSet(nextOccurrenceAction.shortcutSet, this)
    innerActionGroup.add(nextOccurrenceAction, Constraints.FIRST)
    val prevOccurrenceAction = createPrevOccurrenceAction()
    prevOccurrenceAction.registerCustomShortcutSet(prevOccurrenceAction.shortcutSet, searchTextField)
    prevOccurrenceAction.registerCustomShortcutSet(prevOccurrenceAction.shortcutSet, this)
    innerActionGroup.add(prevOccurrenceAction, Constraints.FIRST)
    innerActionGroup.addAction(textSearchFieldAction, Constraints.FIRST)
  }

  private fun addSearchTextFieldListeners() {
    val searchTextField = textSearchFieldAction.myField
    searchTextField.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        val pattern = searchTextField.text
        searchModel.setPattern(pattern)
        if (searchModel.hasMatches || pattern.isEmpty()) {
          searchTextField.textEditor.setBackground(UIUtil.getTextFieldBackground())
        } else {
          searchTextField.textEditor.setBackground(LightColors.RED)
        }
      }
    })
    searchTextField.addKeyboardListener(object : KeyAdapter() {
      override fun keyReleased(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ENTER && searchModel.hasNext) {
          searchModel.next()
        }
      }
    })
  }

  private fun createNextOccurrenceAction() = object : DumbAwareAction() {
    init { ActionUtil.copyFrom(this, IdeActions.ACTION_NEXT_OCCURENCE) }
    override fun actionPerformed(e: AnActionEvent) = searchModel.next()
    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = searchModel.hasNext
    }
  }

  private fun createPrevOccurrenceAction() = object : DumbAwareAction() {
    init { ActionUtil.copyFrom(this, IdeActions.ACTION_PREVIOUS_OCCURENCE) }
    override fun actionPerformed(e: AnActionEvent) = searchModel.prev()
    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = searchModel.hasPrev
    }
  }

  private class TextSearchFieldAction : AnActionButton("", "", null), CustomComponentAction, DumbAware {
    val myField: SearchTextField = SearchTextField()
    override fun isDumbAware(): Boolean = true
    override fun actionPerformed(e: AnActionEvent) {}
    override fun createCustomComponent(presentation: Presentation, place: String): JComponent = myField
  }

  private class SearchModel(private val editorPane: JEditorPane) {
    init {
      editorPane.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          updateIndices()
          updateHighlighting()
        }
      })
    }

    private val scrollPane = UIUtil.getParentOfType(JScrollPane::class.java, editorPane)
    private val indices = ArrayList<Int>()
    private var currentSelection = 0
    private val tags = ArrayList<Any>()
    private var pattern: String = ""
    private val searchHighlighterPainter = SearchHighlighterPainter()

    fun setPattern(pattern: String) {
      if (this.pattern == pattern) return
      this.pattern = pattern
      updateIndices()
      updateHighlighting()
    }

    val hasMatches: Boolean
      get() = indices.isNotEmpty()

    val hasNext: Boolean
      get() = currentSelection + 1 < indices.size

    val hasPrev: Boolean
      get() = currentSelection - 1 >= 0

    fun next() {
      check(hasNext) { "Doesn't have next element" }
      currentSelection += 1
      updateHighlighting()
      scroll()
    }

    fun prev() {
      check(hasPrev) { "Doesn't have prev element" }
      currentSelection -= 1
      updateHighlighting()
      scroll()
    }

    private fun scroll() {
      val viewRectangle = editorPane.modelToView(indices[currentSelection])
      editorPane.scrollRectToVisible(viewRectangle)
    }

    private fun updateIndices() {
      indices.clear()
      currentSelection = 0
      if (pattern.isEmpty()) return
      val text = editorPane.document.getText(0, editorPane.document.length)
      var index = 0
      while (index < text.length) {
        index = StringUtil.indexOfIgnoreCase(text, pattern, index)
        if (index == -1) break
        indices.add(index)
        index += pattern.length
      }
    }

    private fun updateHighlighting() {
      val highlighter = editorPane.highlighter
      for (it in tags) {
        highlighter.removeHighlight(it)
      }
      if (indices.isEmpty()) {
        // get rid of possible visual artifacts
        editorPane.invalidate()
        editorPane.repaint()
      }
      for (index in indices) {
        tags.add(highlighter.addHighlight(index, index + pattern.length, searchHighlighterPainter))
      }
    }

    inner class SearchHighlighterPainter : Highlighter.HighlightPainter {
      override fun paint(g: Graphics, p0: Int, p1: Int, bounds: Shape, c: JTextComponent) {
        val currentMatch = indices[currentSelection] == p0
        val target = c.ui.getRootView(c).modelToView(p0, Position.Bias.Forward, p1, Position.Bias.Backward, bounds).bounds
        val borderColor = getBorderColor(currentMatch)
        val backgroundColor = getBackgroundColor(currentMatch)
        val g2d = g.create() as Graphics2D
        try {
          g2d.color = backgroundColor
          g2d.fillRect(target.x, target.y, target.width, target.height - 1)
          g2d.color = borderColor
          g2d.drawRect(target.x, target.y, target.width, target.height - 1)
        }
        finally {
          g2d.dispose()
        }
      }

      private fun getBorderColor(current: Boolean): Color {
        return if (current) {
          EditorColorsManager.getInstance().globalScheme.defaultForeground
        } else {
          EditorColorsManager.getInstance().globalScheme.getAttributes(SEARCH_RESULT_ATTRIBUTES).backgroundColor
        }
      }

      private fun getBackgroundColor(current: Boolean): Color {
        return if (current) {
          EditorColorsManager.getInstance().globalScheme.getColor(SELECTION_BACKGROUND_COLOR)!!
        } else {
          EditorColorsManager.getInstance().globalScheme.getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES).backgroundColor
        }
      }
    }
  }
}