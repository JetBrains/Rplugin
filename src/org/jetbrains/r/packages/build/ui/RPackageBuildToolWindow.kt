/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.build.ui

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import org.jetbrains.concurrency.*
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.packages.RHelpersUtil
import org.jetbrains.r.packages.build.RPackageBuildUtil
import org.jetbrains.r.packages.remote.RPackageManagementService
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.settings.RPackageBuildSettings
import org.intellij.datavis.r.ui.ToolbarUtil
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

class RPackageBuildToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {
  private val consoleView = ConsoleViewImpl(project, false).apply {
    Disposer.register(project, this)
  }

  private val rootPanel = JPanel(BorderLayout()).apply {
    add(consoleView.component, BorderLayout.CENTER)
  }

  private val service = RPackageManagementService(project)
  private val settings = RPackageBuildSettings.getInstance(project)
  private val packageName = RPackageBuildUtil.getPackageName(project)
  private val manager = RPackageBuildTaskManager(project, this::onReset, this::updateExportsAsync, this::onInterrupted)

  @Volatile
  private var currentProcessHandler: CapturingProcessHandler? = null

  @Volatile
  private var isInterrupted: Boolean = false

  init {
    setContent(rootPanel)
    toolbar = createToolbar()
  }

  private fun createToolbar(): JComponent {
    val primaryHolders = listOf(
      manager.createActionHolder(INSTALL_ACTION_ID, this::installAndReloadPackageAsync, requiredDevTools = false),
      manager.createActionHolder(CHECK_ACTION_ID, this::checkPackageAsync, requiredDevTools = false),
      manager.createActionHolder(TEST_ACTION_ID, this::testPackageAsync, requiredDevTools = true) {
        RPackageBuildUtil.usesTestThat(project)
      },
      manager.createActionHolder(SETUP_TESTS_ACTION_ID, this::setupTestThatAsync, requiredDevTools = true) {
        !RPackageBuildUtil.usesTestThat(project)
      }
    )
    val secondaryHolders = listOf(
      ToolbarUtil.createActionHolder(SETTINGS_ACTION_ID, this::showSettingsDialog)
    )
    return ToolbarUtil.createToolbar(RToolWindowFactory.BUILD, listOf(primaryHolders, secondaryHolders))
  }

  private fun showSettingsDialog() {
    RPackageBuildSettingsDialog(project).show()
  }

  private fun updateExportsAsync(): Promise<Unit> {
    return if (RPackageBuildUtil.usesRcpp(project)) runHelperAsync(UPDATE_EXPORTS_HELPER) else resolvedPromise()
  }

  private fun installAndReloadPackageAsync(hasDevTools: Boolean): Promise<Unit> {
    return if (packageName != null) {
      if (service.isPackageLoaded(packageName)) {
        service.unloadPackage(packageName, true)
      } else {
        resolvedPromise<Unit>()
      }
        .thenAsync { installPackageAsync(hasDevTools) }
        .thenAsync { service.loadPackage(packageName) }
    } else {
      resolvedPromise()
    }
  }

  private fun installPackageAsync(hasDevTools: Boolean): Promise<Unit> {
    val args = getInstallArguments()
    val useDevTools = hasDevTools && settings.useDevTools
    return if (useDevTools) runHelperAsync(INSTALL_PACKAGE_HELPER, args) else runCommandAsync("INSTALL", args)
  }

  private fun getInstallArguments(): List<String> {
    return mutableListOf<String>().also { args ->
      if (settings.cleanBuild) {
        args.add("--preclean")
        args.add("--clean")
      }
      if (settings.mainArchitectureOnly) {
        args.add("--no-multiarch")
      }
      if (settings.keepSources) {
        args.add("--with-keep.source")
      }
      args.addAll(settings.installArgs)
    }
  }

  private fun checkPackageAsync(hasDevTools: Boolean): Promise<Unit> {
    val args = getCheckArguments()
    val useDevTools = hasDevTools && settings.useDevTools
    return if (useDevTools) runHelperAsync(CHECK_PACKAGE_HELPER, args) else runCommandAsync("check", args)
  }

