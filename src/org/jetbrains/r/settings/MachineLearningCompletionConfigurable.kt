package org.jetbrains.r.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionDownloadModelService
import javax.swing.JPanel

class MachineLearningCompletionConfigurable : BoundConfigurable(RBundle.message("project.settings.ml.completion.name")) {

  companion object {
    private const val PORT_FIELD_WIDTH = 5
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

  private fun Cell.createCheckForUpdatesButton() = button("Check for update") {
    // TODO: remove duplicating code with ...ProjectOpenListener
    val modelDownloaderService = MachineLearningCompletionDownloadModelService.getInstance()
    modelDownloaderService.getArtifactsToDownloadDescriptorsAsync { artifactsToUpdate ->
      val size = modelDownloaderService.getArtifactsSize(artifactsToUpdate)
      if (artifactsToUpdate.isNotEmpty()) {
        ApplicationManager.getApplication().invokeLater( { dialog("Update title", createCheckUpdatesDialog()).show() }, MODALITY)
      }
      else {
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

  private fun createCheckUpdatesDialog(): JPanel = panel {
    row {
      label("Hello")
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
}
