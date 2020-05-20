/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel

/**
 *  Table by latest guidelines
 *  https://jetbrains.github.io/ui/controls/table/
 */
open class MaterialTable : JBTable {

  constructor() : super()
  constructor(model: TableModel, columnModel: TableColumnModel) : super(model, columnModel)

  var indexColumnWidth = 0

  /** TODO need to discuss with UI/UX dep. */
  class SimpleHeaderRenderer : JLabel(), TableCellRenderer {

    companion object {
      val cellBorder: Border? = IdeBorderFactory.createBorder(SideBorder.RIGHT)
    }

    init {
      font = JBUI.Fonts.label().deriveFont(Font.BOLD)
      isOpaque = false // In the other case label will not fill the background.
      background = HEADER_BACKGROUND
      verticalAlignment = SwingConstants.CENTER
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = (font.size*2.1).toInt()
      return d
    }

    override fun getTableCellRendererComponent(table: JTable,
                                               value: Any?,
                                               isSelected: Boolean,
                                               hasFocus: Boolean,
                                               row: Int,
                                               column: Int): Component {

      border = if (table.columnCount == column + 1) null else cellBorder
      text = " ${value.toString()} "
      return this
    }
  }

  fun enableMultilineHeader() {
    tableHeader.defaultRenderer = MultiLineTableHeaderRenderer()
  }


  internal class MultiLineTableHeaderRenderer : TableCellRenderer {
    private val mainPanel: JPanel = JPanel()
    private val headerText: JTextArea = JTextArea()
    private val sortLabel: JLabel = JBLabel()
    private val textInsets: Insets = JBUI.insets(3, 3, 3, 3)
    private val sortLabelEmptyBorder: Border = IdeBorderFactory.createEmptyBorder(JBUI.insets(0, 2, 0, 20))
    private val sortLabelIconBorder: Border = IdeBorderFactory.createEmptyBorder(
      JBUI.insets(0, 2, 0, 20 - AllIcons.General.ArrowDown.iconWidth))

    init {
      mainPanel.layout = BorderLayout()
      headerText.font = JBUI.Fonts.label().deriveFont(Font.BOLD)
      headerText.isOpaque = false
      headerText.background = HEADER_BACKGROUND
      headerText.isEditable = false
      headerText.border = IdeBorderFactory.createEmptyBorder(textInsets)
      sortLabel.border = sortLabelEmptyBorder
      sortLabel.text = ""
      mainPanel.add(headerText, BorderLayout.CENTER)
      mainPanel.add(sortLabel, BorderLayout.EAST)
    }

    private fun getSortingIcon(column: Int, sortKeys: List<RowSorter.SortKey>): Icon? {
      val sortKey = sortKeys.firstOrNull { it.column == column } ?: return null
      return when (sortKey.sortOrder) {
        SortOrder.ASCENDING -> AllIcons.General.ArrowDown
        SortOrder.DESCENDING -> AllIcons.General.ArrowUp
        else -> null
      }
    }

    override fun getTableCellRendererComponent(table: JTable,
                                               value: Any,
                                               isSelected: Boolean,
                                               hasFocus: Boolean,
                                               row: Int,
                                               column: Int): Component {
      headerText.text = "${value}"
      sortLabel.icon = getSortingIcon(column, table.rowSorter.sortKeys)
      sortLabel.border = if (sortLabel.icon != null) sortLabelIconBorder else sortLabelEmptyBorder
      return mainPanel
    }
  }

  private var rollOverRowIndex = -1

  init {
    autoResizeMode = JTable.AUTO_RESIZE_OFF
    //table.fillsViewportHeight = true
    background = EditorColorsManager.getInstance().globalScheme.defaultBackground
    setShowColumns(true)
    autoCreateRowSorter = true
    rowHeight = (font.size * 1.8).toInt()

    //table.background = Color.white
    setShowGrid(false)
    tableHeader.defaultRenderer = SimpleHeaderRenderer()
    tableHeader.isOpaque = false
    tableHeader.background = HEADER_BACKGROUND
    tableHeader.resizingAllowed = true
    tableHeader.reorderingAllowed = false // Temporary disabled because of visual artifacts. Should be enabled when we will finish with new table component.

    tableHeader.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM or SideBorder.TOP)

    val mouseListener = object : MouseInputAdapter() {

      override fun mouseExited(e: MouseEvent) {
        rollOverRowIndex = -1
        repaint()
      }

      override fun mouseMoved(e: MouseEvent) {
        val row = rowAtPoint(e.point)
        if (row != rollOverRowIndex) {
          rollOverRowIndex = row
          repaint()
        }
      }
    }

    addMouseMotionListener(mouseListener)
    addMouseListener(mouseListener)
  }

  /** We are preparing renderer background for mouse hovered row. */
  override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
    val c = super.prepareRenderer(renderer, row, column)
    if (isRowSelected(row) || row == rollOverRowIndex) {
      c.foreground = getSelectionForeground()
      c.background = getSelectionBackground()
    }
    else {
      if (0 < indexColumnWidth && column < indexColumnWidth) {
        c.font = JBUI.Fonts.label().deriveFont(Font.BOLD)
        c.background = HEADER_BACKGROUND
      }
      else {
        c.background = background
      }
      c.foreground = foreground
    }
    return c
  }
}

val HEADER_BACKGROUND: Color = JBColor.PanelBackground