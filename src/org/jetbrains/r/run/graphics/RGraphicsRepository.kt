/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.File

class RGraphicsRepository(private val project: Project) {
  private val devices = mutableSetOf<RGraphicsDevice>()
  private val snapshotListeners = mutableListOf<(RSnapshotsUpdate) -> Unit>()

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
    notifyUpdate(device.lastUpdate)
  }

  @Synchronized
  fun addSnapshotListener(listener: (RSnapshotsUpdate) -> Unit) {
    snapshotListeners.add(listener)
    currentDevice?.let { device ->
      listener(device.lastUpdate)
    }
  }

  @Synchronized
  fun clearSnapshot(number: Int) {
    currentDevice?.clearSnapshot(number)
  }

  @Synchronized
  fun clearAllSnapshots() {
    currentDevice?.clearAllSnapshots()
  }

  @Synchronized
  fun rescale(snapshot: RSnapshot, newParameters: RGraphicsUtils.ScreenParameters, onRescale: (File) -> Unit) {
    currentDevice?.rescale(snapshot, newParameters, onRescale)
  }

  private fun notifyUpdate(update: RSnapshotsUpdate) {
    for (listener in snapshotListeners) {
      listener(update)
    }
  }

  companion object {
    fun getInstance(project: Project) = project.service<RGraphicsRepository>()
  }
}