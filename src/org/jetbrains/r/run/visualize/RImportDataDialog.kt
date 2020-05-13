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
import org.jetbrains.r.run.visualize.forms.RImportDataDialogForm
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import kotlin.reflect.KProperty

abstract class RImportDataDialog(
  protected val project: Project,
  protected val interop: RInterop,
  parent: Disposable,
  private val initialPath: String? = null
) : DialogWrapper(project, null, true, IdeModalityType.IDE, false) {

  private val fileInputField = TextFieldWithBrowseButton {
    chooseFile()
  }

  private val openFileLinkLabel = LinkLabel<Any>(OPEN_FILE_TEXT, null, LinkListener<Any> { _, _ ->
    chooseFile()
  })

  private val form = RImportDataDialogForm().apply {
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
      updateVariableName()
      fileInputField.text = path?.beautifyPath() ?: ""
      updatePreviewAsync()
    }

  private var variableName by NameFieldDelegate(form.nameTextField)
  private var previewRowCount by IntFieldDelegate(form.headTextField)
  private var viewAfterImport by CheckBoxDelegate(form.viewAfterImportCheckBox)

  private var currentOptions: ImportOptions? = null

  protected abstract val importOptionComponent: JComponent
  protected abstract val additionalOptions: Map<String, String>?
  protected abstract val supportedFormats: List<String>
  protected abstract val importMode: String

  override fun init() {
    super.init()
    title = TITLE
    setupInputControls()
    setupPreviewComponent()
    removeMarginsIfPossible()
    filePath = initialPath
  }

  override fun createCenterPanel(): JComponent? {
    return form.contentPane
  }

  override fun doOKAction() {
    super.doOKAction()
    importData()
  }

  protected open fun updateOkAction() {
    isOKActionEnabled = filePath != null && variableName != null && previewRowCount != null && previewer.hasPreview
  }

  private fun setupPreviewComponent() {
    val splitter = OnePixelSplitter(false, 1.0f).apply {
      firstComponent = previewer.component
      secondComponent = form.optionPanel
    }
    form.centerPanel.add(splitter)
  }

  private fun setupInputControls() {
    form.importOptionPanel.add(importOptionComponent)
    fileInputField.textField.isFocusable = false
    previewRowCount = DEFAULT_PREVIEW_HEAD
    variableName = DEFAULT_VARIABLE_NAME
  }

  private fun chooseFile() {
    val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
      .withFileFilter { it.extension?.isSupportedFormat ?: false }
      .withDescription(FILE_CHOOSER_DESCRIPTION)
      .withTitle(FILE_CHOOSER_TITLE)
    val dialog = FileChooserDialogImpl(descriptor, project)
    val choice = dialog.choose(project)
    choice.firstOrNull()?.let { file ->
      filePath = file.path
    }
  }

  private val String.isSupportedFormat: Boolean
    get() = toLowerCase() in supportedFormats

  private fun importData() {
    variableName?.let { name ->
      collectImportOptions()?.let { options ->
        interop.commitDataImport(name, options.path, options.mode, options.additional)
        if (viewAfterImport) {
          val ref = RRef.expressionRef(name, interop)
          VisualizeTableHandler.visualizeTable(interop, ref, project, name)
        }
      }
    }
  }

  private fun updateVariableName() {
    variableName = filePath?.let { File(it).nameWithoutExtension } ?: DEFAULT_VARIABLE_NAME
  }

  @Synchronized
  private fun updatePreviewAsync() {
    updateOkAction()
    collectImportOptions()?.let { options ->
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

  private fun prepareViewerAsync(options: ImportOptions): Promise<Pair<RDataFrameViewer, Int>> {
    return preparePreviewRefAsync(options).thenAsync { (ref, errorCount) ->
      interop.dataFrameGetViewer(ref).then { viewer ->
        Pair(viewer, errorCount)
      }
    }
  }

  private fun preparePreviewRefAsync(options: ImportOptions): Promise<Pair<RRef, Int>> {
    return runAsync {
      val result = interop.previewDataImport(options.path, options.mode, options.rowCount, options.additional)
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

  private fun collectImportOptions(): ImportOptions? {
    return filePath?.let { path ->
      previewRowCount?.let { rowCount ->
        additionalOptions?.let { additional ->
          ImportOptions(FileUtil.toSystemIndependentName(path), importMode, rowCount, additional)
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

  protected inner class TextFieldDelegate(private val field: JTextField) {
    init {
      field.addFocusLostListener {
        updatePreviewAsync()
      }
      field.addActionListener {
        updatePreviewAsync()
      }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
      return field.text ?: ""
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
      field.text = value
    }
  }

  protected inner class IntFieldDelegate(private val field: JTextField) {
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

  protected inner class CheckBoxDelegate(private val checkBox: JCheckBox) {
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

  protected inner class ComboBoxDelegate<V>(private val comboBox: JComboBox<Any>, private val entries: List<ComboBoxEntry<V>>) {
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

  protected data class ComboBoxEntry<V>(val representation: String, val value: V)

  private data class ImportOptions(val path: String, val mode: String, val rowCount: Int, val additional: Map<String, String>)

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

    private val HINT_COLOR = JBColor.namedColor("Editor.foreground", JBColor(Gray._80, Gray._160))
    private val HINT_FONT = JBUI.Fonts.label().let { font ->
      font.deriveFont(font.size + 8.0f)
    }

    fun Boolean.toRBoolean(): String {
      return if (this) "TRUE" else "FALSE"
    }

    fun String.quote(): String {
      val escaped = StringUtil.escapeBackSlashes(this)
      return if (!contains('\'')) "'$escaped'" else "\"$escaped\""
    }

    fun Char?.quote(): String {
      return this?.quote() ?: "''"
    }

    fun Char.quote(): String {
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
