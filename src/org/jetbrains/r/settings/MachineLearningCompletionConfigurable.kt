package org.jetbrains.r.settings

import com.intellij.CommonBundle
import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import com.intellij.util.text.DateFormatUtil
import org.eclipse.aether.version.Version
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.model.updater.*
import java.awt.Component
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JLabel


class MachineLearningCompletionConfigurable : BoundConfigurable(RBundle.message("project.settings.ml.completion.name")) {

  companion object {
    private const val PORT_FIELD_WIDTH = 5
    private val settings = MachineLearningCompletionSettings.getInstance()

    private fun updateLastCheckedLabel(label: JLabel, time: Long): Unit = when {
      time <= 0 -> label.text = IdeBundle.message("updates.last.check.never")
      else -> {
        label.text = DateFormatUtil.formatPrettyDateTime(time)
        label.toolTipText = DateFormatUtil.formatDate(time) + ' ' + DateFormatUtil.formatTimeWithSeconds(time)
      }
    }

    private fun updateVersionLabel(label: JLabel, version: Version?) {
      label.text = version?.toString() ?: RBundle.message("project.settings.ml.completion.version.none")
    }
  }

  private val modality = ModalityState.current()

  private fun invokeLaterWithTabModality(action: () -> Unit) = ApplicationManager.getApplication().invokeLater(action, modality)

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(displayName) {
        row {
          checkForUpdatesButton()
        }

        row {
          row(IdeBundle.message("updates.settings.last.check")) { lastCheckedLabel().withLeftGap() }
          row(RBundle.message("project.settings.ml.completion.version.app")) {
            versionLabel(MachineLearningCompletionAppArtifact()).withLeftGap()
          }
          row(RBundle.message("project.settings.ml.completion.version.model")) {
            versionLabel(MachineLearningCompletionModelArtifact()).withLeftGap()
          }.largeGapAfter()
        }

        row {
          val enableCompletionCheckbox = checkBox(RBundle.message("project.settings.ml.completion.checkbox"),
                                                  settings.state::isEnabled)
          row {
            label(RBundle.message("project.settings.ml.completion.server.label"))
          }
          row {
            cell(isFullWidth = true) {
              label(RBundle.message("project.settings.ml.completion.host.label"))
              textField({ settings.state.host ?: "" }, settings.state::host.setter)
                .enableIf(enableCompletionCheckbox.selected)
              label(RBundle.message("project.settings.ml.completion.port.label"))
              intTextField(settings.state::port, columns = PORT_FIELD_WIDTH)
                .enableIf(enableCompletionCheckbox.selected)
            }
          }
        }
      }
    }
  }

  private fun Cell.lastCheckedLabel(): CellBuilder<JLabel> {
    val label = JBLabel()
    updateLastCheckedLabel(label, settings.state.lastCheckedForUpdatesMs)

    disposable?.let {
      MachineLearningCompletionSettingsChangeListener { beforeState, afterState ->
        if (beforeState.lastCheckedForUpdatesMs != afterState.lastCheckedForUpdatesMs) {
          invokeLaterWithTabModality {
            updateLastCheckedLabel(label, afterState.lastCheckedForUpdatesMs)
          }
        }
      }.subscribeWithDisposable(it)
    }

    return component(label)
  }

  private fun Cell.versionLabel(artifact: MachineLearningCompletionRemoteArtifact): CellBuilder<JLabel> {
    val label = JBLabel()
    updateVersionLabel(label, artifact.currentVersion)

    disposable?.let { disposable ->
      MachineLearningCompletionModelFilesService.getInstance().registerVersionChangeListener(artifact, disposable) {
        invokeLaterWithTabModality { updateVersionLabel(label, it) }
      }
    }

    return component(label)
  }

  override fun apply() {
    val beforeState = settings.copyState()
    super.apply()
    MachineLearningCompletionSettingsChangeListener.notifySettingsChanged(beforeState, settings.state)
  }

  private fun Cell.checkForUpdatesButton() = button(RBundle.message("project.settings.ml.completion.button.checkForUpdates")) {
    MachineLearningCompletionDownloadModelService.getInstance().initiateUpdateCycle(true, false) { (artifacts, size) ->
      if (artifacts.isNotEmpty()) {
        val pressedUpdate = createUpdateDialog(artifacts, size).showAndGet()
        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(pressedUpdate)
      }
      else {
        createNoAvailableUpdateDialog().show()
        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
      }
    }
  }.enableIf(object : ComponentPredicate() {
    override fun invoke(): Boolean =
      !MachineLearningCompletionDownloadModelService.isBeingDownloaded.get()

    override fun addListener(listener: (Boolean) -> Unit) {
      this@MachineLearningCompletionConfigurable.disposable?.let { parentDisposable ->
        MachineLearningCompletionDownloadModelService.isBeingDownloaded
          .afterChange({ listener(!it) }, parentDisposable)
      }
    }
  })

  private fun createUpdateDialog(artifacts: List<MachineLearningCompletionRemoteArtifact>,
                                 size: Long) =
    dialog(displayName,
           panel = panel {
             row {
               label(RBundle.message("notification.ml.update.askForUpdate.content", UpdateUtils.showSizeMb(size)))
             }
           },
           createActions = {
             listOf(ButtonAction(CommonBundle.getCancelButtonText(), DialogWrapper.CANCEL_EXIT_CODE),
                    ButtonAction(IdeBundle.message("plugins.configurable.update.button"), DialogWrapper.OK_EXIT_CODE) {
                      val updateAction = MachineLearningCompletionUpdateAction(null, artifacts)
                      updateAction.performAsync()
                    })
           }
    )

  private fun createNoAvailableUpdateDialog(): DialogWrapper =
    dialog(displayName,
           panel = panel {
             row {
               label(RBundle.message("project.settings.ml.completion.dialog.noUpdates"))
             }
           },
           createActions = { listOf(ButtonAction(CommonBundle.getOkButtonText(), DialogWrapper.OK_EXIT_CODE)) }
    )

  private class ButtonAction(name: String,
                             private val exitCode: Int,
                             private val action: ((ActionEvent) -> Unit)? = null) : AbstractAction(name) {
    init {
      putValue(DialogWrapper.DEFAULT_ACTION, exitCode == DialogWrapper.OK_EXIT_CODE)
    }

    override fun actionPerformed(event: ActionEvent) {
      action?.invoke(event)
      val wrapper = DialogWrapper.findInstance(event.source as? Component)
      wrapper?.close(exitCode)
    }
  }
}
