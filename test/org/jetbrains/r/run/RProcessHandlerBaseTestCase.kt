/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

abstract class RProcessHandlerBaseTestCase : RUsefulTestCase() {
  protected lateinit var rInterop: RInterop

  override fun setUp() {
    super.setUp()
    rInterop = getRInterop(project)
  }

  companion object {
    @Volatile private var cachedInterop: RInterop? = null

    private fun getRInterop(project: Project): RInterop {
      var rInterop = cachedInterop
      if (rInterop == null || !validateRInterop(rInterop)) {
        if (rInterop != null && !Disposer.isDisposed(rInterop)) {
          Disposer.dispose(rInterop)
        }
        rInterop = RInteropUtil.runRWrapperAndInterop(project).blockingGet(3000)!!
        cachedInterop = rInterop
        rInterop.processHandler.addProcessListener(object : ProcessListener {
          override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
          }

          override fun processTerminated(event: ProcessEvent) {
            cachedInterop = null
          }

          override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
          }

          override fun startNotified(event: ProcessEvent) {
          }
        })
      } else {
        rInterop.clearEnvironment(rInterop.globalEnvRef)
      }
      return rInterop
    }

    private fun validateRInterop(rInterop: RInterop): Boolean {
      return try {
        val output = StringBuilder()
        rInterop.executeCodeAsync("cat(1)") { it, _ ->
          output.append(it)
        }.blockingGet(1000)
        output.toString() == "1"
      } catch (e: ExecutionException) {
        false
      } catch (e: TimeoutException) {
        false
      }
    }
  }
}
