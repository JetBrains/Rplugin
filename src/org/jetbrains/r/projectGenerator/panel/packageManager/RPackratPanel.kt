/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.packageManager

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.AbstractTableCellEditor
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.projectGenerator.template.RProjectSettings
import java.awt.BorderLayout
import java.awt.Component
import java.util.*
import java.util.function.Consumer
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer
import kotlin.collections.HashMap

const val SETTINGS_NAME_COLUMN = 0
const val SETTINGS_VALUE_COLUMN = 1

/**
 * Packrat — the R package manager
 */
class RPackratPanel(private val rProjectSettings: RProjectSettings) : RPackageManagerPanel(rProjectSettings) {

  override val panelName: String
    get() = "Packrat package manager panel"

  override val packageManagerName: String
    get() = "Packat"

  override val rPackageName: String
    get() = "packrat"

  override val initProjectScriptName: String
    get() = "createPackrat"

  override val willCreateDescription = false

  private val lastSettingsRequest = LastSettingsRequest()
  private val errorBackgroundColor = JBColor.GRAY
  private val tableModel = PackratSettingsTableModel()
  private val tableCellEditor = PackratTableCellEditor()
  private val tablePanel: JPanel
  private var errorAction: Consumer<List<ValidationInfo>>? = null
  private val table = object : JBTable(tableModel) {
    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
      val component = super.prepareRenderer(renderer, row, column)
      if (tableModel.isBadRowValue(row) && !isCellSelected(row, column)) {
        component.background = errorBackgroundColor
      }
      return component
    }
  }

  private val errorMessages = HashMap<Int, String>()
  private val validationInfos
    get() = errorMessages.values.map { ValidationInfo(it) }

  init {
    layout = BorderLayout()
    table.getColumnModel().getColumn(SETTINGS_VALUE_COLUMN).setCellEditor(tableCellEditor)
    table.isStriped = true
    tablePanel = ToolbarDecorator.createDecorator(table).disableUpDownActions().disableAddAction().disableRemoveAction().createPanel()
    add(tablePanel)
  }

  override fun generateProject(project: Project, baseDir: VirtualFile, module: Module) {
    val packratSettings = tableModel.getData().map {
      val value = it.value
      when {
        value is PackartLogicalConstants -> "${it.name} = $value"
        value is PackratExpandedLogicalConstants -> {
          if (value == PackratExpandedLogicalConstants.AUTO) {
            "${it.name} = 'auto'"
          }
          else {
            "${it.name} = $value"
          }
        }
        else -> {
          val realValue = StringJoiner(", ", "c(", ")")
          (value as String).split(",").forEach { valuePart ->
            realValue.add("'${valuePart.trim()}'")
          }
          "${it.name} = $realValue"
        }
      }
    }

    val packratSettingsList = StringJoiner(", ", "list(", ")")
    packratSettings.forEach { packratSettingsList.add(it) }

    initializePackage(project, baseDir, listOf(baseDir.path, packratSettingsList.toString()))
    focusFile(project, baseDir, ".Rprofile")
  }

  override fun validateSettings(): List<ValidationInfo> {
    tableCellEditor.stopCellEditing()
    if (!rProjectSettings.installedPackages.contains(rPackageName)) {
      tableModel.isTableEditable = false
      tableModel.clearBadValue()
      table.updateUI()
      runIsPackageInstalledAction(false)
      return listOf(ValidationInfo(RBundle.message("project.setting.missing.package.manager", rPackageName)))
    }

    runIsPackageInstalledAction(true)
    tableModel.isTableEditable = true
    val validationInfos = validateAllPackratSettings()
    table.updateUI()
    return validationInfos
  }

  private fun validateAllPackratSettings(): List<ValidationInfo> {
    for (row in 0 until tableModel.rowCount) {
      validatePackratSettings(row)
    }

    return validationInfos
  }

  private fun validatePackratSettings(row: Int, value: Any = tableModel.getValueAt(row, SETTINGS_VALUE_COLUMN)) {
    if (value is String && value.isNotBlank()) {
      for (valuePart in value.split(",").map { it.trim() }) {
        if (valuePart.isBlank()) {
          errorMessages[row] = RBundle.message("project.setting.packrat.warning.comma")
          break
        }

        if (tableModel.getValueAt(row, SETTINGS_NAME_COLUMN) == "external.packages") {
          if (valuePart.isNotEmpty() && !rProjectSettings.installedPackages.contains(valuePart)) {
            errorMessages[row] = RBundle.message("project.setting.packrat.warning.missing.package", valuePart)
            break
          }
        }
        errorMessages.remove(row)
      }
    }
    else {
      errorMessages.remove(row)
    }

    tableModel.setIsBadRowValue(errorMessages.containsKey(row), row)
  }

  fun updatePackratSettings(interpreter: String) {
    if (interpreter != lastSettingsRequest.interpreterPath) {
      lastSettingsRequest.interpreterPath = interpreter
      lastSettingsRequest.settings = getAllPackratSettings(interpreter)
    }
    tableModel.updateDataRows(lastSettingsRequest.settings.toTypedArray())
    val bordersSize = (tablePanel.border as SideBorder).getBorderInsets(tablePanel)
    tablePanel.setPreferredSize(JBDimension(-1,
                                            table.rowHeight * table.rowCount +
                                            table.tableHeader.preferredSize.height +
                                            bordersSize.bottom + bordersSize.top))
  }

  fun setErrorAction(action: Consumer<List<ValidationInfo>>) {
    errorAction = action
  }

  inner class PackratSettingsTableModel : AbstractTableModel() {

    private val columnNames = arrayOf("Option", "Value")
    private var data = emptyArray<Array<Any>>()
    private var isBadValue = emptyArray<Boolean>()
    var isTableEditable = true

    fun updateDataRows(dataRows: Array<PackratSettings<*>>) {
      val dataMap = HashMap<String, Any>()
      data.forEach { dataMap[it[SETTINGS_NAME_COLUMN] as String] = it[SETTINGS_VALUE_COLUMN] }
      data = dataRows.map {
        if (dataMap.containsKey(it.name)) {
          PackratSettings(it.name, dataMap[it.name])
        }
        else {
          it
        }
      }.map { arrayOf<Any>(it.name, it.value as Any) }.toTypedArray()

      isBadValue = Array<Boolean>(data.size) { false }
      fireTableDataChanged()
    }

    fun getData(): List<PackratSettings<*>> {
      return data.map { PackratSettings((it[SETTINGS_NAME_COLUMN] as String), it[SETTINGS_VALUE_COLUMN]) }
    }

    override fun getColumnCount(): Int {
      return columnNames.size
    }

    override fun getRowCount(): Int {
      return data.size
    }

    override fun getColumnName(column: Int): String {
      return columnNames[column]
    }

    override fun getValueAt(row: Int, column: Int): Any {
      return data[row][column]
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
      return isTableEditable && column == SETTINGS_VALUE_COLUMN
    }

    override fun setValueAt(value: Any?, row: Int, column: Int) {
      value ?: return
      data[row][column] = value
      fireTableCellUpdated(row, column)
    }

    fun setIsBadRowValue(isBadValue: Boolean, row: Int) {
      if (this.isBadValue[row] != isBadValue) {
        this.isBadValue[row] = isBadValue
        fireTableCellUpdated(row, SETTINGS_VALUE_COLUMN)
      }
    }

    fun isBadRowValue(row: Int): Boolean {
      return isBadValue[row]
    }

    fun clearBadValue() {
      isBadValue.fill(false)
    }
  }

  inner class PackratTableCellEditor : AbstractTableCellEditor() {
    private lateinit var component: JComponent

    override fun getCellEditorValue(): Any {
      return if (component is JComboBox<*>) {
        (component as JComboBox<*>).selectedItem
      }
      else {
        (component as JBTextField).text
      }
    }

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
      val defaultBackgroundColor = if (row % 2 == 1) background else UIUtil.getDecoratedRowColor() // Due of stripes
      component = when (value) {
        is PackartLogicalConstants -> ComboBox<PackartLogicalConstants>(
          PackartLogicalConstants.values()).apply {
          selectedItem = value
          background = defaultBackgroundColor
        }
        is PackratExpandedLogicalConstants -> ComboBox<PackratExpandedLogicalConstants>(
          PackratExpandedLogicalConstants.values()).apply {
          selectedItem = value
          background = defaultBackgroundColor
        }
        else -> JBTextField(value.toString()).apply {
          background = if (tableModel.isBadRowValue(row)) errorBackgroundColor else defaultBackgroundColor
          getDocument().addDocumentListener(object : DocumentListener {
            override fun changedUpdate(event: DocumentEvent?) {
              validatePackratSettings(row, text)
              errorAction?.accept(validationInfos)
              this@apply.background = if (errorMessages.containsKey(row)) errorBackgroundColor else defaultBackgroundColor
            }

            override fun insertUpdate(event: DocumentEvent?) {
              changedUpdate(event)
            }

            override fun removeUpdate(event: DocumentEvent?) {
              changedUpdate(event)
            }
          })
        }
      }
      return component
    }
  }

  private data class LastSettingsRequest(var interpreterPath: String? = null, var settings: List<PackratSettings<*>> = emptyList())
}
