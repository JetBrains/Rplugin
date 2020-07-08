/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ssh.ui.unified.SshConfigComboBox
import com.intellij.ssh.ui.unified.SshConfigConfigurable
import com.jetbrains.plugins.remotesdk.ui.RemoteBrowseActionListener
import org.jetbrains.r.RBundle
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.RBasicInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.getVersion
import org.jetbrains.r.remote.host.RRemoteHostManager
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

object RAddRemoteInterpreterDialog {
  fun show(existingInterpreters: List<RInterpreterInfo>, onAdded: (RInterpreterInfo) -> Unit) {
    val dialog = object : DialogWrapper(null) {
      val panel = JPanel(GridBagLayout())
      val sshConfigLabel = JLabel(RRemoteBundle.message("add.remote.dialog.remote.host.label")).also {
        panel.add(it, GridBagConstraints().apply {
          gridx = 0
          gridy = 0
          weightx = 0.0
          weighty = 1.0
          anchor = GridBagConstraints.WEST
        })
      }
      val sshConfigComboBox = SshConfigComboBox(null, disposable, SshConfigConfigurable.Visibility.App).also {
        panel.add(it, GridBagConstraints().apply {
          gridx = 1
          gridy = 0
          weightx = 1.0
          weighty = 1.0
          fill = GridBagConstraints.HORIZONTAL
        })
        it.reload()
      }
      val pathLabel = JLabel(RRemoteBundle.message("add.remote.dialog.interpreter.path.label")).also {
        panel.add(it, GridBagConstraints().apply {
          gridx = 0
          gridy = 1
          weightx = 0.0
          weighty = 1.0
          anchor = GridBagConstraints.WEST
        })
      }
      val pathField = TextFieldWithBrowseButton().also {
        panel.add(it, GridBagConstraints().apply {
          gridx = 1
          gridy = 1
          weightx = 1.0
          weighty = 1.0
          fill = GridBagConstraints.HORIZONTAL
        })
        it.addActionListener(RemoteBrowseActionListener(it.textField, RBundle.message("project.settings.select.interpreter")) { consumer ->
          val config = sshConfigComboBox.selectedSshConfig ?: return@RemoteBrowseActionListener
          consumer.consume(config.copyToCredentials())
        })
        it.textField.document.addDocumentListener(object : DocumentListener {
          fun onUpdate() {
            val config = sshConfigComboBox.selectedSshConfig
            isOKActionEnabled = config != null && it.text.isNotEmpty()
          }

          override fun changedUpdate(e: DocumentEvent?) = onUpdate()
          override fun insertUpdate(e: DocumentEvent?) = onUpdate()
          override fun removeUpdate(e: DocumentEvent?) = onUpdate()

        })
        it.isEnabled = false
      }

      init {
        title = RRemoteBundle.message("project.settings.add.remote.dialog.title")
        init()
        val listener = listener@{
          val config = sshConfigComboBox.selectedSshConfig
          pathField.isEnabled = config != null
          isOKActionEnabled = config != null && pathField.text.isNotEmpty()
        }
        sshConfigComboBox.setDataListener(listener)
        listener()
      }

      override fun createCenterPanel() = panel
    }
    dialog.show()
    if (!dialog.isOK) return

    val sshConfig = dialog.sshConfigComboBox.selectedSshConfig ?: return
    val interpreterPath = dialog.pathField.text
    val location = RRemoteInterpreterLocation(RRemoteHostManager.getInstance().getRemoteHostBySshConfig(sshConfig), interpreterPath)
    if (existingInterpreters.any { it.interpreterLocation == location }) {
      Messages.showErrorDialog(RBundle.message("project.settings.interpreter.duplicate.description"),
                               RBundle.message("project.settings.interpreter.duplicate.title"))
      return
    }
    val version = try {
      ExecuteExpressionUtils.getSynchronously(RBundle.message("project.settings.check.interpreter")) {
        location.getVersion()
      }
    } catch (e: Exception) {
      val details = e.message?.let { m -> ":\n$m" } ?: ""
      Messages.showErrorDialog("${RBundle.message("project.settings.invalid.interpreter.description")}$details",
                               RBundle.message("project.settings.invalid.interpreter"))
      return
    }
    if (version == null || !RInterpreterUtil.isSupportedVersion(version)) {
      val details = version?.let { m -> ":\nUnsupported version $m" } ?: ""
      Messages.showErrorDialog("${RBundle.message("project.settings.invalid.interpreter.description")}$details",
                               RBundle.message("project.settings.invalid.interpreter"))
      return
    }

    val interpreter = RBasicInterpreterInfo(RRemoteBundle.message("project.settings.remote.name"), location, version)
    onAdded(interpreter)
  }
}
