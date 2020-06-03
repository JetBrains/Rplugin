/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.run.visualize.forms.RImportExcelOptionPanelForm
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.reflect.KProperty

class RImportExcelDataDialog(project: Project, interop: RInterop, parent: Disposable, initialPath: String? = null)
  : RImportDataDialog(project, interop, parent, initialPath)
{
  private val form = RImportExcelOptionPanelForm()
  private val range by RangeInputDelegate(form.rangeTextFieldPanel)
  private val sheetDelegate = ComboBoxDelegate(form.sheetComboBox, DEFAULT_SHEET_ENTRIES)

  private var sheet by sheetDelegate
  private var firstRowAsNames by CheckBoxDelegate(form.firstRowAsNamesCheckBox)
  private var maxRowCount by IntFieldDelegate(form.maxRowsTextField, true)
  private var skipRowCount by IntFieldDelegate(form.skipTextField)
  private var na by TextFieldDelegate(form.naTextField)

  override val importOptionComponent: JComponent
    get() = form.contentPane

  override val additionalOptions: Map<String, String>?
    get() = range?.let { range ->
      maxRowCount?.let { maxCount ->
        skipRowCount?.let { skipCount ->
          mutableMapOf<String, String>().also { options ->
            if (range.isNotBlank()) {
              options["range"] = range.quote()
            } else {
              options["skip"] = "$skipCount"
              if (maxCount >= 0) {  // Note: will be (-1) when not set
                options["nMax"] = "$maxCount"
              }
            }
            if (na.isNotBlank()) {
              options["na"] = na.quote()
            }
            sheet?.let { sheet ->
              options["sheet"] = sheet.quote()
            }
            options["columnNames"] = firstRowAsNames.toRBoolean()
          }
        }
      }
    }

  override val supportedFormats = RImportDataUtil.supportedExcelFormats
  override val importMode = "xls"

  init {
    init()
  }

  override fun updateOkAction() {
    super.updateOkAction()
    isOKActionEnabled = isOKActionEnabled && range != null && maxRowCount != null && skipRowCount != null
    val isRowWiseEnabled = form.rangeTextFieldPanel.isEnabled && range.isNullOrBlank()
    form.maxRowsTextField.isEnabled = isRowWiseEnabled
    form.skipTextField.isEnabled = isRowWiseEnabled
  }

  override fun onFileChanged() {
    updateSheetComboBox(emptyList())
  }

  override fun onUpdateFinished() {
    runAsync {
      val ref = RRef.expressionRef(SHEET_VARIABLE_NAME, interop)
      val sheetNames = ref.getDistinctStrings().filter { it.isNotBlank() }
      updateSheetComboBox(sheetNames)
    }
  }

  private fun updateSheetComboBox(sheetNames: List<String>) {
    runWithDisabled(form.sheetComboBox) {
      val entries = if (sheetNames.isNotEmpty()) sheetNames.map { ComboBoxEntry<String?>(it, it) } else DEFAULT_SHEET_ENTRIES
      sheetDelegate.updateEntries(entries)
    }
  }

  private inner class RangeInputDelegate(panel: JPanel) {
    private val field = JBTextField()

    init {
      field.emptyText.text = RANGE_HINT
      panel.add(field)
      field.addTextChangedListener {
        val errorText = INVALID_RANGE_MESSAGE.takeIf { tryParse() == null }
        setErrorText(errorText, field)
        updateOkAction()
      }
      field.addFocusLostListener {
        updatePreviewAsync()
      }
      field.addActionListener {
        updatePreviewAsync()
      }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
      return tryParse()
    }

    private fun tryParse(): String? {
      val text = field.text.trim()
      if (text.isEmpty()) {
        return text
      }
      tryParsePoint(text, 0)?.let { next ->
        if (next >= text.length) {
          return text
        }
        if (text[next] != ':') {
          return null
        }
        tryParsePoint(text, next + 1)?.let { end ->
          if (end >= text.length) {
            return text
          }
        }
      }
      return null
    }

    private fun tryParsePoint(text: String, start: Int): Int? {
      return tryParseWhile(text, start, Char::isLetter)?.let { next ->
        tryParseWhile(text, next, Char::isDigit)
      }
    }

    private fun tryParseWhile(text: String, start: Int, predicate: (Char) -> Boolean): Int? {
      for (i in start until text.length) {
        if (!predicate(text[i])) {
          return i.takeIf { it > start }
        }
      }
      return text.length.takeIf { it > start }
    }
  }

  companion object {
    private const val SHEET_VARIABLE_NAME = "$PREVIEW_VARIABLE_NAME\$options\$sheets"
    private const val RANGE_HINT = "A1:D10"

    private val INVALID_RANGE_MESSAGE = RBundle.message("import.data.dialog.invalid.range.input.message")
    private val OPTION_DEFAULT = RBundle.message("import.data.dialog.option.default")

    private val DEFAULT_SHEET_ENTRIES = listOf(ComboBoxEntry<String?>(OPTION_DEFAULT, null))
  }
}
