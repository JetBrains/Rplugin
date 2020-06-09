/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.*
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.util.ui.JBUI
import org.intellij.datavis.r.inlays.components.BorderlessDialogWrapper
import org.intellij.datavis.r.inlays.components.DialogUtil
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.rinterop.RInterop
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
) : BorderlessDialogWrapper(project, TITLE, IdeModalityType.IDE) {

  private val fileInputField = TextFieldWithBrowseButton {
    filePath = chooseFile()
  }

  private val openFileLinkLabel = LinkLabel<Any>(OPEN_FILE_TEXT, null, LinkListener<Any> { _, _ ->
    filePath = chooseFile()
  })

  private val previewHeadLinkLabel = LinkLabel<Any>("$DEFAULT_PREVIEW_HEAD", COMBO_ICON, LinkListener<Any> { _, _ ->
    choosePreviewHead()
  })

  private val form = RImportDataDialogForm().apply {
    contentPane.preferredSize = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.WIDE)
    previewStatusComboBoxPanel.add(previewHeadLinkLabel)
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

  private val previewer = RImportDataPreviewer(parent, form.previewContentPanel, form.previewStatusPanel)
  private val importer = RDataImporter(interop)

  private var filePath: String? = null
    set(path) {
      if (field != path) {
        field = path
        onFileChanged()
        updateVariableName()
        fileInputField.text = path?.beautifyPath() ?: ""
        updatePreviewAsync()
      }
    }

  private var previewRowCount: Int = DEFAULT_PREVIEW_HEAD
    set(count) {
      if (field != count) {
        field = count
        previewHeadLinkLabel.text = "$count"
        updatePreviewAsync()
      }
    }

  private var variableName by NameFieldDelegate(form.nameTextField)
  private var viewAfterImport by CheckBoxDelegate(form.viewAfterImportCheckBox)

  private var currentConfiguration: RImportConfiguration? = null

  protected abstract val importOptionComponent: JComponent
  protected abstract val importOptions: RImportOptions?
  protected abstract val supportedFormats: List<String>

  override fun init() {
    super.init()
    setupStatusBar()
    setupInputControls()
    setupPreviewComponent()
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
    isOKActionEnabled = filePath != null && variableName != null && previewer.hasPreview
  }

  protected open fun onFileChanged() {
    // Do nothing
  }

  protected open fun onUpdateFinished() {
    // Do nothing
  }

  private fun setupPreviewComponent() {
    val splitter = OnePixelSplitter(false, 0.25f).apply {
      secondComponent = previewer.component
      firstComponent = form.optionPanel
    }
    form.centerPanel.add(splitter)
  }

  private fun setupStatusBar() {
    form.previewStatusPanel.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM or SideBorder.RIGHT)
    previewHeadLinkLabel.horizontalTextPosition = SwingConstants.LEADING
  }

  private fun setupInputControls() {
    form.importOptionPanel.add(importOptionComponent)
    fileInputField.textField.isFocusable = false
    previewRowCount = DEFAULT_PREVIEW_HEAD
    variableName = DEFAULT_VARIABLE_NAME
  }

  private fun choosePreviewHead() {
    val popup = PreviewHeadPopupStep().createPopup()
    popup.showUnderneathOf(form.previewStatusComboBoxPanel)
  }

  private fun chooseFile(): String? {
    return chooseFile(project, supportedFormats)
  }

  private fun importData() {
    variableName?.let { name ->
      collectImportConfiguration()?.let { configuration ->
        val ref = importer.importData(name, configuration.path, configuration.options)
        updateEditorNotifications()
        if (viewAfterImport) {
          VisualizeTableHandler.visualizeTable(interop, ref, project, name)
        }
      }
    }
  }

  private fun updateEditorNotifications() {
    filePath?.let { path ->
      VfsUtil.findFile(Paths.get(path), true)?.let { file ->
        EditorNotifications.getInstance(project).updateNotifications(file)
      }
    }
  }

  private fun updateVariableName() {
    variableName = filePath?.let { File(it).nameWithoutExtension } ?: DEFAULT_VARIABLE_NAME
  }

  @Synchronized
  protected fun updatePreviewAsync() {
    updateOkAction()
    if (!form.optionPanel.isEnabled) {
      return
    }
    collectImportConfiguration()?.let { configuration ->
      if (configuration == currentConfiguration) {
        return
      }
      form.optionPanel.setEnabledRecursively(false)
      previewer.showLoading()
      updateOkAction()
      prepareViewerAsync(configuration)
        .onSuccess { (viewer, errorCount) ->
          previewer.showPreview(viewer, errorCount)
          currentConfiguration = configuration
        }
        .onError { e ->
          LOGGER.warn("Unable to update preview", e)
          previewer.closePreview()
          currentConfiguration = null
          showErrorDialog()
        }
        .onProcessed {
          form.optionPanel.setEnabledRecursively(true)
          onUpdateFinished()
          updateOkAction()
        }
    }
  }

  private fun showErrorDialog() {
    invokeLater(ModalityState.stateForComponent(form.contentPane)) {
      Messages.showErrorDialog(PREVIEW_FAILURE_DESCRIPTION, PREVIEW_FAILURE_TITLE)
    }
  }

  private fun prepareViewerAsync(configuration: RImportConfiguration): Promise<Pair<RDataFrameViewer, Int>> {
    return prepareViewerAsync(configuration.path, configuration.rowCount, configuration.options)
  }

  private fun prepareViewerAsync(path: String, rowCount: Int, options: RImportOptions): Promise<Pair<RDataFrameViewer, Int>> {
    return importer.previewDataAsync(path, rowCount, options).thenAsync { (ref, errorCount) ->
      interop.dataFrameGetViewer(ref).then { viewer ->
        Pair(viewer, errorCount)
      }
    }
  }

  private fun collectImportConfiguration(): RImportConfiguration? {
    return filePath?.let { path ->
      importOptions?.let { options ->
        RImportConfiguration(path, previewRowCount, options)
      }
    }
  }

  private fun String.beautifyPath(): String {
    val path = Paths.get(this).beautify()
    return path.takeIf { it.toString().isNotEmpty() }?.toString() ?: PROJECT_DIRECTORY_HINT
  }

  private fun Path.beautify(): Path {
    val basePath = Paths.get(project.basePath!!)
    return if (startsWith(basePath)) basePath.relativize(this) else this
  }

  private inner class PreviewHeadPopupStep : BaseListPopupStep<Int>(null, PREVIEW_HEAD_VALUES) {
    override fun onChosen(selectedValue: Int, finalChoice: Boolean): PopupStep<*>? {
      return doFinalStep {
        previewRowCount = selectedValue
      }
    }

    fun createPopup(): ListPopup {
      return JBPopupFactory.getInstance().createListPopup(this)
    }
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

  /**
   * **Note:** if input is blank and [isBlankAllowed] is enabled, a result will be set to (-1)
   */
  protected inner class IntFieldDelegate(private val field: JTextField, private val isBlankAllowed: Boolean = false) {
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
      if (isBlankAllowed && field.text.isBlank()) {
        return -1
      }
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

  protected inner class ComboBoxDelegate<V>(private val comboBox: JComboBox<Any>, entries: List<ComboBoxEntry<V>>) {
    private val currentEntries = mutableListOf<ComboBoxEntry<V>>()

    init {
      updateEntries(entries)
      comboBox.addItemListener { e ->
        if (comboBox.isEnabled && e.stateChange == ItemEvent.SELECTED) {
          updatePreviewAsync()
        }
      }
    }

    fun updateEntries(entries: List<ComboBoxEntry<V>>) {
      if (currentEntries != entries) {
        currentEntries.clear()
        currentEntries.addAll(entries)
        comboBox.removeAllItems()
        for (entry in currentEntries) {
          comboBox.addItem(entry.representation)
        }
      }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): V {
      return currentEntries[comboBox.selectedIndex].value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
      currentEntries.find { it.value == value }?.let { entry ->
        comboBox.selectedItem = entry
      }
    }
  }

  protected data class ComboBoxEntry<V>(val representation: String, val value: V)

  private data class RImportConfiguration(val path: String, val rowCount: Int, val options: RImportOptions)

  companion object {
    private const val DEFAULT_PREVIEW_HEAD = 50
    private val PREVIEW_HEAD_VALUES = (1..10).map { it * 10 }

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

    private val COMBO_ICON = AllIcons.Actions.FindAndShowNextMatches

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

    private fun checkFileSupported(file: VirtualFile, supportedFormats: List<String>): Boolean {
      return file.extension?.let { it.toLowerCase() in supportedFormats } ?: false
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

    fun JTextField.addTextChangedListener(listener: (DocumentEvent) -> Unit) {
      document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          listener(e)
        }
      })
    }

    fun JTextField.addFocusLostListener(listener: (FocusEvent) -> Unit) {
      addFocusListener(object : FocusAdapter() {
        override fun focusLost(e: FocusEvent) {
          listener(e)
        }
      })
    }

    fun runWithDisabled(component: JComponent, task: () -> Unit) {
      val isEnabled = component.isEnabled
      component.isEnabled = false
      task()
      component.isEnabled = isEnabled
    }

    fun String?.orChooseFile(project: Project, supportedFormats: List<String>): String? {
      return this ?: chooseFile(project, supportedFormats)
    }

    private fun chooseFile(project: Project, supportedFormats: List<String>): String? {
      val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        .withFileFilter { checkFileSupported(it, supportedFormats) }
        .withDescription(FILE_CHOOSER_DESCRIPTION)
        .withTitle(FILE_CHOOSER_TITLE)
      val dialog = FileChooserDialogImpl(descriptor, project)
      val choice = dialog.choose(project)
      return choice.firstOrNull()?.path
    }
  }
}
