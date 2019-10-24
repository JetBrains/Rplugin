/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer

import com.intellij.openapi.diagnostic.Logger
import java.io.File

class RBasicViewerState(override val tracedFile: File) : RViewerState {
  private var lastTimestamp = tracedFile.lastModified()
  private var lastUrl: String? = null

  private val listeners: MutableList<RViewerState.Listener> = mutableListOf()

  override val url: String?
    get() = lastUrl

  override fun addListener(listener: RViewerState.Listener) {
    listeners.add(listener)
    lastUrl?.let {
      listener.onCurrentChange(it)
    }
  }

  override fun removeListener(listener: RViewerState.Listener) {
    listeners.remove(listener)
  }

  override fun update() {
    // Time granularity on MacOS seems to be not as fine as on Linux
    fun isUpdated(timestamp: Long, url: String): Boolean {
      return timestamp > lastTimestamp ||
             timestamp == lastTimestamp && lastUrl.let { it == null || it != url }
    }

    val currentTimestamp = tracedFile.lastModified()
    val lines = tracedFile.readLines()  // readText() is not suitable for Windows
    if (lines.isNotEmpty()) {
      val newUrl = lines[0]
      if (newUrl.isNotEmpty() && isUpdated(currentTimestamp, newUrl)) {
        lastTimestamp = currentTimestamp
        lastUrl = newUrl
        for (listener in listeners) {
          listener.onCurrentChange(newUrl)
        }
      }
    }
  }

  override fun reset() {
    lastUrl = null
    for (listener in listeners) {
      listener.onReset()
    }
  }

  companion object {
    private val LOGGER = Logger.getInstance(RBasicViewerState::class.java)
  }
}