package org.jetbrains.r.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionDownloadModelService
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionRemoteArtifact
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionUpdateAction
import java.awt.Component
import java.awt.event.ActionEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.AbstractAction
import javax.swing.JLabel
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty


class MachineLearningCompletionConfigurable : BoundConfigurable(RBundle.message("project.settings.ml.completion.name")) {

  companion object {
    private const val PORT_FIELD_WIDTH = 5
    private val settings = MachineLearningCompletionSettings.getInstance()
    private val applicationManager = ApplicationManager.getApplication()
  }

  private val observableDelegate = object : ObservableProperty<String>("Updating") {
    // TODO: Maybe should be replaced by a map of <JLabel, Modality>
    private val listeners = Collections.newSetFromMap(ConcurrentHashMap<JLabel, Boolean>())

    override fun afterChange(property: KProperty<*>, oldValue: String, newValue: String) =
      listeners.forEach {
        applicationManager.invokeLater { it.text = newValue }
      }

    // TODO: Maybe add a little bit more of an abstraction by passing functions as listeners
    // TODO: (oldValue, newValue) -> Unit
    fun subscribe(label: JLabel) = listeners.add(label)

    fun unsubscribe(label: JLabel) = listeners.remove(label)
  }

  private val MODALITY = ModalityState.current()

  private var infoString by observableDelegate
  private val infoLabel = JBLabel("").also(observableDelegate::subscribe)

  override fun disposeUIResources() {
    super.disposeUIResources()
    observableDelegate.unsubscribe(infoLabel)
  }

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(displayName) {
        row {
          createCheckForUpdatesButton()
          component(infoLabel)
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
      if (artifacts.isNotEmpty()) {
        val pressedUpdate = createUpdateDialog(null, artifacts, size).showAndGet()
        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(pressedUpdate)
      }
      else {
        createNoAvailableUpdateDialog(null).show()
        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
      }
      // TODO: Update lastChecked datetime somewhere
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