  private fun getCheckArguments(): List<String> {
    return mutableListOf<String>().also { args ->
      if (settings.asCran) {
        args.add("--as-cran")
      }
      args.addAll(settings.checkArgs)
    }
  }

  private fun testPackageAsync(hasDevTools: Boolean): Promise<Unit> {
    return if (hasDevTools) runHelperAsync(TEST_PACKAGE_HELPER) else resolvedPromise()
  }

  private fun setupTestThatAsync(hasDevTools: Boolean): Promise<Unit> {
    return if (hasDevTools) runHelperAsync(SETUP_TESTS_HELPER) else resolvedPromise()
  }

  private fun runCommandAsync(command: String, args: List<String> = emptyList()): Promise<Unit> {
    return runProcessAsync { interpreterPath ->
      val commands = mutableListOf(interpreterPath, "CMD", command, ".").apply {
        addAll(args)
      }
      RInterpreterUtil.createProcessHandler(interpreterPath, commands, project.basePath)
    }
  }

  private fun runHelperAsync(helper: File, args: List<String> = emptyList()): Promise<Unit> {
    return runProcessAsync { interpreterPath ->
      RInterpreterUtil.createProcessHandlerForHelper(interpreterPath, helper, project.basePath, args)
    }
  }

  private fun runProcessAsync(processHandlerSupplier: (String) -> CapturingProcessHandler): Promise<Unit> {
    return AsyncPromise<Unit>().also { promise ->
      val interpreterPath = RInterpreterManager.getInstance(project).interpreterPath
      val processHandler = processHandlerSupplier(interpreterPath)
      currentProcessHandler = processHandler
      processHandler.addProcessListener(createProcessListener(promise))
      runInEdt {
        consoleView.attachToProcess(processHandler)
        consoleView.scrollToEnd()
        processHandler.startNotify()
      }
    }
  }

  private fun createProcessListener(promise: AsyncPromise<Unit>) = object : ProcessAdapter() {
    override fun processTerminated(event: ProcessEvent) {
      if (!isInterrupted) {
        if (event.exitCode == 0) {
          promise.setResult(Unit)
        } else {
          promise.setError("Process terminated with non-zero code ${event.exitCode}")
        }
      } else {
        isInterrupted = false
        promise.setError("Process was interrupted")
      }
    }
  }

  private fun onInterrupted() {
    isInterrupted = true
    terminateLastProcess()
    consoleView.print("\nInterrupted\n", ConsoleViewContentType.ERROR_OUTPUT)
  }

  private fun onReset() {
    terminateLastProcess()
    consoleView.clear()
  }

  private fun terminateLastProcess() {
    currentProcessHandler?.process?.destroy()
    currentProcessHandler = null
  }

  companion object {
    // Not to be moved to RBundle
    private const val INSTALL_ACTION_ID = "org.jetbrains.r.packages.build.ui.RInstallPackageAction"
    private const val CHECK_ACTION_ID = "org.jetbrains.r.packages.build.ui.RCheckPackageAction"
    private const val TEST_ACTION_ID = "org.jetbrains.r.packages.build.ui.RTestPackageAction"
    private const val SETUP_TESTS_ACTION_ID = "org.jetbrains.r.packages.build.ui.RSetupTestsAction"
    private const val SETTINGS_ACTION_ID = "org.jetbrains.r.packages.build.ui.RPackageBuildSettingsAction"

    private val UPDATE_EXPORTS_HELPER = RHelpersUtil.findFileInRHelpers("R/packages/update_rcpp_exports.R")
    private val INSTALL_PACKAGE_HELPER = RHelpersUtil.findFileInRHelpers("R/packages/install_package.R")
    private val CHECK_PACKAGE_HELPER = RHelpersUtil.findFileInRHelpers("R/packages/check_package.R")
    private val TEST_PACKAGE_HELPER = RHelpersUtil.findFileInRHelpers("R/packages/test_package.R")
    private val SETUP_TESTS_HELPER = RHelpersUtil.findFileInRHelpers("R/packages/setup_tests.R")
  }
}
