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
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.colors.EditorColors.*
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent
import kotlin.math.max

class RDocumentationComponent(project: Project) : DocumentationComponent(DocumentationManager.getInstance(project)) {
  override fun needsToolbar(): Boolean = true
  private val statusLabelAction = StatusLabelAction()
  private val searchModel = SearchModel(component as JEditorPane, statusLabelAction.statusLabel)
  private val textSearchFieldAction = TextSearchFieldAction()

  init {
    Disposer.register(project, this)

    // UI hacky hacks
    UIUtil.findComponentOfType(this, ActionToolbarImpl::class.java)?.let { actionToolbarImpl ->
      updateActionGroup(actionToolbarImpl.actionGroup as DefaultActionGroup)
      actionToolbarImpl.secondaryActionsButton
      addSearchTextFieldListeners()
    }
  }

  private fun updateActionGroup(actionGroup: DefaultActionGroup) {
    actionGroup.remove(actionGroup.childActionsOrStubs[5]) // remove Restore Size
    actionGroup.remove(actionGroup.childActionsOrStubs[4]) // remove Show Toolbar
    val fontSize = actionGroup.childActionsOrStubs[3]
    actionGroup.remove(fontSize)
    actionGroup.remove(actionGroup.childActionsOrStubs[2]) // remove Show on Mouse Move
    actionGroup.remove(actionGroup.childActionsOrStubs[1]) // remove Open as Tool Window
    val innerActionGroup = actionGroup.childActionsOrStubs[0] as DefaultActionGroup // action group with back and forward buttons
    innerActionGroup.remove(innerActionGroup.childActionsOrStubs[2]) // remove edit action

    actionGroup.add(wrapAction(innerActionGroup.childActionsOrStubs[0]))
    actionGroup.add(wrapAction(innerActionGroup.childActionsOrStubs[1]))

    innerActionGroup.removeAll()

    addActions(innerActionGroup)
  }

  private fun wrapAction(anAction: AnAction): AnAction {
    class ActionWrapper : AnAction(), RightAlignedToolbarAction {
      init {
        copyFrom(anAction)
      }

      override fun actionPerformed(e: AnActionEvent) {
        anAction.actionPerformed(e)
      }

      override fun update(e: AnActionEvent) {
        anAction.update(e)
      }
    }
    return ActionWrapper()
  }

