/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.diagnostic.Logger
import java.io.File

class RBasicGraphicsState(override val tracedDirectory: File, initialParameters: RGraphicsUtils.ScreenParameters?) : RGraphicsState {
  private var lastSnapshots: List<RSnapshot>? = null

  private val listeners: MutableList<RGraphicsState.Listener> = mutableListOf()

  override val snapshots: List<RSnapshot>
    get() = lastSnapshots ?: listOf()

  private val mutableScreenParameters = MutableLiveScreenParameters(initialParameters)
  override val screenParameters: LiveScreenParameters
    get() = mutableScreenParameters

  override var currentSnapshotNumber: Int? = null
    set(value) {
      field = if (value != null) {
        val last = lastSnapshots
        if (last != null) {
          if (value in last.indices) {
            value
          } else {
            throw IndexOutOfBoundsException("Expected snapshot number from range ${last.indices} but got $value")
          }
        } else {
          throw IndexOutOfBoundsException("Cannot set snapshot number to $value: state hasn't got any snapshots")
        }
      } else {
        value
      }
    }

  override fun update() {
    fun checkUpdated(snapshots: List<RSnapshot>): Boolean {
      val last = lastSnapshots
      return if (last != null) {
        val lastIdentities = last.map { it.identity }
        val currentIdentities = snapshots.map { it.identity }
        return lastIdentities != currentIdentities
      } else {
        snapshots.isNotEmpty()
      }
    }

    tracedDirectory.listFiles { _, name -> name.endsWith("png") }?.let { files ->
      val allSnapshots = files.mapNotNull { RSnapshot.from(it) }
      val (snapshots, sketches) = allSnapshots.partition { it.type != RSnapshotType.SKETCH }
      val numbers2versions = snapshots.groupBy { it.number }
      val mostRecent = numbers2versions.mapNotNull { entry -> entry.value.maxBy { it.version } }.sortedBy { it.number }
      if (checkUpdated(mostRecent)) {
        lastSnapshots = mostRecent
        for (listener in listeners) {
          listener.onCurrentChange(mostRecent)
        }
      }

      // TODO [mine]: remove previous versions
      for (sketch in sketches) {
        sketch.file.delete()
      }
    }
  }

  override fun reset() {
    lastSnapshots?.let { snapshots ->
      for (snapshot in snapshots) {
        snapshot.file.delete()
      }
    }
    lastSnapshots = null
    for (listener in listeners) {
      listener.onReset()
    }
  }

  override fun clearSnapshot(number: Int) {
    lastSnapshots?.let { snapshots ->
      // TODO [mine]: this lookup is a kind of stupid. I guess it should be replaced with a map
      snapshots.find { it.number == number }?.let { snapshot ->
        if (snapshot.file.delete()) {
          val shrunken = snapshots.minus(snapshot)
          for (listener in listeners) {
            listener.onCurrentChange(shrunken)
          }
          lastSnapshots = shrunken
        }
      }
    }
  }

  override fun addListener(listener: RGraphicsState.Listener) {
    listeners.add(listener)
    lastSnapshots?.let {
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