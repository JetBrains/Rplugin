package org.jetbrains.r.run.graphics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.r.rinterop.RInterop
import java.awt.Dimension
import java.io.File

class RVirtualGraphicsDevice(
  private val rInterop: RInterop,
  private val tracedDirectory: File,
  private val initialParameters: RGraphicsUtils.ScreenParameters
) : RGraphicsDevice {

  private var isLoaded = false
  private var lastNormal = listOf<RSnapshot>()
  private var lastZoomed = listOf<RSnapshot>()

  private val numbers2parameters = mutableMapOf<Int, RGraphicsUtils.ScreenParameters>()
  private val listeners = mutableListOf<(RSnapshotsUpdate) -> Unit>()

  override val lastUpdate: RSnapshotsUpdate
    get() = RSnapshotsUpdate(lastNormal, lastZoomed)

  override var configuration: RGraphicsDevice.Configuration = RGraphicsDevice.Configuration(initialParameters, null)
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

  override fun update() {
    rescale(null, configuration.screenParameters.dimension)
  }

  override fun reset() {
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

    lastNormal = shrunk(lastNormal, number)
    lastZoomed = shrunk(lastZoomed, number)
    numbers2parameters.remove(number)
    notifyListenersOnUpdate()
  }

  override fun clearAllSnapshots() {
    deleteSnapshots(lastNormal)
    deleteSnapshots(lastZoomed)
    lastNormal = listOf()
    lastZoomed = listOf()
    numbers2parameters.clear()
    for (listener in listeners) {
      listener(lastUpdate)
    }
  }

  override fun addListener(listener: (RSnapshotsUpdate) -> Unit) {
    listeners.add(listener)
    listener(lastUpdate)
  }

  override fun removeListener(listener: (RSnapshotsUpdate) -> Unit) {
    listeners.remove(listener)
  }

  private fun rescale(snapshotNumber: Int?, newDimension: Dimension) {
    if (isLoaded) {
      ApplicationManager.getApplication().executeOnPooledThread {
        rInterop.graphicsRescale(snapshotNumber, newDimension)
        lookForNewSnapshots(snapshotNumber)
      }
    }
  }

  private fun lookForNewSnapshots(tracedSnapshotNumber: Int?) {
    fun checkUpdated(snapshots: List<RSnapshot>): Boolean {
      val lastIdentities = lastNormal.map { it.identity }
      val currentIdentities = snapshots.map { it.identity }
      return lastIdentities != currentIdentities
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
        val tracedNumber = tracedSnapshotNumber ?: mostRecentNormal.last().number
        numbers2parameters[tracedNumber] = configuration.screenParameters
        postSnapshotNumber(tracedSnapshotNumber)
        notifyListenersOnUpdate()
      }
      deleteSnapshots(sketches)
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

  companion object {
    private val LOGGER = Logger.getInstance(RVirtualGraphicsDevice::class.java)

    private fun deleteSnapshots(snapshots: List<RSnapshot>) {
      for (snapshot in snapshots) {
        snapshot.file.delete()
      }
    }
  }
}
