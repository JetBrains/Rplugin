/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.util.ui.JBUI
import org.intellij.datavis.r.inlays.components.DialogUtil
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.run.visualize.forms.RImportCsvDataDialogForm
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import kotlin.reflect.KProperty

class RImportCsvDataDialog(private val project: Project, private val interop: RInterop, parent: Disposable)
  : DialogWrapper(project, null, true, IdeModalityType.IDE, false)
{
  private val fileInputField = TextFieldWithBrowseButton {
    chooseFile()
  }

  private val openFileLinkLabel = LinkLabel<Any>(OPEN_FILE_TEXT, null, LinkListener<Any> { _, _ ->
    chooseFile()
  })

  private val form = RImportCsvDataDialogForm().apply {
    contentPane.preferredSize = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.WIDE)
    okCancelButtonsPanel.add(createOkCancelPanel())
    openFileLinkPanel.add(openFileLinkLabel)
    fileInputFieldPanel.add(fileInputField)
    previewPanel.detach()
    optionPanel.detach()
    noDataLabel.apply {
      foreground = HINT_COLOR
      font = HINT_FONT
    }
    openFileLinkLabel.apply {
      font = HINT_FONT
    }
  }

  private val previewer = RImportDataPreviewer(parent, form.previewPanel)

  private var filePath: String? = null
    set(path) {
      field = path
      fileInputField.text = path?.beautifyPath() ?: ""
      updatePreviewAsync()
    }

  private var variableName by NameFieldDelegate(form.nameTextField)
  private var skipRowCount by IntFieldDelegate(form.skipTextField)
  private var previewRowCount by IntFieldDelegate(form.headTextField)
  private var trimSpaces by CheckBoxDelegate(form.trimSpacesCheckBox)
  private var firstRowAsNames by CheckBoxDelegate(form.firstRowAsNamesCheckBox)
  private var viewAfterImport by CheckBoxDelegate(form.viewAfterImportCheckBox)

  private var delimiter by ComboBoxDelegate(form.delimiterComboBox, DELIMITER_ENTRIES)
  private var quotes by ComboBoxDelegate(form.quotesComboBox, QUOTES_ENTRIES)
  private var escape by ComboBoxDelegate(form.escapeComboBox, ESCAPE_ENTRIES)
  private var comment by ComboBoxDelegate(form.commentComboBox, COMMENT_ENTRIES)
  private var na by ComboBoxDelegate(form.naComboBox, NA_ENTRIES)

  private var currentOptions: Map<String, String>? = null

  init {
    init()
    title = TITLE
    setupInputControls()
    setupPreviewComponent()
    removeMarginsIfPossible()
    updateOkAction()
  }

  override fun createCenterPanel(): JComponent? {
    return form.contentPane
  }

  override fun doOKAction() {
    super.doOKAction()
    importData()
  }

  private fun setupPreviewComponent() {
    val splitter = OnePixelSplitter(false, 1.0f).apply {
      firstComponent = previewer.component
      secondComponent = form.optionPanel
    }
    form.centerPanel.add(splitter)
  }

  private fun setupInputControls() {
    fileInputField.textField.isFocusable = false
    previewRowCount = DEFAULT_PREVIEW_HEAD
    variableName = DEFAULT_VARIABLE_NAME
  }

  private fun chooseFile() {
    val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
      .withDescription(FILE_CHOOSER_DESCRIPTION)
      .withTitle(FILE_CHOOSER_TITLE)
    val dialog = FileChooserDialogImpl(descriptor, project)
    val choice = dialog.choose(project)
    choice.firstOrNull()?.let { file ->
      filePath = file.path
    }
  }

  private fun importData() {
    variableName?.let { name ->
      interop.commitDataImport(name)
      if (viewAfterImport) {
        val ref = RRef.expressionRef(name, interop)
        VisualizeTableHandler.visualizeTable(interop, ref, project, name)
      }
    }
  }

  @Synchronized
  private fun updatePreviewAsync() {
    updateOkAction()
    getPreviewOptions()?.let { options ->
      if (!form.optionPanel.isEnabled) {
        return
      }
      if (options == currentOptions) {
        return
      }
      form.optionPanel.setEnabledRecursively(false)
      form.topPanel.setEnabledRecursively(false)
      previewer.showLoading()
      updateOkAction()
      prepareViewerAsync(options)
        .onSuccess { (viewer, errorCount) ->
          variableName = File(filePath!!).nameWithoutExtension
          previewer.showPreview(viewer, errorCount)
          currentOptions = options
        }
        .onError { e ->
          LOGGER.warn("Unable to update preview", e)
          previewer.closePreview()
          currentOptions = null
          showErrorDialog()
        }
        .onProcessed {
          form.optionPanel.setEnabledRecursively(true)
          form.topPanel.setEnabledRecursively(true)
          updateOkAction()
        }
    }
  }

  private fun showErrorDialog() {
    invokeLater(ModalityState.stateForComponent(form.contentPane)) {
      Messages.showErrorDialog(PREVIEW_FAILURE_DESCRIPTION, PREVIEW_FAILURE_TITLE)
    }
  }

  private fun prepareViewerAsync(options: Map<String, String>): Promise<Pair<RDataFrameViewer, Int>> {
    return preparePreviewRefAsync(options).thenAsync { (ref, errorCount) ->
      interop.dataFrameGetViewer(ref).then { viewer ->
        Pair(viewer, errorCount)
      }
    }
  }

  private fun preparePreviewRefAsync(options: Map<String, String>): Promise<Pair<RRef, Int>> {
    return runAsync {
      val result = interop.previewDataImport(options)
      val errorCount = parseErrorCount(result.stdout, result.stderr)
      val ref = RRef.expressionRef(PREVIEW_DATA_VARIABLE_NAME, interop)
      Pair(ref, errorCount)
    }
  }

  private fun parseErrorCount(output: String, error: String): Int {
    if (output.isBlank()) {
      throw RuntimeException("Cannot get any output from interop\nStderr was: '$error'")
    }
    // Note: expected format
    //  - for failure: "NULL"
    //  - for success: "[1] errorCount\n"
    if (output == "NULL" || output.length < 6 || !output.startsWith("[1]")) {
      throw RuntimeException("Failed to preview data import.\nStdout was: '$output'\nStderr was: '$error'")
    }
    return output.substring(4, output.length - 1).toInt()
  }

  private fun getPreviewOptions(): Map<String, String>? {
    return filePath?.let { path ->
      skipRowCount?.let { skipCount ->
        previewRowCount?.let { previewCount ->
          mutableMapOf<String, String>().also { options ->
            options["importLocation"] = FileUtil.toSystemIndependentName(path).quote()
            options["modelLocation"] = "NULL"
            options["mode"] = "'text'"
            options["maxPreviewRows"] = "$previewCount"
            options["openDataViewer"] = "FALSE"
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
      }
    }
  }

  private fun removeMarginsIfPossible() {
    (rootPane.contentPane as? JPanel)?.let { panel ->
      panel.border = JBUI.Borders.empty()
    }
  }

  private fun createOkCancelPanel(): JComponent {
    val buttons = createActions().map { createJButtonForAction(it) }
    return createButtonsPanel(buttons)
  }

  private fun updateOkAction() {
    form.escapeComboBox.isEnabled = delimiter.let { it != ',' && it != ' ' }
    isOKActionEnabled = filePath != null && variableName != null && skipRowCount != null && previewRowCount != null && previewer.hasPreview
  }

  private fun String.beautifyPath(): String {
    val path = Paths.get(this).beautify()
    return path.takeIf { it.toString().isNotEmpty() }?.toString() ?: PROJECT_DIRECTORY_HINT
  }

  private fun Path.beautify(): Path {
    val basePath = Paths.get(project.basePath!!)
    return if (startsWith(basePath)) basePath.relativize(this) else this
  }

  private inner class NameFieldDelegate(private val field: JTextField) {
    init {
      field.addTextChangedListener {
        val errorText = INVALID_NAME_INPUT_MESSAGE.takeIf { tryParse() == null }
        setErrorText(errorText, field)
        updateOkAction()
      }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
      return tryParse()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
      field.text = value ?: ""
    }

    private fun tryParse(): String? {
      val text = field.text.trim()
      if (text.isBlank()) {
        return null
      }
      for (ch in text) {
        if (!ch.isLetterOrDigit() && ch != '.' && ch != '_') {
          return null
        }
      }
      if (text[0] == '_' || text[0].isDigit()) {
        return null
      }
      if (text.length >= 2 && text[0] == '.' && text[1].isDigit()) {
        return null
      }
      return text
    }
  }

  private inner class IntFieldDelegate(private val field: JTextField) {
    init {
      field.addTextChangedListener {
        val errorText = INVALID_INTEGER_INPUT_MESSAGE.takeIf { tryParse() == null }
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

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
      return tryParse()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
      field.text = (value ?: 0).toString()
    }

    private fun tryParse(): Int? {
      return field.text.toIntOrNull()?.takeIf { it >= 0 }
    }
  }

  private inner class CheckBoxDelegate(private val checkBox: JCheckBox) {
    init {
      checkBox.addItemListener {
        updatePreviewAsync()
      }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
      return checkBox.isSelected
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
      checkBox.isSelected = value
    }
  }

  private inner class ComboBoxDelegate<V>(private val comboBox: JComboBox<Any>, private val entries: List<ComboBoxEntry<V>>) {
    init {
      for (entry in entries) {
        comboBox.addItem(entry.representation)
      }
      comboBox.addItemListener { e ->
        if (e.stateChange == ItemEvent.SELECTED) {
          updatePreviewAsync()
        }
      }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): V {
      return entries[comboBox.selectedIndex].value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
      entries.find { it.value == value }?.let { entry ->
        comboBox.selectedItem = entry
      }
    }
  }

  private data class ComboBoxEntry<V>(val representation: String, val value: V)

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
    private const val DEFAULT_PREVIEW_HEAD = 50
    private const val PREVIEW_DATA_VARIABLE_NAME = ".jetbrains\$previewDataImportResult\$data"

    private val LOGGER = Logger.getInstance(RImportCsvDataDialog::class.java)

    private val TITLE = RBundle.message("import.data.dialog.title")
    private val DEFAULT_VARIABLE_NAME = RBundle.message("import.data.dialog.default.variable.name")
    private val PROJECT_DIRECTORY_HINT = RBundle.message("import.data.dialog.project.directory.hint")
    private val INVALID_NAME_INPUT_MESSAGE = RBundle.message("import.data.dialog.invalid.name.input.message")
    private val INVALID_INTEGER_INPUT_MESSAGE = RBundle.message("import.data.dialog.invalid.integer.input.message")

    private val PREVIEW_FAILURE_TITLE = RBundle.message("import.data.dialog.preview.failure.title")
    private val PREVIEW_FAILURE_DESCRIPTION = RBundle.message("import.data.dialog.preview.failure.description")

    private val OPEN_FILE_TEXT = RBundle.message("import.data.dialog.preview.open.file")
    private val FILE_CHOOSER_TITLE = RBundle.message("import.data.dialog.file.chooser.title")
    private val FILE_CHOOSER_DESCRIPTION = RBundle.message("import.data.dialog.file.chooser.description")

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

    private val HINT_COLOR = JBColor.namedColor("Editor.foreground", JBColor(Gray._80, Gray._160))
    private val HINT_FONT = JBUI.Fonts.label().let { font ->
      font.deriveFont(font.size + 8.0f)
    }

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

    private fun Boolean.toRBoolean(): String {
      return if (this) "TRUE" else "FALSE"
    }

    private fun String.quote(): String {
      val escaped = StringUtil.escapeBackSlashes(this)
      return if (!contains('\'')) "'$escaped'" else "\"$escaped\""
    }

    private fun Char?.quote(): String {
      return this?.quote() ?: "''"
    }

    private fun Char.quote(): String {
      return when (this) {
        '\'' -> "\"'\""
        '\\' -> "'\\\\'"
        else -> "'$this'"
      }
    }

    private fun JComponent.detach() {
      parent.remove(this)
    }

    private fun JComponent.setEnabledRecursively(enabled: Boolean) {
      isEnabled = enabled
      for (component in components) {
        if (component is JComponent) {
          component.setEnabledRecursively(enabled)
        }
      }
    }

    private fun JTextField.addTextChangedListener(listener: (DocumentEvent) -> Unit) {
      document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          listener(e)
        }
      })
    }

    private fun JTextField.addFocusLostListener(listener: (FocusEvent) -> Unit) {
      addFocusListener(object : FocusAdapter() {
        override fun focusLost(e: FocusEvent) {
          listener(e)
        }
      })
    }
  }
}
