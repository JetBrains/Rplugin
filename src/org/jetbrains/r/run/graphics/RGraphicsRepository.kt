/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class RGraphicsRepository(private val project: Project) {
  private val devices = mutableSetOf<RGraphicsDevice>()
  private val notNullDevicePromise = AsyncPromise<Unit>()
  private val updateListeners = mutableListOf<(RGraphicsUpdate) -> Unit>()

  private var currentDevice: RGraphicsDevice? = null

  var configuration: RGraphicsDevice.Configuration?
    get() {
      return synchronized(this) {
        currentDevice?.configuration
      }
    }
    set(value) {
      if (value != null) {
        synchronized(this) {
          currentDevice?.let { device ->
            device.configuration = value
          }
        }
      }
    }

  @Synchronized
  fun setActiveDevice(device: RGraphicsDevice) {
    if (device !in devices) {
      devices.add(device)
      device.addListener { update ->
        notifyUpdate(update)
      }
    }
    currentDevice = device
    if (!notNullDevicePromise.isSucceeded) {
      notNullDevicePromise.setResult(Unit)
    }
    notifyUpdate(device.lastUpdate)
  }

  @Synchronized
  fun addUpdateListener(listener: (RGraphicsUpdate) -> Unit) {
    updateListeners.add(listener)
    currentDevice?.let { device ->
      listener(device.lastUpdate)
    }
  }

  @Synchronized
  fun clearOutput(number: Int) {
    currentDevice?.clearOutput(number)
  }

  @Synchronized
  fun clearAllOutputs() {
    currentDevice?.clearAllOutputs()
  }

  @Synchronized
  fun rescaleStoredAsync(snapshot: RSnapshot, newParameters: RGraphicsUtils.ScreenParameters): Promise<RSnapshot?> {
    return notNullDevicePromise.thenAsync {
      currentDevice?.rescaleStoredAsync(snapshot, newParameters)
    }
  }

  @Synchronized
  fun createDeviceGroupAsync(directory: Path): Promise<Disposable> {
    return notNullDevicePromise.thenAsync {
      currentDevice?.createDeviceGroupAsync(directory)
    }
  }

  private fun notifyUpdate(update: RGraphicsUpdate) {
    for (listener in updateListeners) {
      listener(update)
    }
  }

  companion object {
    fun getInstance(project: Project) = project.service<RGraphicsRepository>()
  }
}