/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.psi.stubs.StubUpdatingIndex
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginUtil
import com.intellij.r.psi.interpreter.*
import com.intellij.r.psi.rinterop.RInteropCoroutineScope
import com.intellij.r.psi.settings.RInterpreterSettings
import com.intellij.r.psi.settings.RSettings
import com.intellij.util.indexing.FileBasedIndex
import kotlinx.coroutines.*
import org.jetbrains.r.console.RConsoleManagerImpl
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.packages.remote.RepoProvider
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.statistics.RInterpretersCollector
import java.io.IOException
import java.nio.file.Paths

class NoRInterpreterException(message: String = "No R Interpreter"): RuntimeException(message)

internal class RInterpreterManagerImpl(
  private val project: Project,
  private val coroutineScope: CoroutineScope,
): RInterpreterManager {
  @Volatile
  private var interpreterDeferred: Deferred<Result<RInterpreter>> = CompletableDeferred(Result.failure<RInterpreter>(NoRInterpreterException()))
  @Volatile
  private var initialized = false

  @Volatile
  override var interpreterOrNull: RInterpreterBase? = null
    private set(value) {
      field?.onUnsetAsProjectInterpreter()
      field = value
      value?.onSetAsProjectInterpreter()
    }

  @Volatile
  override var interpreterLocation = fetchInterpreterLocation()

  init {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      invalidateRSkeletonCaches()
      removeLocalRDataTmpFiles()
    }
  }

  override fun getInterpreterDeferred(force: Boolean): Deferred<Result<RInterpreter>> {
    synchronized(this) {
      if (initialized && !force) return interpreterDeferred
      if (force) {
        interpreterOrNull = null
        interpreterLocation = fetchInterpreterLocation()
      }
      val location = interpreterLocation
                     ?: return CompletableDeferred(Result.failure<RInterpreter>(NoRInterpreterException())).also { interpreterDeferred = it }
      if (!initialized) {
        RLibraryWatcher.subscribeAsync(project, RLibraryWatcher.TimeSlot.FIRST) { roots ->
          val states = RInterpreterStateManager.getInstance(project).states
          val statesRoots = states.map { state -> state.libraryPaths.map { it.path } }
          for (i in states.indices) {
            val state = states[i]
            val stateRoots = statesRoots[i]
            if (roots.any { stateRoots.contains(it) }) {
              state.scheduleSkeletonUpdate()
            }
          }
        }
      }
      initialized = true

      val deferred = coroutineScope.async(ModalityState.defaultModalityState().asContextElement()) {
        setupInterpreter(location)
      }

      interpreterDeferred = deferred
      return deferred
    }
  }

  override fun hasInterpreterLocation() = RSettings.getInstance(project).interpreterLocation != null

  // TODO this function was originally a static function but I put it in the interface meanwhile because
  // because I don't want to deal with RToolWindowFactory yet.
  override fun restartInterpreter(afterRestart: Runnable?) {
    val manager = this
    RInteropCoroutineScope.getCoroutineScope(project).launch(ModalityState.defaultModalityState().asContextElement()) {
      val interpreter = manager.awaitInterpreter(force = true).getOrNull()
      if (interpreter != null) {
        RepoProvider.getInstance(project).onInterpreterVersionChange()
        launch(Dispatchers.EDT) {
          val packagesPanel = RToolWindowFactory.findContent(project, RToolWindowFactory.PACKAGES).component as RInstalledPackagesPanel
          packagesPanel.scheduleRefresh()
        }
      }
      RConsoleManagerImpl.getInstance(project).awaitCurrentConsole().onSuccess {
        withContext(Dispatchers.EDT) {
          RConsoleManagerImpl.closeMismatchingConsoles(project, interpreter)
          RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show(afterRestart)
        }
      }
    }
  }

  private fun fetchInterpreterLocation(): RInterpreterLocation? {
    return if (ApplicationManager.getApplication().isUnitTestMode) {
      RLocalInterpreterLocation(runBlockingMaybeCancellable { RInterpreterUtil.suggestHomePath() })
    } else {
      RSettings.getInstance(project).interpreterLocation
    }
  }

  private fun ensureInterpreterStored(interpreter: RInterpreter) {
    val info = RBasicInterpreterInfo(SUGGESTED_INTERPRETER_NAME, interpreter.interpreterLocation, interpreter.version)
    RInterpreterSettings.addOrEnableInterpreter(info)
  }

  private suspend fun setupInterpreter(location: RInterpreterLocation): Result<RInterpreter> {
    return withBackgroundProgress(project, RBundle.message("initializing.r.interpreter.message")) {
      location.createInterpreter(project).onSuccess { it ->
        interpreterOrNull = it
        ensureInterpreterStored(it)
        val suggestedInterpreters = RInterpretersCollector.collectFoundInterpreters(it)
        RInterpretersCollector.logSetupInterpreter(project, it, suggestedInterpreters)
      }.onFailure { e ->
        RInterpreterBase.LOG.warn(e)
        RSettings.getInstance(project).interpreterLocation = null
      }
    }
  }

  private fun removeLocalRDataTmpFiles() {
    try {
      val dir = Paths.get(project.basePath ?: return, ".RDataFiles").toFile()
      if (dir.isDirectory) {
        dir.listFiles()?.filter { ".RDataTmp" in it.name }?.forEach { it.delete() }
      }
    } catch (e: IOException) {
      RInterpreterBase.LOG.error(e)
    }
  }

  companion object {
    private val SUGGESTED_INTERPRETER_NAME = RBundle.message("project.settings.suggested.interpreter")
  }
}

/**
 * This method invalidate Stub Index on first start of R plugin
 * We need to do that to workaround IntelliJ 2020.1 platform bug when
 * skeleton files are not re-indexed on plugin update.
 */
private fun invalidateRSkeletonCaches() {
  val key = "rplugin.version"
  val version = RPluginUtil.getPlugin().version
  val propertiesComponent = PropertiesComponent.getInstance()
  val lastVersion = propertiesComponent.getValue(key)
  if (version != lastVersion) {
    propertiesComponent.setValue(key, version)
    FileBasedIndex.getInstance().requestRebuild(StubUpdatingIndex.INDEX_ID)
  }
}
