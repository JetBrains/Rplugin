/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.showErrorDialog
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.r.psi.interpreter.LocalOrRemotePath
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.r.psi.interpreter.isLocal
import com.intellij.r.psi.run.visualize.RVisualization
import com.intellij.r.psi.visualization.inlays.components.DialogUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.EditorNotifications.getInstance
import com.intellij.ui.Gray
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.components.ActionLink
import com.intellij.util.PathUtil
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.await
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.run.visualize.forms.RImportDataDialogForm
import org.jetbrains.r.visualization.inlays.components.BorderlessDialogWrapper
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KProperty

abstract class RImportDataDialog(
  protected val project: Project,
  protected val interop: RInteropImpl,
  parent: Disposable,
  private val initialPath: LocalOrRemotePath? = null
) : BorderlessDialogWrapper(project, RBundle.message("import.data.dialog.title"), IdeModalityType.IDE) {
  private val interpreter get() = interop.interpreter

  private val fileInputField = TextFieldWithBrowseButton {
    filePath = chooseFile() ?: filePath
  }

  private val openFileLinkLabel = ActionLink(RBundle.message("import.data.dialog.preview.open.file")) {
    filePath = chooseFile() ?: filePath
  }

  private val previewHeadLinkLabel = ActionLink("$DEFAULT_PREVIEW_HEAD") {
    choosePreviewHead()
  }.apply {
    icon = COMBO_ICON
  }

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

  private var filePath: LocalOrRemotePath? = null
    set(path) {
      if (field != path) {
        field = path
        onFileChanged()
        updateVariableName()
        fileInputField.text = path?.path?.beautifyPath() ?: ""
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
  protected abstract val supportedFormats: Array<String>

  @TestOnly
  internal fun variableName(): String? = variableName

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
    onUpdateAdditional?.let { it() }
  }

  @TestOnly
  var onUpdateAdditional: (() -> Unit)? = null

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
    variableName = RBundle.message("import.data.dialog.default.variable.name")
  }

  private fun choosePreviewHead() {
    val popup = PreviewHeadPopupStep().createPopup()
    popup.showUnderneathOf(form.previewStatusComboBoxPanel)
  }

  private fun chooseFile(): LocalOrRemotePath? = chooseFile(interpreter, supportedFormats)

  private fun importData() {
    variableName?.let { name ->
      collectImportConfiguration()?.let { configuration ->
        val ref = importer.importData(name, configuration.path, configuration.options)

        RPluginCoroutineScope.getScope(project).launch(ModalityState.defaultModalityState().asContextElement()) {
          filePath?.findFile(interpreter)?.let { getInstance(project).updateNotifications(it) }
          if (viewAfterImport) {
            RVisualization.getInstance(project).visualizeTable(interop, ref, name)
          }
        }
      }
    }
  }

  /**
   * Clears out the given string from restricted characters
   *
   * Steps:
   * - Replace all characters except a-zA-Z0-9 and `.` with `_`
   * - Replace all continuous `_` with only one
   * - Remove tailing `_`
   *
   * @return modified string, may be empty
   */
  private fun replaceRestrictedCharacters(name: String): String {
    return name.replace(Regex("[^\\w|.]"), "_").replace(Regex("_+"), "_").replace(Regex("_$"), "")
  }

  private fun updateVariableName() {
    var name = filePath?.let { FileUtilRt.getNameWithoutExtension(PathUtil.getFileName(it.path)) }
    if (name.isNullOrBlank()) {
      name = RBundle.message("import.data.dialog.default.variable.name")
    } else {
      name = replaceRestrictedCharacters(name)
      if (name.isBlank() || name[0] == '_' || name[0].isDigit() || (name.length >= 2 && name[0] == '.' && name[1].isDigit()))
        name = "dataset${name.replace(Regex("^[_.]"), "").let { if (it.isBlank()) "" else "_$it" }}"
    }
    variableName = name
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

      RPluginCoroutineScope.getScope(project).launch(Dispatchers.EDT + ModalityState.stateForComponent(form.contentPane).asContextElement()) {
        try {
          val (viewer, errorCount) = prepareViewerAsync(configuration)
          previewer.showPreview(viewer, errorCount)
          currentConfiguration = configuration
        }
        catch (e: CancellationException) {
          throw e
        }
        catch (e: Exception) {
          logger<RImportDataDialog>().warn("Unable to update preview", e)
          previewer.closePreview()
          currentConfiguration = null
          showErrorDialog(RBundle.message("import.data.dialog.preview.failure.description"), RBundle.message("import.data.dialog.preview.failure.title"))
        }
        finally {
          form.optionPanel.setEnabledRecursively(true)
          onUpdateFinished()
          updateOkAction()
        }
      }
    }
  }

  private suspend fun prepareViewerAsync(configuration: RImportConfiguration): Pair<RDataFrameViewer, Int> {
    val (ref, errorCount) = importer.previewDataAsync(configuration.path, configuration.rowCount, configuration.options).await()
    val viewer = interop.dataFrameGetViewer(ref).await()
    return Pair(viewer, errorCount)
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
    return path.takeIf { it.toString().isNotEmpty() }?.toString() ?: RBundle.message("import.data.dialog.project.directory.hint")
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
        val errorText = RBundle.message("import.data.dialog.invalid.name.input.message").takeIf { tryParse() == null }
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
        val errorText = RBundle.message("import.data.dialog.invalid.integer.input.message").takeIf { tryParse() == null }
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

  private data class RImportConfiguration(val path: LocalOrRemotePath, val rowCount: Int, val options: RImportOptions)

  companion object {
    private const val DEFAULT_PREVIEW_HEAD = 50
    private val PREVIEW_HEAD_VALUES = (1..10).map { it * 10 }

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

    fun LocalOrRemotePath?.orChooseFile(interpreter: RInterpreter, supportedFormats: Array<String>): LocalOrRemotePath? =
      this ?: chooseFile(interpreter, supportedFormats)

    private fun chooseFile(interpreter: RInterpreter, supportedFormats: Array<String>): LocalOrRemotePath? {
      val project = interpreter.project
      val isRemote: Boolean
      if (interpreter.isLocal()) {
        isRemote = false
      } else {
        val buttons = arrayOf(
          RBundle.message("import.data.dialog.choose.host.local"),
          RBundle.message("import.data.dialog.choose.host.remote"),
          RBundle.message("import.data.dialog.choose.host.cancel")
        )
        val result = Messages.showDialog(
          project, RBundle.message("import.data.dialog.choose.host.message"), RBundle.message("import.data.dialog.file.chooser.title"), buttons,
          0, Messages.getQuestionIcon())
        isRemote = when (result) {
          0 -> false
          1 -> true
          else -> return null
        }
      }

      if (isRemote) {
        val path = interpreter.showFileChooserDialogForHost() ?: return null
        return LocalOrRemotePath(path, true)
      }
      else {
        val descriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
          .withExtensionFilter(RBundle.message("import.data.dialog.file.chooser.label"), *supportedFormats)
          .withDescription(RBundle.message("import.data.dialog.file.chooser.description"))
          .withTitle(RBundle.message("import.data.dialog.file.chooser.title"))
        val dialog = FileChooserDialogImpl(descriptor, project)
        val choice = dialog.choose(project)
        return choice.firstOrNull()?.path?.let { LocalOrRemotePath(it, false) }
      }
    }
  }
}

private fun LocalOrRemotePath.findFile(interpreter: RInterpreter, refreshIfNeeded: Boolean = false): VirtualFile? {
  if (isRemote) return interpreter.findFileByPathAtHost(path)
  return VfsUtil.findFile(Paths.get(path), refreshIfNeeded)
}
