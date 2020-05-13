/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.run.visualize.forms.RImportCsvOptionPanelForm
import javax.swing.JComponent

class RImportCsvDataDialog(project: Project, interop: RInterop, parent: Disposable, initialPath: String? = null)
  : RImportDataDialog(project, interop, parent, initialPath)
{
  private val form = RImportCsvOptionPanelForm()

  private var skipRowCount by IntFieldDelegate(form.skipTextField)
  private var trimSpaces by CheckBoxDelegate(form.trimSpacesCheckBox)
  private var firstRowAsNames by CheckBoxDelegate(form.firstRowAsNamesCheckBox)

  private var delimiter by ComboBoxDelegate(form.delimiterComboBox, DELIMITER_ENTRIES)
  private var quotes by ComboBoxDelegate(form.quotesComboBox, QUOTES_ENTRIES)
  private var escape by ComboBoxDelegate(form.escapeComboBox, ESCAPE_ENTRIES)
  private var comment by ComboBoxDelegate(form.commentComboBox, COMMENT_ENTRIES)
  private var na by ComboBoxDelegate(form.naComboBox, NA_ENTRIES)

  override val importOptionComponent: JComponent = form.contentPane

  override val additionalOptions: Map<String, String>?
    get() = skipRowCount?.let { skipCount ->
      mutableMapOf<String, String>().also { options ->
        options["skip"] = skipCount.toString()
        options["columnNames"] = firstRowAsNames.toRBoolean()
        options["trimSpaces"] = trimSpaces.toRBoolean()
        options["delimiter"] = delimiter.quote()
        if (quotes != QuoteKind.DEFAULT) {
          options["quotes"] = quotes.toChar().quote()
        }
        if (form.escapeComboBox.isEnabled) {
          options["escapeBackslash"] = escape.let { it == EscapeKind.BACKSLASH || it == EscapeKind.BOTH }.toRBoolean()
          options["escapeDouble"] = escape.let { it == EscapeKind.DOUBLE || it == EscapeKind.BOTH }.toRBoolean()
        }
        comment?.let { comment ->
          options["comments"] = comment.quote()
        }
        na?.let { na ->
          options["na"] = na.quote()
        }
      }
    }

  override val supportedFormats = RImportDataUtil.supportedTextFormats
  override val importMode = "text"

  init {
    init()
  }

  override fun updateOkAction() {
    super.updateOkAction()
    isOKActionEnabled = isOKActionEnabled && skipRowCount != null
    form.escapeComboBox.isEnabled = delimiter.let { it != ',' && it != ' ' }
  }

  private enum class QuoteKind {
    DEFAULT,
    DOUBLE,
    SINGLE,
    NONE;

    fun toChar(): Char? {
      return when (this) {
        DOUBLE -> '\"'
        SINGLE -> '\''
        else -> null
      }
    }
  }

  private enum class EscapeKind {
    BACKSLASH,
    DOUBLE,
    BOTH,
    NONE,
  }

  companion object {
    private val OPTION_NONE = RBundle.message("import.data.dialog.option.none")
    private val OPTION_DEFAULT = RBundle.message("import.data.dialog.option.default")
    private val OPTION_EMPTY_STRING = RBundle.message("import.data.dialog.option.empty.string")

    private val DELIMITER_TAB = RBundle.message("import.data.dialog.delimiter.tab")
    private val DELIMITER_SPACE = RBundle.message("import.data.dialog.delimiter.space")
    private val DELIMITER_COMMA = RBundle.message("import.data.dialog.delimiter.comma")
    private val DELIMITER_SEMICOLON = RBundle.message("import.data.dialog.delimiter.semicolon")

    private val QUOTES_SINGLE = RBundle.message("import.data.dialog.quotes.single")
    private val QUOTES_DOUBLE = RBundle.message("import.data.dialog.quotes.double")

    private val ESCAPE_BOTH = RBundle.message("import.data.dialog.escape.both")
    private val ESCAPE_DOUBLE = RBundle.message("import.data.dialog.escape.double")
    private val ESCAPE_BACKSLASH = RBundle.message("import.data.dialog.escape.backslash")

    private val DELIMITER_ENTRIES = listOf(
      ComboBoxEntry(DELIMITER_COMMA, ','),
      ComboBoxEntry(DELIMITER_SEMICOLON, ';'),
      ComboBoxEntry(DELIMITER_TAB, '\t'),
      ComboBoxEntry(DELIMITER_SPACE, ' ')
    )

    private val QUOTES_ENTRIES = listOf(
      ComboBoxEntry(OPTION_DEFAULT, QuoteKind.DEFAULT),
      ComboBoxEntry(QUOTES_DOUBLE, QuoteKind.DOUBLE),
      ComboBoxEntry(QUOTES_SINGLE, QuoteKind.SINGLE),
      ComboBoxEntry(OPTION_NONE, QuoteKind.NONE)
    )

    private val ESCAPE_ENTRIES = listOf(
      ComboBoxEntry(OPTION_NONE, EscapeKind.NONE),
      ComboBoxEntry(ESCAPE_BACKSLASH, EscapeKind.BACKSLASH),
      ComboBoxEntry(ESCAPE_DOUBLE, EscapeKind.DOUBLE),
      ComboBoxEntry(ESCAPE_BOTH, EscapeKind.BOTH)
    )

    private val COMMENT_ENTRIES = listOf(null, "#", "%", "//", "'", "!", ";", "--", "*", "||", "\"", "\\", "*>").map { value ->
      if (value != null) ComboBoxEntry<String?>(value, value) else ComboBoxEntry<String?>(OPTION_DEFAULT, null)
    }

    private val NA_ENTRIES = listOf(
      ComboBoxEntry<String?>(OPTION_DEFAULT, null),
      ComboBoxEntry<String?>("NA", "NA"),
      ComboBoxEntry<String?>("null", "null"),
      ComboBoxEntry<String?>("0", "0"),
      ComboBoxEntry<String?>(OPTION_EMPTY_STRING, "")
    )
  }
}
