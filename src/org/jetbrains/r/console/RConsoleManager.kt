/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.ui.UIUtil.findComponentOfType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.asDeferred
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.RBundle
import org.jetbrains.r.configuration.RInterpreterBarWidgetFactory
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.packages.RPackageProjectManager
import java.util.concurrent.atomic.AtomicInteger

@Service(Service.Level.PROJECT)
class RConsoleManager(
  private val project: Project,
  private val coroutineScope: CoroutineScope
) {
  @Volatile
  private var currentConsole: RConsoleView? = null
  private val consoleCounter: AtomicInteger = AtomicInteger()
  private var consolePromise: Promise<RConsoleView>? = null
  @Volatile
  var initialized = false
    private set

  @Deprecated("use awaitCurrentConsole() instead")
  val currentConsoleAsync: Promise<RConsoleView>
    get() = currentConsole?.let { resolvedPromise(it) } ?: runConsole()

  suspend fun awaitCurrentConsole(): Result<RConsoleView> =
    runCatching {
      currentConsoleAsync.asDeferred().await()
    }

  private fun run(lambda: (RConsoleView) -> Unit): Promise<Unit> {
    return currentConsoleAsync.onError {
      throw IllegalStateException("Cannot run console", it)
    }.then {
      lambda(it)
    }.onError { e ->
      if (e is ProcessCanceledException || e is InterruptedException) {
        return@onError
      }
      LOGGER.error(e)
    }
  }

  fun runAsync(lambda: (RConsoleView) -> Unit): Promise<Unit> {
    val result = AsyncPromise<Unit>()
    run { org.jetbrains.concurrency.runAsync { lambda(it) }.processed(result) }
    return result
  }

  @Synchronized
  private fun runConsole(): Promise<RConsoleView> {
    consolePromise?.let { return it }
    return runSingleConsole().also {
      consolePromise = it
    }.onProcessed {
      synchronized(this) {
        consolePromise = null
      }
    }.onError {
    }
  }

  val currentConsoleOrNull: RConsoleView?
    get() = currentConsole

  val consoles: List<RConsoleView>
    get() = getContentDescription(project)?.contentConsolePairs?.map { it.second }?.toList() ?: listOf()


  private fun runSingleConsole(): Promise<RConsoleView> {
    if (RConsoleToolWindowFactory.getRConsoleToolWindows(project) == null) {
      return AsyncPromise<RConsoleView>().apply {
        val message = RBundle.message("notification.console.noToolWindowFound")
        LOGGER.error(message)
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
        if (RConsoleToolWindowFactory.isConsole(event.content) && consoleCounter.incrementAndGet() == 1) {
          RConsoleToolWindowFactory.setAvailableForRToolWindows(project, true)
          currentConsole = findComponentOfType(event.content.component, RConsoleView::class.java)
          currentConsole?.onSelect()
        }
        RInterpreterBarWidgetFactory.updateWidget(project)
      }

      override fun contentRemoved(event: ContentManagerEvent) {
        if (RConsoleToolWindowFactory.isConsole(event.content) && consoleCounter.decrementAndGet() == 0) {
          currentConsole = null
          RConsoleToolWindowFactory.setAvailableForRToolWindows(project, false)
        }
        RInterpreterBarWidgetFactory.updateWidget(project)
      }

      override fun selectionChanged(event: ContentManagerEvent) {
        if (!RConsoleToolWindowFactory.isConsole(event.content)) return
        val eventConsole = findComponentOfType(event.content.component, RConsoleView::class.java)
        if (event.content.isSelected) {
          currentConsole = eventConsole
          currentConsole?.onSelect()
        } else {
          eventConsole?.rInterop?.state?.markOutdated()
        }
      }
    })
  }

  @TestOnly
  internal fun setCurrentConsoleForTests(console: RConsoleView?) {
    check(ApplicationManager.getApplication().isUnitTestMode)
    currentConsole = console
  }

  companion object {
    private val LOGGER = Logger.getInstance(RConsoleManager::class.java)

    private data class ContentDescription(
      val contentManager: ContentManager,
      val contentConsolePairs: Sequence<Pair<Content, RConsoleView>>
    )

    fun getInstance(project: Project): RConsoleManager {
      return project.service()
    }

    /**
     * Success promise means that [currentConsoleOrNull] is not null
     */
    fun runConsole(project: Project, requestFocus: Boolean = false, workingDir: String? = null): Promise<RConsoleView> {
      val promise = AsyncPromise<RConsoleView>()
      doRunConsole(project, requestFocus, workingDir).processed(promise)
      return promise
    }

    /**
     * Close all consoles that has interpreter different than [interpreter]
     */
    fun closeMismatchingConsoles(project: Project, interpreter: RInterpreter?) {
      getContentDescription(project)?.let { description ->
        for ((content, console) in description.contentConsolePairs) {
          if (console.interpreter != interpreter) {
            console.rInterop.state.cancelStateUpdating()
            description.contentManager.removeContent(content, true)
          }
        }
      }
    }

    private fun doRunConsole(project: Project, requestFocus: Boolean, workingDir: String?): Promise<RConsoleView> {
      val result = AsyncPromise<RConsoleView>()
      RInterpreterManager.getInterpreterAsync(project).onSuccess { interpreter ->
        RConsoleRunner(interpreter, workingDir ?: interpreter.basePath).initAndRun().onSuccess { console ->
          result.setResult(console)
          getInstance(project).coroutineScope.launch(Dispatchers.EDT) {
            val toolWindow = RConsoleToolWindowFactory.getRConsoleToolWindows(project)
            if (requestFocus) {
              toolWindow?.show {
                val focusManager = IdeFocusManager.findInstanceByComponent(console)
                focusManager.requestFocusInProject(console.consoleEditor.contentComponent, project)
              }
            }
            toolWindow?.component?.validate()
          }
          RPackageProjectManager.getInstance(project).loadOrSuggestToInstallMissedPackages()
        }.onError {
          result.setError(it)
        }
      }.onError {
        result.setError(RBundle.message("r.console.missing.path.to.r.message"))
      }
      return result
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