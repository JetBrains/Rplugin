/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ssh.*
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.ui.unified.SshConfigComboBox
import com.intellij.ssh.ui.unified.SshConfigConfigurable
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.plugins.remotesdk.ui.RemoteBrowseActionListener
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.RBasicInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.getVersion
import org.jetbrains.r.remote.host.RRemoteHostManager
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

object RAddOrEditRemoteInterpreterDialog {
  fun show(existingInterpreters: List<RInterpreterInfo>, initialInfo: RInterpreterInfo? = null,
           onDone: (RInterpreterInfo) -> Unit) {
    val initialLocation = initialInfo?.interpreterLocation as? RRemoteInterpreterLocation
    val dialogTitle = if (initialLocation == null) {
      RRemoteBundle.message("project.settings.add.remote.dialog.title")
    } else {
      RRemoteBundle.message("project.settings.edit.remote.dialog.title")
    }
    var name = initialInfo?.interpreterName ?: RRemoteBundle.message("project.settings.remote.name")
    var sshConfig: SshConfig? = initialLocation?.remoteHost?.sshConfig
    var interpreterPath = initialLocation?.remotePath.orEmpty()
    var basePath = initialLocation?.basePath.orEmpty()
    var previousConfig: SshConfig? = sshConfig
    while (true) {
      val dialog = object : DialogWrapper(null) {
        val nameField = JBTextField(name).also {
          addUpdateDocumentListener(it)
        }
        val sshConfigComboBox = SshConfigComboBox(null, disposable, SshConfigConfigurable.Visibility.App).also {
          it.reload()
          it.select(sshConfig)
        }
        val pathField = TextFieldWithBrowseButton().also {
          it.text = interpreterPath
          initTextField(it, RBundle.message("project.settings.select.interpreter"))
        }
        val basePathField = TextFieldWithBrowseButton().also {
          it.text = basePath
          initTextField(it, RRemoteBundle.message("add.remote.dialog.select.base.path"))
        }

        private val panel = panel {
          row(RRemoteBundle.message("add.remote.dialog.interpreter.name.label")) { nameField() }
          row(RRemoteBundle.message("add.remote.dialog.remote.host.label")) { sshConfigComboBox() }
          row(RRemoteBundle.message("add.remote.dialog.interpreter.path.label")) { pathField() }
          row(RRemoteBundle.message("add.remote.dialog.base.path.label")) { basePathField() }
        }

        init {
          title = dialogTitle
          init()
          sshConfigComboBox.setDataListener(this::onUpdate)
          onUpdate()
        }

        override fun createCenterPanel() = panel

        private fun addUpdateDocumentListener(component: JTextComponent) {
          component.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = onUpdate()
            override fun insertUpdate(e: DocumentEvent?) = onUpdate()
            override fun removeUpdate(e: DocumentEvent?) = onUpdate()
          })
        }

        private fun initTextField(field: TextFieldWithBrowseButton, dialogTitle: String) {
          field.addActionListener(RemoteBrowseActionListener(field.textField, dialogTitle) { consumer ->
            val config = sshConfigComboBox.selectedSshConfig ?: return@RemoteBrowseActionListener
            runAsync { consumer.consume(config.copyToCredentials()) }
          })
          addUpdateDocumentListener(field.textField)
          field.isEnabled = false
        }

        private fun onUpdate() {
          val config = sshConfigComboBox.selectedSshConfig
          if (config != null && previousConfig != config) {
            previousConfig = config
            guessPaths(config)
          }
          pathField.isEnabled = config != null
          basePathField.isEnabled = config != null
          isOKActionEnabled = config != null && nameField.text.isNotBlank() && pathField.text.isNotBlank() && basePathField.text.isNotBlank()
        }

        private fun guessPaths(config: SshConfig) {
          pathField.text = ""
          basePathField.text = ""
          runAsync {
            try {
              val host = RRemoteHostManager.getInstance().getRemoteHostBySshConfig(config)
              val output = host.runCommand(GeneralCommandLine("which", "R"), WHICH_COMMAND_TIMEOUT_IN_MILLIS)
              if (output.exitCode != 0) return@runAsync
              val rPath = output.stdout.trim()
              if (rPath.isNotEmpty()) {
                SwingUtilities.invokeLater {
                  pathField.text = rPath
                  interpreterPath = rPath
                }
              }
              val homePath = host.useSftpChannel { it.home }.trim()
              if (homePath.isNotEmpty()) {
                SwingUtilities.invokeLater {
                  basePathField.text = homePath
                  basePath = homePath
                }
              }
            } catch (e: Exception) {
              RRemoteInterpreterImpl.LOG.warn(e)
            }
          }
        }
      }
      dialog.show()
      if (!dialog.isOK) return

      sshConfig = dialog.sshConfigComboBox.selectedSshConfig ?: continue
      interpreterPath = dialog.pathField.text
      basePath = dialog.basePathField.text
      val remoteHost = RRemoteHostManager.getInstance().getRemoteHostBySshConfig(sshConfig)
      val location = RRemoteInterpreterLocation(remoteHost, interpreterPath, basePath)
      if (existingInterpreters.any { it.interpreterLocation == location }) {
        Messages.showErrorDialog(RBundle.message("project.settings.interpreter.duplicate.description"),
                                 RBundle.message("project.settings.interpreter.duplicate.title"))
        continue
      }
      try {
        ExecuteExpressionUtils.getSynchronously(RRemoteBundle.message("add.remote.dialog.check.remote.host")) {
          remoteHost.ensureHasCredentials()
        }
      } catch (e: SftpChannelException) {
        Messages.showErrorDialog(e.cause?.message ?: e.message, RRemoteBundle.message("add.remote.dialog.connection.failed"))
        continue
      } catch (e: SshTransportException) {
        Messages.showErrorDialog(e.cause?.message ?: e.message, RRemoteBundle.message("add.remote.dialog.connection.failed"))
        continue
      }

      val errorMessage = ExecuteExpressionUtils.getSynchronously(RRemoteBundle.message("add.remote.dialog.check.working.directory")) {
        RRemoteInterpreterImpl.checkBasePath(basePath, remoteHost)
      }
      if (errorMessage != null) {
        Messages.showErrorDialog(errorMessage, RRemoteBundle.message("add.remote.dialog.invalid.working.directory"))
        continue
      }
      val version = try {
        ExecuteExpressionUtils.getSynchronously(RBundle.message("project.settings.check.interpreter")) {
          location.getVersion()
        }
      }
      catch (e: Exception) {
        val details = e.message?.let { m -> ":\n$m" } ?: ""
        Messages.showErrorDialog("${RBundle.message("project.settings.invalid.interpreter.description")}$details",
                                 RBundle.message("project.settings.invalid.interpreter"))
        continue
      }
      if (version == null || !RInterpreterUtil.isSupportedVersion(version)) {
        val details = version?.let { m -> ":\nUnsupported version $m" } ?: ""
        Messages.showErrorDialog("${RBundle.message("project.settings.invalid.interpreter.description")}$details",
                                 RBundle.message("project.settings.invalid.interpreter"))
        continue
      }

      name = dialog.nameField.text.trim()
      val interpreter = RBasicInterpreterInfo(name, location, version)
      onDone(interpreter)
      break
    }
  }

  private const val WHICH_COMMAND_TIMEOUT_IN_MILLIS = 3000L
}
