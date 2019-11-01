/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.esotericsoftware.minlog.Log
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.ui.UIUtil.findComponentOfType
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.settings.RSettings
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RConsoleManager(private val project: Project) {
  @Volatile
  private var currentConsole: RConsoleView? = null
  private val consoleCounter: AtomicInteger = AtomicInteger()
  private var consolePromise: Promise<RConsoleView>? = null
  private val isRunningConsole: AtomicBoolean = AtomicBoolean()
  @Volatile
  var initialized = false
    private set

  val currentConsoleAsync: Promise<RConsoleView>
    get() = AsyncPromise<RConsoleView>().apply {
      currentConsole?.let { setResult(it) } ?: runConsole().processed(this)

    }

  @Synchronized
  private fun runConsole(): Promise<RConsoleView> {
    consolePromise?.let { return it }
    return runSingleConsole().onProcessed {
      synchronized(this) {
        consolePromise = null
      }
    }.also {
      consolePromise = it
    }
  }

  val currentConsoleOrNull: RConsoleView?
    get() = currentConsole

  private val consoles: List<RConsoleView>
    get() = getContentDescription(project)?.contentConsolePairs?.map { it.second }?.toList() ?: listOf()


  private fun runSingleConsole(): Promise<RConsoleView> {
    if (RConsoleToolWindowFactory.getRConsoleToolWindows(project) == null) {
      return AsyncPromise<RConsoleView>().apply {
        val message = RBundle.message("notification.console.noToolWindowFound")
        Log.error(message)
        setError(message)
      }
    }
    return runConsole(project)
  }

  fun registerContentManager(contentManager: ContentManager) {
    initialized = true
    contentManager.addContentManagerListener(object : ContentManagerListener {
      override fun contentRemoveQuery(event: ContentManagerEvent) {}

      override fun contentAdded(event: ContentManagerEvent) {
        isRunningConsole.set(false)
        if (consoleCounter.incrementAndGet() == 1) {
          RConsoleToolWindowFactory.setAvailableForRToolWindows(project, true)
          currentConsole = findComponentOfType(event.content.component, RConsoleView::class.java)
          currentConsole?.onSelect()
        }
      }

      override fun contentRemoved(event: ContentManagerEvent) {
        if (consoleCounter.decrementAndGet() == 0) {
          currentConsole = null
          RConsoleToolWindowFactory.setAvailableForRToolWindows(project, false)
        }
      }

      override fun selectionChanged(event: ContentManagerEvent) {
        currentConsole = findComponentOfType(event.content.component, RConsoleView::class.java)
        currentConsole?.onSelect()
      }
    })
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
        RInterpreterManager.getInstance(project).initializeInterpreter().onSuccess {
          doRunConsole(project, requestFocus).processed(promise)
        }
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
            RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show {
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
      return RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.contentManager?.let { cm ->
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