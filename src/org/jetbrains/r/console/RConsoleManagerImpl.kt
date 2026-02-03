/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.console.RConsoleManager
import com.intellij.r.psi.console.RConsoleView
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.r.psi.interpreter.RInterpreterManager
import com.intellij.r.psi.packages.RPackageProjectManager
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
import org.jetbrains.concurrency.await
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.configuration.RInterpreterBarWidgetFactory
import java.util.concurrent.atomic.AtomicInteger

private val LOG = fileLogger()

class RConsoleManagerImpl(
  private val project: Project,
  private val coroutineScope: CoroutineScope
) : RConsoleManager {
  @Volatile
  private var currentConsole: RConsoleViewImpl? = null
  private val consoleCounter: AtomicInteger = AtomicInteger()
  private var consolePromise: Promise<RConsoleViewImpl>? = null
  @Volatile
  var initialized = false
    private set

  @Deprecated("use awaitCurrentConsole() instead")
  val currentConsoleAsync: Promise<RConsoleViewImpl>
    get() = currentConsole?.let { resolvedPromise(it) } ?: runConsole()

  suspend fun awaitCurrentConsole(): Result<RConsoleViewImpl> =
    runCatching {
      currentConsoleAsync.await()
    }

  @Synchronized
  private fun runConsole(): Promise<RConsoleViewImpl> {
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

  override fun runConsole(requestFocus: Boolean, workingDir: String?): Promise<RConsoleView> {
    return runConsole(project, requestFocus, workingDir).then { it as RConsoleView }
  }

  val currentConsoleOrNull: RConsoleViewImpl?
    get() = currentConsole

  override val consoles: List<RConsoleViewImpl> get() = consolesCache
  var consolesCache: List<RConsoleViewImpl> = listOf()

  private fun runSingleConsole(): Promise<RConsoleViewImpl> {
    if (RConsoleToolWindowFactory.getRConsoleToolWindows(project) == null) {
      return AsyncPromise<RConsoleViewImpl>().apply {
        val message = RBundle.message("notification.console.noToolWindowFound")
        LOG.error(message)
        setError(message)
      }
    }
    return runConsole(project)
  }

  private fun updateConsolesCache() {
    consolesCache = getContentDescription(project)?.contentConsolePairs?.map { it.second }?.toList() ?: listOf()
  }

  fun registerContentManager(contentManager: ContentManager) {
    initialized = true
    contentManager.addContentManagerListener(object : ContentManagerListener {
      override fun contentRemoveQuery(event: ContentManagerEvent) {}

      override fun contentAdded(event: ContentManagerEvent) {
        if (RConsoleToolWindowFactory.isConsole(event.content) && consoleCounter.incrementAndGet() == 1) {
          RConsoleToolWindowFactory.setAvailableForRToolWindows(project, true)
          currentConsole = findComponentOfType(event.content.component, RConsoleViewImpl::class.java)
          currentConsole?.onSelect()
        }
        RInterpreterBarWidgetFactory.updateWidget(project)
        updateConsolesCache()
      }

      override fun contentRemoved(event: ContentManagerEvent) {
        if (RConsoleToolWindowFactory.isConsole(event.content) && consoleCounter.decrementAndGet() == 0) {
          currentConsole = null
          RConsoleToolWindowFactory.setAvailableForRToolWindows(project, false)
        }
        RInterpreterBarWidgetFactory.updateWidget(project)
        updateConsolesCache()
      }

      override fun selectionChanged(event: ContentManagerEvent) {
        if (!RConsoleToolWindowFactory.isConsole(event.content)) return
        val eventConsole = findComponentOfType(event.content.component, RConsoleViewImpl::class.java)
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
  internal fun setCurrentConsoleForTests(console: RConsoleViewImpl?) {
    check(ApplicationManager.getApplication().isUnitTestMode)
    currentConsole = console
  }

  companion object {
    private data class ContentDescription(
      val contentManager: ContentManager,
      val contentConsolePairs: Sequence<Pair<Content, RConsoleViewImpl>>
    )

    fun getInstance(project: Project): RConsoleManagerImpl {
      return project.service<RConsoleManager>() as RConsoleManagerImpl
    }

    /**
     * Success promise means that [currentConsoleOrNull] is not null
     */
    fun runConsole(project: Project, requestFocus: Boolean = false, workingDir: String? = null): Promise<RConsoleViewImpl> {
      val promise = AsyncPromise<RConsoleViewImpl>()
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

    private fun doRunConsole(project: Project, requestFocus: Boolean, workingDir: String?): Promise<RConsoleViewImpl> {
      val result = AsyncPromise<RConsoleViewImpl>()
      RInterpreterManager.getInterpreterAsync(project).onSuccess { interpreter ->
        RConsoleRunner(interpreter, workingDir ?: interpreter.basePath).initAndRun().onSuccess { console ->
          result.setResult(console)
          getInstance(project).coroutineScope.launch(Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()) {
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
            findComponentOfType(it.component, RConsoleViewImpl::class.java)?.let { console ->
              Pair(it, console)
            }
          }
        ContentDescription(cm, pairs)
      }
    }
  }
}