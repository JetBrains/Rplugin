package org.jetbrains.r.settings

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionDownloadModelService
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionRemoteArtifact
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionUpdateAction
import java.awt.Component
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

class MachineLearningCompletionConfigurable : BoundConfigurable(RBundle.message("project.settings.ml.completion.name")) {

  companion object {
    private const val PORT_FIELD_WIDTH = 5
    private const val GET_CURRENT_PROJECT_TIMEOUT_MS = 100
    private val settings = MachineLearningCompletionSettings.getInstance()
  }

  private val MODALITY = ModalityState.current()

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(displayName) {
        row {
          createCheckForUpdatesButton()
        }
        row {
          val enableCompletionCheckbox = checkBox(RBundle.message("project.settings.ml.completion.checkbox"),
                                                  settings.state::isEnabled)
          row {
            label(RBundle.message("project.settings.ml.completion.server.label"))
          }
          row {
            cell {
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

  private fun Cell.createCheckForUpdatesButton() = button("Check for updates") {
    MachineLearningCompletionDownloadModelService.getInstance().initiateUpdateCycle(true, false) { (artifacts, size) ->
      // TODO: search for similar code in the codebase
      val focusedProject = try {
        DataManager.getInstance().dataContextFromFocusAsync.blockingGet(GET_CURRENT_PROJECT_TIMEOUT_MS)
      } catch (e: Exception) {
        null
      }?.getData(PlatformDataKeys.PROJECT)

      if (artifacts.isNotEmpty()) {
        ApplicationManager.getApplication().invokeLater({ createUpdateDialog(focusedProject, artifacts, size).show() }, MODALITY)
      }
      else {
        ApplicationManager.getApplication().invokeLater({ createNoAvailableUpdateDialog(focusedProject).show() }, MODALITY)
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
    UpdateDialogWrapper(project, artifacts, size)

  private class UpdateDialogWrapper(
    private val project: Project?,
    private val artifacts: List<MachineLearningCompletionRemoteArtifact>,
    private val size: Long
  ) : DialogWrapper(project, true) {

    init {
      // TODO: verify with MyDialogWrapper
      init()
      title = "R Machine Learning completion update"
    }

    override fun createDefaultActions() {
      super.createDefaultActions()
      okAction.putValue(Action.NAME, "Update")
    }

    override fun createCenterPanel(): JComponent =
      panel {
        row {
          // TODO: make MB
          label("Update is available $size bytes")
        }
      }

    override fun doOKAction() {
      if (okAction.isEnabled) {
        val updateAction = MachineLearningCompletionUpdateAction(project, artifacts)
        updateAction.performAsync()
        close(OK_EXIT_CODE)
      }
    }

    override fun doCancelAction() {
      if (cancelAction.isEnabled) {
        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
        close(CANCEL_EXIT_CODE)
      }
    }
  }

  private fun createNoAvailableUpdateDialog(project: Project?): DialogWrapper =
    dialog("R Machine Learning completion",
           panel = panel {
             row {
               label("No updates are available")
             }
           },
           project = project,
           createActions = {
             listOf(OkButtonAction())
           }
    )

  private class OkButtonAction : AbstractAction("OK") {
    init {
      putValue(DialogWrapper.DEFAULT_ACTION, true)
    }

    override fun actionPerformed(event: ActionEvent) {
      val wrapper = DialogWrapper.findInstance(event.source as? Component)
      wrapper?.close(DialogWrapper.OK_EXIT_CODE)
    }
  }
}
