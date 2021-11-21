package org.jetbrains.r.settings

import com.intellij.CommonBundle
import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Version
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.dialog
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.*
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.update.*
import org.jetbrains.r.editor.mlcompletion.update.MachineLearningCompletionNotifications.showUpdateDialog
import java.awt.Component
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
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
      val lastTimeChecked = when {
        time <= 0 -> IdeBundle.message("updates.last.check.never")
        else -> DateFormatUtil.formatPrettyDateTime(time)
      }
      label.text = IdeBundle.message("updates.settings.last.check", lastTimeChecked)
      if (time > 0) {
        label.toolTipText = DateFormatUtil.formatDate(time) + ' ' + DateFormatUtil.formatTimeWithSeconds(time)
      }
    }

    private fun updateVersionLabel(label: JLabel, version: Version?) {
      label.text = version?.toString() ?: RBundle.message("project.settings.ml.completion.version.none")
    }
  }

  private val modality = ModalityState.current()

  private fun invokeLaterWithTabModality(action: () -> Unit): Unit = ApplicationManager.getApplication().invokeLater(action, modality)

  override fun createPanel(): DialogPanel {
    return panel {
      group(displayName) {

        row(RBundle.message("project.settings.ml.completion.version.app")) {
          versionLabel(MachineLearningCompletionLocalArtifact.Application)
          versionLabel(MachineLearningCompletionLocalArtifact.Model)
            .label(RBundle.message("project.settings.ml.completion.version.model"))
        }

        row {
          checkForUpdatesButton()
          lastCheckedLabel()
        }.bottomGap(BottomGap.SMALL)

        lateinit var enableCompletionCheckbox: Cell<JBCheckBox>
        row {
          enableCompletionCheckbox = checkBox(RBundle.message("project.settings.ml.completion.checkbox"))
            .bindSelected(settings.state::isEnabled)
        }

        indent {
          panel {
            row {
              label(RBundle.message("project.settings.ml.completion.server.label"))
            }

            row(RBundle.message("project.settings.ml.completion.host.label")) {
              textField()
                .bindText({ settings.state.host ?: "" }, {})
                .resizableColumn()
                .horizontalAlign(HorizontalAlign.FILL)
                .enabled(false)
              intTextField(range = PORT_RANGE)
                .bindIntText(settings.state::port)
                .columns(PORT_FIELD_WIDTH)
                .label(RBundle.message("project.settings.ml.completion.port.label"))
            }.layout(RowLayout.INDEPENDENT)

            row(RBundle.message("project.settings.ml.completion.timeout.label")) {
              intTextField(range = TIMEOUT_RANGE_MS)
                .bindIntText(settings.state::requestTimeoutMs)
                .columns(TIMEOUT_FIELD_WIDTH)
                .gap(RightGap.SMALL)
              label(IdeBundle.message("label.milliseconds"))
            }
          }.enabledIf(enableCompletionCheckbox.selected)
        }
      }
    }.withBorder(JBUI.Borders.emptyTop(TOP_PANEL_OFFSET))
  }

  private fun Row.lastCheckedLabel(): Cell<JLabel> {
    val result = label("")
      .applyToComponent {
        foreground = JBColor.GRAY
      }

    updateLastCheckedLabel(result.component, settings.state.lastUpdateCheckTimestampMs)

    disposable?.let {
      MachineLearningCompletionSettingsChangeListener { beforeState, afterState ->
        if (beforeState.lastUpdateCheckTimestampMs != afterState.lastUpdateCheckTimestampMs) {
          invokeLaterWithTabModality {
            updateLastCheckedLabel(result.component, afterState.lastUpdateCheckTimestampMs)
          }
        }
      }.subscribe(it)
    }

    return result
  }

  private fun Row.versionLabel(artifact: MachineLearningCompletionLocalArtifact): Cell<JLabel> {
    val result = label("")
    updateVersionLabel(result.component, artifact.currentVersion)

    disposable?.let { disposable ->
      MachineLearningCompletionModelFilesService.getInstance().registerVersionChangeListener(artifact, disposable) {
        invokeLaterWithTabModality { updateVersionLabel(result.component, it) }
      }
    }

    return result
  }

  override fun apply() {
    val beforeState = settings.copyState()
    super.apply()
    MachineLearningCompletionSettingsChangeListener.notifySettingsChanged(beforeState, settings.state)
  }

  private fun Row.checkForUpdatesButton(): Cell<JButton> = button(
    RBundle.message("project.settings.ml.completion.button.checkForUpdates")) {
    MachineLearningCompletionDownloadModelService.getInstance()
      .initiateUpdateCycle(isModal = true, reportIgnored = true, { showFailedToCheckForUpdatesDialog() }) { (artifacts, size) ->
        if (artifacts.isNotEmpty()) {
          showUpdateDialog(artifacts, size)
        }
        else {
          showNoAvailableUpdateDialog()
        }
      }
  }.enabledIf(object : ComponentPredicate() {
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
                               size: Long): Unit =
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
    ).showUpdateDialog()

  private fun showInfoDialog(text: String): Unit =
    dialog(displayName,
           panel = panel {
             row {
               label(text)
             }
           },
           createActions = { listOf(ButtonAction(CommonBundle.getOkButtonText(), DialogWrapper.OK_EXIT_CODE)) }
    ).show()

  private fun showNoAvailableUpdateDialog(): Unit =
    showInfoDialog(RBundle.message("project.settings.ml.completion.dialog.noUpdates"))

  private fun showFailedToCheckForUpdatesDialog(): Unit =
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
