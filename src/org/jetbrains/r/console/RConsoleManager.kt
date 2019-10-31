/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.UIUtil.findComponentOfType
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.settings.RSettings
import java.lang.ref.WeakReference

class RConsoleManager(private val project: Project) {
  @Volatile
  private var firstConsolePromise: Promise<RConsoleView>? = null

  fun runIfEmpty() {
    currentConsoleOrNull ?: runSingleConsole()
  }

  val currentConsoleAsync: Promise<RConsoleView>
    get() = AsyncPromise<RConsoleView>().apply {
      currentConsoleOrNull?.let { setResult(it) } ?: runSingleConsole().processed(this)
    }

  val currentConsoleOrNull: RConsoleView?
    get() {
      val toolWindow = RConsoleToolWindowFactory.getToolWindow(project) ?: return null
      if (findComponentOfType(toolWindow.component, RConsoleView::class.java) == null) return null
      return toolWindow?.contentManager?.let { cm ->
        with(cm) {
          findComponentOfType(selectedContent?.component, RConsoleView::class.java) ?: contents.asSequence().map {
            findComponentOfType(it.component, RConsoleView::class.java)
          }.firstOrNull()
        }
      }
    }


  private val consoles: List<RConsoleView>
    get() = getContentDescription(project)?.contentConsolePairs?.map { it.second }?.toList() ?: listOf()


  private fun runSingleConsole(): Promise<RConsoleView> {
    firstConsolePromise?.let { return it }
    synchronized(this) {
      firstConsolePromise?.let { return it }
      return runConsole(project).apply { firstConsolePromise = this }.onSuccess { firstConsolePromise = null }
    }
  }

  companion object {
    private data class ContentDescription(
      val contentManager: ContentManager,
      val contentConsolePairs: Sequence<Pair<Content, RConsoleView>>
    )

    fun getInstance(project: Project): RConsoleManager {
      return ServiceManager.getService(project, RConsoleManager::class.java)
    }

    /**
     * Success promise means that [currentConsoleOrNull] is not null
     */
    fun runConsole(project: Project, requestFocus: Boolean = false): Promise<RConsoleView> {
      val promise = AsyncPromise<RConsoleView>()
      if (!RInterpreterManager.getInstance(project).hasInterpreter()) {
        RInterpreterManager.getInstance(project).initializeInterpreter().onSuccess { doRunConsole(project, requestFocus).processed(promise) }
      }
      else {
        doRunConsole(project, requestFocus).processed(promise)
      }
      return promise
    }

    /**
     * Close all consoles that has path to interpreter different than [interpreterPath]
     */
    fun closeMismatchingConsoles(project: Project, interpreterPath: String) {
      getContentDescription(project)?.let { description ->
        for ((content, console) in description.contentConsolePairs) {
          if (console.interpreterPath != interpreterPath) {
            description.contentManager.removeContent(content, true)
          }
        }
      }
    }

    private fun doRunConsole(project: Project, requestFocus: Boolean): Promise<RConsoleView> {
      return if (RSettings.getInstance(project).interpreterPath.isNotBlank()) {
        RConsoleRunner(project, project.basePath!!).initAndRun().onSuccess { console ->
          if (requestFocus) {
            RConsoleToolWindowFactory.getToolWindow(project)?.show {

              val focusManager = IdeFocusManager.findInstanceByComponent(console)
              focusManager.requestFocusInProject(focusManager.getFocusTargetFor(console.component) ?: return@show, project)
            }
          }
        }
      } else {
        AsyncPromise<RConsoleView>().apply {
          setError("Cannot run console until path to viable R interpreter is specified")
        }
      }
    }

    private fun getContentDescription(project: Project): ContentDescription? {
      return RConsoleToolWindowFactory.getToolWindow(project)?.contentManager?.let { cm ->
        val pairs = cm.contents.asSequence()
          .mapNotNull {
            findComponentOfType(it.component, RConsoleView::class.java)?.let { console ->
              Pair(it, console)
            }
          }
        ContentDescription(cm, pairs)
      }
    }
  }
}