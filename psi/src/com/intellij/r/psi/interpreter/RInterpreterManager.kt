package com.intellij.r.psi.interpreter

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.asPromise

interface RInterpreterManager {
  val interpreterOrNull: RInterpreter?
  var interpreterLocation: RInterpreterLocation?

  /**
   * better to use [launchInterpreter] or [awaitInterpreter]
   */
  fun getInterpreterDeferred(force: Boolean = false): Deferred<Result<RInterpreter>>

  fun launchInterpreter(force: Boolean = false) {
    getInterpreterDeferred(force)
  }

  suspend fun awaitInterpreter(force: Boolean = false): Result<RInterpreter> =
    withContext(Dispatchers.IO) {
      getInterpreterDeferred(force).await()
    }

  fun hasInterpreterLocation(): Boolean

  fun restartInterpreter(afterRestart: Runnable? = null)

  companion object {
    private const val DOWNLOAD_R_PAGE = "https://cloud.r-project.org/"

    fun openDownloadRPage() {
      BrowserLauncher.instance.browse(DOWNLOAD_R_PAGE)
    }

    fun getInstance(project: Project): RInterpreterManager = project.service()
    private fun getInstanceIfCreated(project: Project): RInterpreterManager? = project.getServiceIfCreated(RInterpreterManager::class.java)

    @Deprecated("use RInterpreterManager.getInstance(project).getInterpreterDeferred(force) instead")
    @JvmOverloads
    fun getInterpreterAsync(project: Project): Promise<RInterpreter> =
      getInstance(project).getInterpreterDeferred().asCompletableFuture().asPromise().then<RInterpreter>{ it.getOrThrow() }

    fun getInterpreterOrNull(project: Project): RInterpreter? = getInstanceIfCreated(project)?.interpreterOrNull

    fun getInterpreterBlocking(project: Project, timeout: Int): RInterpreter? =
      runBlockingCancellable {
        withTimeoutOrNull(timeout.toLong()) {
          getInstance(project).awaitInterpreter().getOrNull()
        }
      }
  }
}