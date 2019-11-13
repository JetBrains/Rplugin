/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.r.rinterop.RInterop
import java.awt.Dimension
import java.io.File

data class RSnapshotsUpdate(
  val normal: List<RSnapshot>,
  val zoomed: List<RSnapshot>
)

class RGraphicsDevice(
  private val rInterop: RInterop,
  private val tracedDirectory: File,
  private val initialParameters: RGraphicsUtils.ScreenParameters
) {

  private var isLoaded = false
  private var lastNormal = emptyList<RSnapshot>()
  private var lastZoomed = emptyList<RSnapshot>()

  private val numbers2parameters = mutableMapOf<Int, RGraphicsUtils.ScreenParameters>()
  private val listeners = mutableListOf<(RSnapshotsUpdate) -> Unit>()

  val lastUpdate: RSnapshotsUpdate
    get() = RSnapshotsUpdate(lastNormal, lastZoomed)

  var configuration: Configuration = Configuration(initialParameters, null)
    set(value) {
      val oldConfiguration = field
      val number = value.snapshotNumber
      field = value

      if (oldConfiguration.screenParameters.resolution != value.screenParameters.resolution) {
        reset()
      }

      if (number != null) {
        val newDimension = value.screenParameters.dimension
        val previousDimension = numbers2parameters[number]?.dimension
        if (previousDimension != null && previousDimension != newDimension) {
          rescale(number, newDimension)
        }
      }
    }

  init {
    reset()
  }

  fun update() {
    rescale(null, configuration.screenParameters.dimension)
  }

  fun reset() {
    val path = tracedDirectory.absolutePath
    val initialDimension = initialParameters.dimension
    val resolution = configuration.screenParameters.resolution
    val initProperties = RGraphicsUtils.calculateInitProperties(path, initialDimension, resolution)
    val result = rInterop.graphicsInit(initProperties)
    isLoaded = if (result.stderr.isNotBlank()) {
      LOGGER.error(result.stderr)
      false
    } else {
      true
    }
  }

  fun clearSnapshot(number: Int) {
    lastNormal = removeSnapshotByNumber(lastNormal, number)
    lastZoomed = removeSnapshotByNumber(lastZoomed, number)
    numbers2parameters.remove(number)
    notifyListenersOnUpdate()
  }

  private fun removeSnapshotByNumber(snapshots: List<RSnapshot>, number: Int): List<RSnapshot> {
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

  fun clearAllSnapshots() {
    deleteSnapshots(lastNormal)
    deleteSnapshots(lastZoomed)
    lastNormal = listOf()
    lastZoomed = listOf()
    numbers2parameters.clear()
    for (listener in listeners) {
      listener(lastUpdate)
    }
  }

  fun addListener(listener: (RSnapshotsUpdate) -> Unit) {
    listeners.add(listener)
    listener(lastUpdate)
  }

  fun removeListener(listener: (RSnapshotsUpdate) -> Unit) {
    listeners.remove(listener)
  }

  private fun rescale(snapshotNumber: Int?, newDimension: Dimension) {
    if (isLoaded) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val result = rInterop.graphicsRescale(snapshotNumber, RGraphicsUtils.scaleForRetina(newDimension))
        if (result.stderr.isNotBlank()) {
          // Note: This might be due to large margins and therefore shouldn't be treated as a fatal error
          LOGGER.warn("Rescale for snapshot <$snapshotNumber> has failed:\n${result.stderr}")
        }
        if (result.stdout.isNotBlank()) {
          val output = result.stdout.let { it.substring(4, it.length - 1) }
          if (output == "TRUE") {
            lookForNewSnapshots(snapshotNumber)
          }
        } else if (result.stderr.isBlank()) {
          LOGGER.error("Cannot get any output from graphics device")
        }
      }
    }
  }

  private fun lookForNewSnapshots(tracedSnapshotNumber: Int?) {
    tracedDirectory.listFiles { _, name -> name.endsWith("png") }?.let { files ->
      val allSnapshots = files.mapNotNull { RSnapshot.from(it) }
      val type2snapshots = allSnapshots.groupBy { it.type }
      val normal = type2snapshots[RSnapshotType.NORMAL] ?: listOf()
      val zoomed = type2snapshots[RSnapshotType.ZOOMED] ?: listOf()
      val sketches = type2snapshots[RSnapshotType.SKETCH] ?: listOf()
      val mostRecentNormal = normal.groupAndShrinkBy { it.version }
      val orderedZoomed = zoomed.sortedBy { it.number }
      if (checkUpdated(mostRecentNormal)) {
        lastNormal = mostRecentNormal
        lastZoomed = orderedZoomed
        val tracedNumber = tracedSnapshotNumber ?: mostRecentNormal.last().number
        numbers2parameters[tracedNumber] = configuration.screenParameters
        postSnapshotNumber(tracedSnapshotNumber)
        notifyListenersOnUpdate()
      }
      deleteSnapshots(sketches)
    }
  }

  private fun checkUpdated(snapshots: List<RSnapshot>): Boolean {
    val lastIdentities = lastNormal.map { Pair(it.number, it.version) }
    val currentIdentities = snapshots.map { Pair(it.number, it.version) }
    return lastIdentities != currentIdentities
  }

  private fun <K: Comparable<K>>List<RSnapshot>.groupAndShrinkBy(key: (RSnapshot) -> K): List<RSnapshot> {
    val groups = groupBy { it.number }
    return groups.mapNotNull { entry -> entry.value.shrinkBy(key) }.sortedBy { it.number }
  }

  private fun <K: Comparable<K>>List<RSnapshot>.shrinkBy(key: (RSnapshot) -> K): RSnapshot? {
    return if (isNotEmpty()) {
      val ordered = sortedBy { key(it) }
      for (i in 0 until ordered.size - 1) {
        ordered[i].file.delete()
      }
      ordered.last()
    } else {
      null
    }
  }

  private fun postSnapshotNumber(number: Int?) {
    configuration = configuration.copy(snapshotNumber = number)
  }

  private fun notifyListenersOnUpdate() {
    for (listener in listeners) {
      listener(lastUpdate)
    }
  }

  data class Configuration(
    val screenParameters: RGraphicsUtils.ScreenParameters,
    val snapshotNumber: Int?
  )

  companion object {
    private val LOGGER = Logger.getInstance(RGraphicsDevice::class.java)

    private fun deleteSnapshots(snapshots: List<RSnapshot>) {
      for (snapshot in snapshots) {
        snapshot.file.delete()
      }
    }
  }
}
