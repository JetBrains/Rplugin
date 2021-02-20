package org.jetbrains.r.settings

import com.intellij.CommonBundle
import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
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
    private const val TOP_PANEL_OFFSET = 20
    private const val PORT_FIELD_WIDTH = 5
    private val PORT_RANGE = IntRange(1024, 65353)
    private const val TIMEOUT_FIELD_WIDTH = 4
    private val TIMEOUT_RANGE_MS = IntRange(0, 2000)
    private val settings = MachineLearningCompletionSettings.getInstance()

    private fun updateLastCheckedLabel(label: JLabel, time: Long) {
      val prefix = IdeBundle.message("updates.settings.last.check")
      when {
        time <= 0 -> label.text = prefix + ' ' + IdeBundle.message("updates.last.check.never")
        else -> {
          label.text = prefix + ' ' + DateFormatUtil.formatPrettyDateTime(time)
          label.toolTipText = DateFormatUtil.formatDate(time) + ' ' + DateFormatUtil.formatTimeWithSeconds(time)
        }
      }
    }

    private fun updateVersionLabel(label: JLabel, version: Version?) {
      label.text = version?.toString() ?: RBundle.message("project.settings.ml.completion.version.none")
    }

    private fun Row.enableSubRowsIf(predicate: ComponentPredicate) {
      subRowsEnabled = predicate.invoke()
      predicate.addListener { subRowsEnabled = it }
    }
  }

  private val modality = ModalityState.current()

  private fun invokeLaterWithTabModality(action: () -> Unit) = ApplicationManager.getApplication().invokeLater(action, modality)

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(displayName) {

        row {
          cell {
            label(RBundle.message("project.settings.ml.completion.version.app"))
            versionLabel(MachineLearningCompletionAppArtifact())
            label(RBundle.message("project.settings.ml.completion.version.model")).withLargeLeftGap()
            versionLabel(MachineLearningCompletionModelArtifact())
          }
        }

        row {
          cell {
            checkForUpdatesButton()
            lastCheckedLabel().withLargeLeftGap()
          }
        }.largeGapAfter()

        row {
          val enableCompletionCheckbox = checkBox(RBundle.message("project.settings.ml.completion.checkbox"),
                                                  settings.state::isEnabled)
          enableSubRowsIf(enableCompletionCheckbox.selected)

          row {
            label(RBundle.message("project.settings.ml.completion.server.label"))
          }

          row {
            cell(isFullWidth = true) {
              label(RBundle.message("project.settings.ml.completion.host.label"))
              textField({ settings.state.host ?: "" }, settings.state::host.setter)
              label(RBundle.message("project.settings.ml.completion.port.label"))
              intTextField(settings.state::port, columns = PORT_FIELD_WIDTH, range = PORT_RANGE)
            }
          }

          row {
            cell {
              label(RBundle.message("project.settings.ml.completion.timeout.label"))
              intTextField(settings.state::requestTimeoutMs, columns = TIMEOUT_FIELD_WIDTH, range = TIMEOUT_RANGE_MS)
              label(IdeBundle.message("label.milliseconds"))
            }
          }
        }
      }
    }.withBorder(JBUI.Borders.emptyTop(TOP_PANEL_OFFSET))
  }

  private fun Cell.lastCheckedLabel(): CellBuilder<JLabel> {
    val label = JBLabel()
    label.foreground = JBColor.GRAY

    updateLastCheckedLabel(label, settings.state.lastUpdateCheckTimestampMs)

    disposable?.let {
      MachineLearningCompletionSettingsChangeListener { beforeState, afterState ->
        if (beforeState.lastUpdateCheckTimestampMs != afterState.lastUpdateCheckTimestampMs) {
          invokeLaterWithTabModality {
            updateLastCheckedLabel(label, afterState.lastUpdateCheckTimestampMs)
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
    MachineLearningCompletionDownloadModelService.getInstance()
      .initiateUpdateCycle(isModal = true, reportIgnored = true, { showFailedToCheckForUpdatesDialog() }) { (artifacts, size) ->
        if (artifacts.isNotEmpty()) {
          showUpdateDialog(artifacts, size)
        }
        else {
          showNoAvailableUpdateDialog()
        }
      }
  }.enableIf(object : ComponentPredicate() {
    override fun invoke(): Boolean =
      MachineLearningCompletionUpdateAction.canInitiateUpdateAction.get()

    override fun addListener(listener: (Boolean) -> Unit) {
      this@MachineLearningCompletionConfigurable.disposable?.let { parentDisposable ->
        MachineLearningCompletionUpdateAction.canInitiateUpdateAction
          .afterChange(listener, parentDisposable)
      }
    }
  })

  private fun showUpdateDialog(artifacts: List<MachineLearningCompletionRemoteArtifact>,
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
    ).show()

  private fun showInfoDialog(text: String) =
    dialog(displayName,
           panel = panel {
             row {
               label(text)
             }
           },
           createActions = { listOf(ButtonAction(CommonBundle.getOkButtonText(), DialogWrapper.OK_EXIT_CODE)) }
    ).show()

  private fun showNoAvailableUpdateDialog() =
    showInfoDialog(RBundle.message("project.settings.ml.completion.dialog.noUpdates"))

  private fun showFailedToCheckForUpdatesDialog() =
    showInfoDialog(IdeBundle.message("error.occurred.please.check.your.internet.connection"))

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
