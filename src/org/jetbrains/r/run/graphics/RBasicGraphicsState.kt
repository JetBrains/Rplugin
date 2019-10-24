/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.diagnostic.Logger
import java.io.File

class RBasicGraphicsState(override val tracedDirectory: File, initialParameters: RGraphicsUtils.ScreenParameters?) : RGraphicsState {
  private var lastTimestamp: Long? = null
  private var lastFiles: List<File>? = null

  private val listeners: MutableList<RGraphicsState.Listener> = mutableListOf()

  override val snapshots: List<File>
    get() = lastFiles ?: listOf()

  private val mutableScreenParameters = MutableLiveScreenParameters(initialParameters)
  override val screenParameters: LiveScreenParameters
    get() = mutableScreenParameters

  override fun update() {
    fun checkUpdated(files: List<File>, timestamp: Long?): Boolean {
      val last = lastFiles
      return if (last != null) {
        // No need to check against ID since each command creates a new plot
        files.count() != last.count() ||
        files.isNotEmpty() && timestamp!! > lastTimestamp!!
      } else {
        files.isNotEmpty()
      }
    }

    fun File.snapshotId(): Long {
      val name = this.name
      val startIndex = 9
      val endIndex = name.length - 4
      return name.substring(startIndex, endIndex).toLong()
    }

    data class Snapshot(
      val file: File,
      val timestamp: Long,
      val id: Long
    )

    tracedDirectory.listFiles { _, name -> name.endsWith("png") }?.let {
      val files = it.asList()
      val snapshots = files.map { f -> Snapshot(f, f.lastModified(), f.snapshotId()) }
      val timestamp = snapshots.map { s -> s.timestamp }.max()
      if (checkUpdated(files, timestamp)) {
        val ordered = snapshots.sortedWith(compareBy(Snapshot::timestamp, Snapshot::id))
        lastTimestamp = timestamp
        ordered.map { s -> s.file }.also { fs ->
          lastFiles = fs
          for (listener in listeners) {
            listener.onCurrentChange(fs)
          }
        }
      }
    }
  }

  override fun reset() {
    lastFiles?.let { files ->
      for (file in files) {
        file.delete()
      }
    }
    lastFiles = null
    lastTimestamp = null
    for (listener in listeners) {
      listener.onReset()
    }
  }

  override fun clearSnapshot(index: Int) {
    lastFiles?.let { files ->
      val file = files[index]
      if (file.delete()) {
        val shrunken = files.minus(file)
        for (listener in listeners) {
          listener.onCurrentChange(shrunken)
        }
        lastFiles = shrunken
      }
    }
  }

  override fun addListener(listener: RGraphicsState.Listener) {
    listeners.add(listener)
    lastFiles?.let {
      listener.onCurrentChange(it)
    }
  }

  override fun removeListener(listener: RGraphicsState.Listener) {
    listeners.remove(listener)
  }

  override fun changeScreenParameters(parameters: RGraphicsUtils.ScreenParameters) {
    mutableScreenParameters.postParameters(parameters)
  }

  class MutableLiveScreenParameters(private var currentParameters: RGraphicsUtils.ScreenParameters?) : LiveScreenParameters {
    private val listeners = mutableListOf<(RGraphicsUtils.ScreenParameters) -> Unit>()

    override val currentValue: RGraphicsUtils.ScreenParameters?
      get() = currentParameters

    override fun addListener(listener: (RGraphicsUtils.ScreenParameters) -> Unit) {
      listeners.add(listener)
      currentParameters?.let { parameters ->
        listener(parameters)
      }
    }

    fun postParameters(parameters: RGraphicsUtils.ScreenParameters) {
      currentParameters = parameters
      for (listener in listeners) {
        listener(parameters)
      }
    }
  }

  companion object {
    private val LOGGER = Logger.getInstance(RBasicGraphicsState::class.java)
  }
}