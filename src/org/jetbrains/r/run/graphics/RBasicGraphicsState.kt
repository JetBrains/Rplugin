/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.diagnostic.Logger
import java.io.File

class RBasicGraphicsState(override val tracedDirectory: File, initialParameters: RGraphicsUtils.ScreenParameters?) : RGraphicsState {
  private var lastNormal: List<RSnapshot>? = null
  private var lastZoomed: List<RSnapshot> = mutableListOf()

  private val listeners: MutableList<RGraphicsState.Listener> = mutableListOf()

  override val snapshots: RSnapshotsUpdate
    get() = RSnapshotsUpdate(lastNormal ?: listOf(), lastZoomed)

  private val mutableScreenParameters = MutableLiveScreenParameters(initialParameters)
  override val screenParameters: LiveScreenParameters
    get() = mutableScreenParameters

  override var currentSnapshotNumber: Int? = null
    set(value) {
      field = if (value != null) {
        val last = lastNormal
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
      val last = lastNormal
      return if (last != null) {
        val lastIdentities = last.map { it.identity }
        val currentIdentities = snapshots.map { it.identity }
        return lastIdentities != currentIdentities
      } else {
        snapshots.isNotEmpty()
      }
    }

    fun <K: Comparable<K>>shrunkGroups(groups: Map<Int, List<RSnapshot>>, key: (RSnapshot) -> K): List<RSnapshot> {
      fun shrunk(snapshots: List<RSnapshot>, key: (RSnapshot) -> K): RSnapshot? {
        return if (snapshots.isNotEmpty()) {
          val ordered = snapshots.sortedBy { key(it) }
          for (i in 0 until ordered.size - 1) {
            ordered[i].file.delete()
          }
          ordered.last()
        } else {
          null
        }
      }

      return groups.mapNotNull { entry -> shrunk(entry.value, key) }.sortedBy { it.number }
    }

    tracedDirectory.listFiles { _, name -> name.endsWith("png") }?.let { files ->
      val allSnapshots = files.mapNotNull { RSnapshot.from(it) }
      val type2snapshots = allSnapshots.groupBy { it.type }
      val normal = type2snapshots[RSnapshotType.NORMAL] ?: listOf()
      val zoomed = type2snapshots[RSnapshotType.ZOOMED] ?: listOf()
      val sketches = type2snapshots[RSnapshotType.SKETCH] ?: listOf()
      val numbers2normal = normal.groupBy { it.number }
      val mostRecentNormal = shrunkGroups(numbers2normal) { it.version }
      val numbers2zoomed = zoomed.groupBy { it.number }
      val mostOldZoomed = shrunkGroups(numbers2zoomed) { -it.version }  // Note: minus was intentional
      if (checkUpdated(mostRecentNormal)) {
        lastNormal = mostRecentNormal
        lastZoomed = mostOldZoomed
        notifyListenersOnUpdate()
      }
      deleteSnapshots(sketches)
    }
  }

  override fun reset() {
    lastNormal?.let { normal ->
      deleteSnapshots(normal)
      deleteSnapshots(lastZoomed)
    }
    lastNormal = null
    for (listener in listeners) {
      listener.onReset()
    }
  }

  override fun clearSnapshot(number: Int) {
    fun shrunk(snapshots: List<RSnapshot>, number: Int): List<RSnapshot> {
      val snapshot = snapshots.find { it.number == number }
      return if (snapshot != null) {
        if (snapshot.file.delete()) {
          snapshots.minus(snapshot)
        } else {
          snapshots
        }
      } else {
        snapshots
      }
    }

    lastNormal?.let { normal ->
      lastNormal = shrunk(normal, number)
      lastZoomed = shrunk(lastZoomed, number)
      notifyListenersOnUpdate()
    }
  }

  override fun addListener(listener: RGraphicsState.Listener) {
    listeners.add(listener)
    lastNormal?.let {
      listener.onCurrentChange(RSnapshotsUpdate(it, lastZoomed))
    }
  }

  override fun removeListener(listener: RGraphicsState.Listener) {
    listeners.remove(listener)
  }

  override fun changeScreenParameters(parameters: RGraphicsUtils.ScreenParameters) {
    mutableScreenParameters.postParameters(parameters)
  }

  private fun notifyListenersOnUpdate() {
    lastNormal?.let { normal ->
      for (listener in listeners) {
        listener.onCurrentChange(RSnapshotsUpdate(normal, lastZoomed))
      }
    }
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

    private fun deleteSnapshots(snapshots: List<RSnapshot>) {
      for (snapshot in snapshots) {
        snapshot.file.delete()
      }
    }
  }
}