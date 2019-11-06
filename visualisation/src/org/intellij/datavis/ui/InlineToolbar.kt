/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.intellij.datavis.ui

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.psi.PsiElement
import java.awt.*
import javax.swing.JPanel

class InlineToolbar(val cell: PsiElement,
                    private val editor: Editor,
                    toolbarActions: ActionGroup) : JPanel(BorderLayout()) {

  private var couldUpdateBound = false
  private val actionToolBar = ActionManager.getInstance().createActionToolbar("Editor", toolbarActions, true).apply {
    setTargetComponent(editor.contentComponent)
  }

  init {
    val component = actionToolBar.component
    component.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    component.background = editor.contentComponent.background
    add(component)
  }

  override fun paint(g: Graphics) {
    // We need this fix with AlphaComposite.SrcOver to resolve problem of black background on transparent images such as icons.
    //ToDo: - We need to make some tests on mac and linux for this, maybe this is applicable only to windows platform.
    //      - And also we need to check this on different Windows versions (definitely we have problems on Windows) .
    //      - Definitely we have problems on new macBook
    val oldComposite = (g as Graphics2D).composite
    g.composite = AlphaComposite.SrcOver
    super<JPanel>.paint(g)
    g.composite = oldComposite
  }

  fun updateBounds() {
    val toolbarWidth = actionToolBar.component.preferredSize.width
    val toolbarHeight = actionToolBar.component.preferredSize.height
    val gutterWidth = (editor.gutter as EditorGutterComponentEx).width
    val editorBounds = editor.component.bounds
    val deltaY = (toolbarHeight - editor.lineHeight) / 2
    val newToolbarX = editorBounds.x + editorBounds.width - toolbarWidth - gutterWidth
    val newToolbarY = editor.visualLineToY(editor.document.getLineNumber(cell.textRange.endOffset)) - deltaY
    try {
      couldUpdateBound = true
      bounds = Rectangle(newToolbarX, newToolbarY, toolbarWidth, toolbarHeight)
    } finally {
      couldUpdateBound = false
    }
    invalidate()
    repaint()
  }
}