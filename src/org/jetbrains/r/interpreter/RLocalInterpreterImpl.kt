/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import java.io.File
import javax.swing.JTextField

class RLocalInterpreterImpl(
  override val interpreterLocation: RLocalInterpreterLocation,
  versionInfo: Map<String, String>, project: Project) : RInterpreterBase(versionInfo, project) {
  val interpreterPath get() = interpreterLocation.path
  override val basePath = project.basePath!!
  override val hostOS get() = OperatingSystem.current()

  override fun getProcessOutput(scriptText: String) = runScript(scriptText, interpreterPath, basePath)

  override fun runHelper(helper: File, workingDirectory: String?, args: List<String>, errorHandler: ((ProcessOutput) -> Unit)?): String {
    return RInterpreterUtil.runHelper(interpreterPath, helper, workingDirectory, args, errorHandler)
  }

  override fun runHelperProcess(script: String, args: List<String>, workingDirectory: String?): ProcessHandler {
    val commands = RInterpreterUtil.getRunHelperCommands(interpreterPath, script, args)
    val commandLine = RInterpreterUtil.createCommandLine(interpreterPath, commands, workingDirectory)
    return OSProcessHandler(commandLine)
  }

  override fun runMultiOutputHelper(helper: File,
                                    workingDirectory: String?,
                                    args: List<String>,
                                    processor: RMultiOutputProcessor) {
    RInterpreterUtil.runMultiOutputHelper(interpreterPath, helper, workingDirectory, args, processor)
  }

  override fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop {
    return RInteropUtil.createRInteropForLocalProcess(this, process, port)
  }

  override fun uploadHelperToHost(helper: File): String {
    return helper.absolutePath
  }

  override fun runProcessOnHost(command: GeneralCommandLine): ColoredProcessHandler {
    return ColoredProcessHandler(command.withWorkDirectory(basePath)).apply {
      setShouldDestroyProcessRecursively(true)
    }
  }

  override fun uploadFileToHostIfNeeded(file: VirtualFile, preserveName: Boolean): String {
    return file.path
  }

  override fun createFileChooserForHost(value: String, selectFolder: Boolean): TextFieldWithBrowseButton {
    return TextFieldWithBrowseButton().also { component ->
      component.text = value
      val descriptor = if (selectFolder) {
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
      } else {
        FileChooserDescriptorFactory.createSingleFileDescriptor()
      }
      component.addActionListener(
        ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
          null, null, component, project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT))
      FileChooserFactory.getInstance().installFileCompletion(component.textField, descriptor, true, null)
    }
  }

  override fun createTempFileOnHost(name: String, content: ByteArray?): String {
    val i = name.indexOfLast { it == '.' }
    val file = if (i == -1) {
      FileUtilRt.createTempFile(name, null, true)
    } else {
      FileUtilRt.createTempFile(name.substring(0, i), name.substring(i), true)
    }
    content?.let { file.writeBytes(it) }
    return file.path
  }

  override fun createTempDirOnHost(name: String): String = FileUtilRt.createTempDirectory(name, null, true).path

  override fun getGuaranteedWritableLibraryPath(libraryPaths: List<RInterpreter.LibraryPath>, userPath: String): Pair<String, Boolean> {
    val writable = libraryPaths.find { it.isWritable }
    return if (writable != null) {
      Pair(writable.path, false)
    } else {
      Pair(userPath, File(userPath).mkdirs())
    }
  }

  companion object {
    val LOG = Logger.getInstance(RLocalInterpreterImpl::class.java)

    // TODO: run via helper
    fun loadInterpreterVersionInfo(interpreterPath: String, workingDirectory: String): Map<String, String> {
      return runScript("version", interpreterPath, workingDirectory)?.stdoutLines?.map { it.split(' ', limit = 2) }
               ?.filter { it.size == 2 }?.map { it[0] to it[1].trim() }?.toMap() ?: emptyMap()
    }

    private fun runScript(scriptText: String, interpreterPath: String, workingDirectory: String): ProcessOutput? {
      val commandLine = arrayOf<String>(interpreterPath, "--no-restore", "--quiet", "--slave", "-e", scriptText)
      try {
        val processHandler = CapturingProcessHandler(GeneralCommandLine(*commandLine).withWorkDirectory(workingDirectory))
        return processHandler.runProcess(RInterpreterUtil.DEFAULT_TIMEOUT)
      } catch (e: Throwable) {
        LOG.info("Failed to run R executable: \n" +
                 "Interpreter path " + interpreterPath + "\n" +
                 "Exception occurred: " + e.message)
      }
      return null
    }
  }
}