  private fun addActions(innerActionGroup: DefaultActionGroup) {
    val searchTextField = textSearchFieldAction.myField
    innerActionGroup.add(statusLabelAction, Constraints.FIRST)
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

  private class StatusLabelAction : AnActionButton("", "", null), CustomComponentAction, DumbAware {
    val statusLabel: JLabel = JLabel().also { label ->
      // copy-pasted from com.intellij.find.editorHeaderActions.StatusTextAction
      //noinspection HardCodedStringLiteral
      label.font = JBUI.Fonts.toolbarFont()
      label.text = "9888 results"
      val size = label.preferredSize
      size.height = max(size.height, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE.height)
      label.preferredSize = size
      label.text = null
      label.horizontalAlignment = SwingConstants.CENTER
    }
    override fun isDumbAware(): Boolean = true
    override fun actionPerformed(e: AnActionEvent) {}
    override fun createCustomComponent(presentation: Presentation, place: String): JComponent = statusLabel
  }

  private class SearchModel(private val editorPane: JEditorPane, private val matchLabel: JLabel) {
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
      updateMatchLabel()
      scroll()
    }

    fun prev() {
      check(hasPrev) { "Doesn't have prev element" }
      currentSelection -= 1
      updateHighlighting()
      updateMatchLabel()
      scroll()
    }

    private fun scroll() {
      val viewRectangle = editorPane.modelToView(indices[currentSelection])
      editorPane.scrollRectToVisible(viewRectangle)
    }

    private fun updateIndices() {
      indices.clear()
      currentSelection = 0
      if (pattern.isNotEmpty()) {
        val text = editorPane.document.getText(0, editorPane.document.length)
        var index = 0
        while (index < text.length) {
          index = StringUtil.indexOfIgnoreCase(text, pattern, index)
          if (index == -1) break
          indices.add(index)
          index += pattern.length
        }
      }
      updateMatchLabel()
    }

    private fun updateMatchLabel() {
      matchLabel.foreground = UIUtil.getLabelForeground()
      matchLabel.font = JBUI.Fonts.toolbarFont()
      val matches = indices.size
      val cursorIndex = currentSelection + 1
      if (pattern.isEmpty()) {
        matchLabel.text = ""
      } else {
        if (indices.size > 0) {
          matchLabel.text = ApplicationBundle.message("editorsearch.current.cursor.position", cursorIndex, matches)
        } else {
          matchLabel.foreground = UIUtil.getErrorForeground()
          matchLabel.text = ApplicationBundle.message("editorsearch.matches", matches)
        }
      }
    }

    private fun updateHighlighting() {
      val highlighter = editorPane.highlighter
      for (it in tags) {
        highlighter.removeHighlight(it)
      }
      editorPane.invalidate()
      editorPane.repaint()
      for (index in indices) {
        tags.add(highlighter.addHighlight(index, index + pattern.length, searchHighlighterPainter))
      }
    }

    private inner class SearchHighlighterPainter : Highlighter.HighlightPainter {
      override fun paint(g: Graphics, p0: Int, p1: Int, bounds: Shape, c: JTextComponent) {
        val currentMatch = indices[currentSelection] == p0
        val mapper = c.ui


        val rec0: Rectangle = mapper.modelToView(c, p0)
        val rec1: Rectangle = mapper.modelToView(c, p1)

        val borderColor = getBorderColor()
        val backgroundColor = getBackgroundColor(currentMatch)
        val g2d = g.create() as Graphics2D
        try {
          // one line selection
          if (rec0.y == rec1.y) {
            val target = rec0.union(rec1)
            drawRectangle(g2d, target, backgroundColor, borderColor)
          } else {
            drawMultilinesRectangles(bounds, rec0, rec1, g2d, backgroundColor, borderColor)
          }
        }
        finally {
          g2d.dispose()
        }
      }

      private fun drawMultilinesRectangles(bounds: Shape,
                                           rec0: Rectangle,
                                           rec1: Rectangle,
                                           g2d: Graphics2D,
                                           backgroundColor: Color,
                                           borderColor: Color) {
        val area = bounds.bounds
        val rec0ToMarginWidth = area.x + area.width - rec0.x
        val secondLineY = rec0.y + rec0.height
        val firstLineRectangle = Rectangle(rec0.x, rec0.y, rec0ToMarginWidth, rec0.height)
        val lastLineRectangle = Rectangle(area.x, rec1.y, (rec1.x - area.x), rec1.height)
        // draw first line
        drawRectangle(g2d, firstLineRectangle, backgroundColor, borderColor)
        // draw middle lines
        val multiline = secondLineY != rec1.y
        if (multiline) {
          drawRectangle(g2d, Rectangle(area.x, secondLineY, area.width, rec1.y - secondLineY), backgroundColor, borderColor)
          // clear border between the first and the middle
          g2d.color = backgroundColor
          g2d.fillRect(firstLineRectangle.x, firstLineRectangle.y + firstLineRectangle.height - 2, firstLineRectangle.width - 1, 5)
        }
        // draw last line
        drawRectangle(g2d, lastLineRectangle, backgroundColor, borderColor)
        g2d.color = backgroundColor
        if (multiline) {
          // clear border between the middle and the last
          g2d.fillRect(lastLineRectangle.x + 1, lastLineRectangle.y - 2, lastLineRectangle.width - 1, 5)
        } else {
          val lastMaxX = lastLineRectangle.x + lastLineRectangle.width
          // clear border between the first and the last
          if (firstLineRectangle.x + 1 <= lastMaxX - 1) {
            g2d.fillRect(firstLineRectangle.x  + 1, lastLineRectangle.y - 2, lastMaxX - firstLineRectangle.x - 1, 5)
          }
        }
      }

      private fun drawRectangle(g2d: Graphics2D,
                                target: Rectangle,
                                backgroundColor: Color,
                                borderColor: Color) {
        g2d.color = backgroundColor
        g2d.fillRect(target.x, target.y, target.width, target.height - 1)
        g2d.color = borderColor
        g2d.drawRect(target.x, target.y, target.width, target.height - 1)
      }

      private fun getBorderColor(): Color {
        return EditorColorsManager.getInstance().globalScheme.defaultForeground
      }

      private fun getBackgroundColor(current: Boolean): Color {
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        return if (current) {
          if (ColorUtil.isDark(globalScheme.defaultBackground)) {
            EditorColorsManager.getInstance().globalScheme.getColor(SELECTION_BACKGROUND_COLOR)!!
          } else {
            globalScheme.getAttributes(SEARCH_RESULT_ATTRIBUTES).backgroundColor
          }
        } else {
          globalScheme.getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES).backgroundColor
        }
      }
    }
  }
}