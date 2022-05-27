/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.LocalOrRemotePath
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.run.visualize.forms.RImportBaseOptionPanelForm
import javax.swing.JComponent

class RImportBaseDataDialog private constructor(project: Project, interop: RInterop, parent: Disposable, initialPath: LocalOrRemotePath)
  : RImportDataDialog(project, interop, parent, initialPath)
{
  @TestOnly
  constructor(project: Project, interop: RInterop, initialPath: LocalOrRemotePath) : this(project, interop, project, initialPath)

  private val form = RImportBaseOptionPanelForm()

  private var firstRowAsNames by CheckBoxDelegate(form.firstRowAsNamesCheckBox)
  private var stringsAsFactors by CheckBoxDelegate(form.stringsAsFactorsCheckBox)

  private var encoding by ComboBoxDelegate(form.encodingComboBox, ENCODING_ENTRIES)
  private var rowNames by ComboBoxDelegate(form.rowNamesComboBox, ROW_NAMES_ENTRIES)
  private var delimiter by ComboBoxDelegate(form.delimiterComboBox, DELIMITER_ENTRIES)
  private var decimal by ComboBoxDelegate(form.decimalComboBox, DECIMAL_ENTRIES)
  private var quotes by ComboBoxDelegate(form.quotesComboBox, QUOTES_ENTRIES)
  private var comment by ComboBoxDelegate(form.commentComboBox, COMMENT_ENTRIES)

  private var na by TextFieldDelegate(form.naTextField)

  override val importOptionComponent: JComponent = form.contentPane

  override val importOptions: RImportOptions
    get() = collectOptions(firstRowAsNames, stringsAsFactors, encoding, rowNames, delimiter, decimal, quotes, comment, na)

  override val supportedFormats = RImportDataUtil.supportedTextFormats

  init {
    init()
  }

  companion object {
    private val OPTION_NONE = RBundle.message("import.data.dialog.option.none")
    private val OPTION_AUTOMATIC = RBundle.message("import.data.dialog.option.automatic")

    private val ENCODING_UTF = RBundle.message("import.data.dialog.encoding.utf")
    private val ENCODING_LATIN = RBundle.message("import.data.dialog.encoding.latin")

    private val ROW_NAMES_NUMBERS = RBundle.message("import.data.dialog.row.names.numbers")
    private val ROW_NAMES_FIRST_COLUMN = RBundle.message("import.data.dialog.row.names.first.column")

    private val DELIMITER_TAB = RBundle.message("import.data.dialog.delimiter.tab")
    private val DELIMITER_COMMA = RBundle.message("import.data.dialog.delimiter.comma")
    private val DELIMITER_SEMICOLON = RBundle.message("import.data.dialog.delimiter.semicolon")
    private val DELIMITER_WHITESPACE = RBundle.message("import.data.dialog.delimiter.whitespace")

    private val DECIMAL_PERIOD = RBundle.message("import.data.dialog.decimal.period")

    private val QUOTES_SINGLE = RBundle.message("import.data.dialog.quotes.single")
    private val QUOTES_DOUBLE = RBundle.message("import.data.dialog.quotes.double")

    private val ENCODING_ENTRIES = listOf(
      ComboBoxEntry(OPTION_AUTOMATIC, "unknown"),
      ComboBoxEntry(ENCODING_LATIN, "latin1"),
      ComboBoxEntry(ENCODING_UTF, "UTF-8")
    )

    private val ROW_NAMES_ENTRIES = listOf<ComboBoxEntry<String?>>(
      ComboBoxEntry(OPTION_AUTOMATIC, null),
      ComboBoxEntry(ROW_NAMES_FIRST_COLUMN, "c(1)"),
      ComboBoxEntry(ROW_NAMES_NUMBERS, "NULL")
    )

    private val DELIMITER_ENTRIES = listOf(
      ComboBoxEntry(DELIMITER_COMMA, ","),
      ComboBoxEntry(DELIMITER_SEMICOLON, ";"),
      ComboBoxEntry(DELIMITER_TAB, "\t"),
      ComboBoxEntry(DELIMITER_WHITESPACE, "")
    )

    private val DECIMAL_ENTRIES = listOf(
      ComboBoxEntry(DECIMAL_PERIOD, '.'),
      ComboBoxEntry(DELIMITER_COMMA, ',')
    )

    private val QUOTES_ENTRIES = listOf(
      ComboBoxEntry(QUOTES_DOUBLE, "\""),
      ComboBoxEntry(QUOTES_SINGLE, "'"),
      ComboBoxEntry(OPTION_NONE, "")
    )

    private val COMMENT_ENTRIES = listOf(null, "#", "!", "%", "@", "/", "~").map { comment ->
      if (comment == null) ComboBoxEntry(OPTION_NONE, "") else ComboBoxEntry(comment, comment)
    }

    fun show(project: Project, interop: RInterop, parent: Disposable, initialPath: LocalOrRemotePath? = null) {
      initialPath.orChooseFile(interop.interpreter, RImportDataUtil.supportedTextFormats)?.let { path ->
        RImportBaseDataDialog(project, interop, parent, path).show()
      }
    }

    fun collectOptions(
      firstRowAsNames: Boolean = true,
      stringsAsFactors: Boolean = true,
      encoding: String = "unknown",
      rowNames: String? = null,
      delimiter: String = ",",
      decimal: Char = '.',
      quotes: String = "\"",
      comment: String = "",
      na: String = "NA"
    ): RImportOptions {
      val additional = mutableMapOf<String, String>().also { options ->
        options["header"] = firstRowAsNames.toRBoolean()
        options["stringsAsFactors"] = stringsAsFactors.toRBoolean()
        options["encoding"] = encoding.quote()
        rowNames?.let { rowNames ->
          options["row.names"] = rowNames
        }
        options["sep"] = delimiter.quote()
        options["dec"] = decimal.quote()
        options["quote"] = quotes.quote()
        options["comment.char"] = comment.quote()
        options["na.strings"] = na.quote()
      }
      return RImportOptions("base", additional)
    }
  }
}
