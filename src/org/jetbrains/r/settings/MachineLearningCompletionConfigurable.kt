package org.jetbrains.r.settings

import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import com.intellij.util.text.DateFormatUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionDownloadModelService
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionLastCheckForUpdatesInfo
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionRemoteArtifact
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionUpdateAction
import java.awt.Component
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JLabel
import kotlin.reflect.KProperty


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
  }

  private val lastCheckedTimeLabel = JLabel().apply {
    updateLastCheckedLabel(this, MachineLearningCompletionLastCheckForUpdatesInfo.lastUpdateCheckTimeMs)
  }

  private val infoLabelListener: (KProperty<*>, Long, Long) -> Unit

  init {
    val modality = ModalityState.current()
    infoLabelListener = { _: KProperty<*>, _: Long, newDate: Long ->
      ApplicationManager.getApplication().invokeLater({ updateLastCheckedLabel(lastCheckedTimeLabel, newDate) }, modality)
    }
    MachineLearningCompletionLastCheckForUpdatesInfo.subscribe(infoLabelListener)
  }

  override fun disposeUIResources() {
    super.disposeUIResources()
    MachineLearningCompletionLastCheckForUpdatesInfo.unsubscribe(infoLabelListener)
  }

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(displayName) {
        row {
          createCheckForUpdatesButton()
        }

        row {
          val filesService = MachineLearningCompletionModelFilesService.getInstance()
          row(IdeBundle.message("updates.settings.last.check")) { component(lastCheckedTimeLabel).withLeftGap() }
          row("Application version:") { label(filesService.applicationVersion?.toString() ?: "none").withLeftGap() }
          row("Model version:") { label(filesService.modelVersion?.toString() ?: "none").withLeftGap() }
            .largeGapAfter()
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

  override fun apply() {
    val beforeState = settings.copyState()
    super.apply()
    notifySettingsChanged(beforeState, settings.state)
  }

  private fun notifySettingsChanged(beforeState: MachineLearningCompletionSettings.State,
                                    afterState: MachineLearningCompletionSettings.State) {
    ApplicationManager.getApplication().messageBus.syncPublisher(R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC)
      .settingsChanged(beforeState, afterState)
  }

  private fun Cell.createCheckForUpdatesButton() = button("Check for Updates...") {
    MachineLearningCompletionDownloadModelService.getInstance().initiateUpdateCycle(true, false) { (artifacts, size) ->
      if (artifacts.isNotEmpty()) {
        val pressedUpdate = createUpdateDialog(null, artifacts, size).showAndGet()
        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(pressedUpdate)
      }
      else {
        createNoAvailableUpdateDialog(null).show()
        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
      }
    }
  }.enableIf(object : ComponentPredicate() {
    override fun invoke(): Boolean =
      !MachineLearningCompletionDownloadModelService.isBeingDownloaded.get()

    override fun addListener(listener: (Boolean) -> Unit) =
      MachineLearningCompletionDownloadModelService.isBeingDownloaded.afterChange { newValue ->
        listener(!newValue)
      }
  })

  private fun createUpdateDialog(project: Project?,
                                 artifacts: List<MachineLearningCompletionRemoteArtifact>,
                                 size: Long) =
    dialog("R Machine Learning completion update",
           panel = panel {
             row {
               label("Update is available $size bytes")
             }
           },
           project = project,
           createActions = {
             listOf(ButtonAction("Cancel", DialogWrapper.CANCEL_EXIT_CODE),
                    ButtonAction("Update", DialogWrapper.OK_EXIT_CODE) {
                      val updateAction = MachineLearningCompletionUpdateAction(project, artifacts)
                      updateAction.performAsync()
                    })
           }
    )

  private fun createNoAvailableUpdateDialog(project: Project?): DialogWrapper =
    dialog("R Machine Learning completion",
           panel = panel {
             row {
               label("No updates are available")
             }
           },
           project = project,
           createActions = { listOf(ButtonAction("OK", DialogWrapper.OK_EXIT_CODE)) }
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
