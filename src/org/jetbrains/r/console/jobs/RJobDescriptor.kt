/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.*
import javax.swing.JComponent

interface RJobDescriptor {
  val project: Project
  val outputComponent: JComponent
  val processTerminated: Boolean
  val processFailed: Boolean
  val startedAt: Date
  val duration: Long
  val scriptFile: VirtualFile

  fun onProgressChanged(lambda: (current: Int, total: Int) -> Unit)
  fun onProcessTerminated(lambda: () -> Unit)
  fun destroyProcess()
  fun rerun()
}

class RJobDescriptorImpl(
  override val project: Project,
  private val task: RJobTask,
  private val progressProvider: RJobProgressProvider,
  private val processHandler: ProcessHandler,
  private val consoleView: ConsoleView
  ): RJobDescriptor {

  @Volatile
  private var progressChanged: ((current: Int, total: Int) -> Unit)? = null

  init {
    progressProvider.progressUpdated = {
      progressChanged?.invoke(progressProvider.current, progressProvider.total)
    }
  }

  override val startedAt: Date = Date()
  private val startedAtInMillis: Long = System.currentTimeMillis()
  override val scriptFile = task.script

  override val outputComponent: JComponent
    get() = consoleView.component
  override val processTerminated: Boolean
    get() = processHandler.isProcessTerminated
  override val processFailed: Boolean
    get() = processHandler.exitCode != null && processHandler.exitCode != 0
  override val duration: Long
    get() = System.currentTimeMillis() - startedAtInMillis

  override fun onProgressChanged(lambda: (current: Int, total: Int) -> Unit) {
    progressChanged = lambda
  }

  override fun onProcessTerminated(lambda: () -> Unit) {
    processHandler.addProcessListener(object : ProcessAdapter() {
      override fun processTerminated(event: ProcessEvent) {
        lambda()
      }
    })
  }

  override fun destroyProcess() {
    processHandler.destroyProcess()
  }

  override fun rerun() {
    RJobRunner.getInstance(project).runRJob(task)
  }
}