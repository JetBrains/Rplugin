/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.rinterop.RIExecutionResult
import org.jetbrains.r.rinterop.RInterop
import java.io.File

data class RSnapshotsUpdate(
  val normal: List<RSnapshot>,
  val zoomed: List<RSnapshot>
)

class RGraphicsDevice(
  private val rInterop: RInterop,
  private val tracedDirectory: File,
  initialParameters: RGraphicsUtils.ScreenParameters,
  inMemory: Boolean
) {

  private var lastNormal = emptyList<RSnapshot>()
  private var lastZoomed = emptyList<RSnapshot>()

  private val numbers2parameters = mutableMapOf<Int, RGraphicsUtils.ScreenParameters>()
  private val listeners = mutableListOf<(RSnapshotsUpdate) -> Unit>()
  private val devicePromise = AsyncPromise<Unit>()

  val lastUpdate: RSnapshotsUpdate
    get() = RSnapshotsUpdate(lastNormal, lastZoomed)

  var configuration: Configuration = Configuration(initialParameters, null)
    set(value) {
      field = value
      value.snapshotNumber?.let { number ->
        numbers2parameters[number]?.let { previousParameters ->
          val newParameters = value.screenParameters
          if (previousParameters != newParameters) {
            rescale(number, newParameters)
          }
        }
      }
    }

  init {
    val path = tracedDirectory.absolutePath
    val initProperties = RGraphicsUtils.calculateInitProperties(path, initialParameters)
    val result = rInterop.graphicsInit(initProperties, inMemory)
    val stderr = result.stderr
    if (stderr.isNotBlank()) {
      devicePromise.setError(stderr)
      LOGGER.error(stderr)
    } else {
      devicePromise.setResult(Unit)
    }
  }

  fun update() {
    rescale(null, configuration.screenParameters)
  }

  fun reset() {
    // Nothing to do here
  }

  fun clearSnapshot(number: Int) {
    lastNormal = removeSnapshotByNumber(lastNormal, number)
    lastZoomed = removeSnapshotByNumber(lastZoomed, number)
    numbers2parameters.remove(number)
    notifyListenersOnUpdate()
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

  fun shutdown() {
    rInterop.graphicsShutdown()
  }

  fun addListener(listener: (RSnapshotsUpdate) -> Unit) {
    listeners.add(listener)
    listener(lastUpdate)
  }

  fun removeListener(listener: (RSnapshotsUpdate) -> Unit) {
    listeners.remove(listener)
  }

  fun rescale(snapshot: RSnapshot, newParameters: RGraphicsUtils.ScreenParameters, onRescale: (File) -> Unit) {
    val parentDirectory = snapshot.file.parentFile
    rescale(newParameters, object : RescaleStrategy {
      override val hint = "Recorded snapshot at '${snapshot.file}'"

      override fun rescale(interop: RInterop, parameters: RGraphicsUtils.ScreenParameters): RIExecutionResult {
        val directoryPath = FileUtil.toSystemIndependentName(parentDirectory.absolutePath)
        return interop.graphicsRescaleStored(directoryPath, snapshot.number, snapshot.version, parameters)
      }

      override fun onSuccessfulRescale() {
        fetchLatestNormalSnapshots(parentDirectory)?.find { it.number == snapshot.number && it.version > snapshot.version }?.let { snapshot ->
          onRescale(snapshot.file)
        }
      }
    })
  }

  private fun rescale(snapshotNumber: Int?, newParameters: RGraphicsUtils.ScreenParameters) {
    rescale(newParameters, object : RescaleStrategy {
      override val hint = if (snapshotNumber != null) "In-Memory snapshot #${snapshotNumber}" else "Last in-memory snapshots"

      override fun rescale(interop: RInterop, parameters: RGraphicsUtils.ScreenParameters): RIExecutionResult {
        return interop.graphicsRescale(snapshotNumber, parameters)
      }

      override fun onSuccessfulRescale() {
        lookForNewSnapshots(snapshotNumber)
      }
    })
  }

  private fun rescale(newParameters: RGraphicsUtils.ScreenParameters, strategy: RescaleStrategy) {
    if (rInterop.isAlive) {
      devicePromise.onSuccess {
        ApplicationManager.getApplication().executeOnPooledThread {
          val result = strategy.rescale(rInterop, RGraphicsUtils.scaleForRetina(newParameters))
          if (result.stderr.isNotBlank()) {
            // Note: This might be due to large margins and therefore shouldn't be treated as a fatal error
            LOGGER.warn("Rescale for <${strategy.hint}> has failed:\n${result.stderr}")
          }
          if (result.stdout.isNotBlank()) {
            val output = result.stdout.let { it.substring(4, it.length - 1) }
            if (output == "TRUE") {
              strategy.onSuccessfulRescale()
            }
          } else if (result.stderr.isBlank()) {
            LOGGER.error("Cannot get any output from graphics device")
          }
        }
      }
    }
  }

  private fun lookForNewSnapshots(tracedSnapshotNumber: Int?) {
    fetchLatestSnapshots(tracedDirectory)?.let { type2snapshots ->
      val normal = type2snapshots[RSnapshotType.NORMAL] ?: listOf()
      val zoomed = type2snapshots[RSnapshotType.ZOOMED] ?: listOf()
      if (checkUpdated(normal)) {
        traceUpdatedSnapshots(normal)
        lastNormal = normal
        lastZoomed = zoomed
        postSnapshotNumber(tracedSnapshotNumber)
        notifyListenersOnUpdate()
      }
    }
  }

  private fun traceUpdatedSnapshots(mostRecentNormal: List<RSnapshot>) {
    val latestIdentities = mostRecentNormal.toIdentities().toSet()
    val notTracedIdentities = latestIdentities.minus(lastNormal.toIdentities())
    for (identity in notTracedIdentities) {
      numbers2parameters[identity.first] = configuration.screenParameters
    }
  }

  private fun checkUpdated(snapshots: List<RSnapshot>): Boolean {
    val lastIdentities = lastNormal.map { Pair(it.number, it.version) }
    val currentIdentities = snapshots.map { Pair(it.number, it.version) }
    return lastIdentities != currentIdentities
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

  private interface RescaleStrategy {
    val hint: String
    fun rescale(interop: RInterop, parameters: RGraphicsUtils.ScreenParameters): RIExecutionResult
    fun onSuccessfulRescale()
  }

  companion object {
    private val LOGGER = Logger.getInstance(RGraphicsDevice::class.java)

    fun fetchLatestNormalSnapshots(directory: File): List<RSnapshot>? {
      return fetchLatestSnapshots(directory)?.get(RSnapshotType.NORMAL)
    }

    private fun fetchLatestSnapshots(directory: File): Map<RSnapshotType, List<RSnapshot>>? {
      return directory.listFiles { _, name -> name.endsWith("png") }?.let { files ->
        val type2snapshots = files.mapNotNull { RSnapshot.from(it) }.groupBy { it.type }
        val latest = type2snapshots.asSequence().mapNotNull { entry ->
          shrinkOrDeleteSnapshots(entry.key, entry.value)
        }
        latest.toMap()
      }
    }

    private fun shrinkOrDeleteSnapshots(type: RSnapshotType, snapshots: List<RSnapshot>): Pair<RSnapshotType, List<RSnapshot>>? {
      return if (type != RSnapshotType.SKETCH) {
        Pair(type, snapshots.groupAndShrinkBy { it.version })
      } else {
        deleteSnapshots(snapshots)
        null
      }
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

    private fun List<RSnapshot>.toIdentities(): Sequence<Pair<Int, Int>> {
      return asSequence().map { Pair(it.number, it.version) }
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

    private fun deleteSnapshots(snapshots: List<RSnapshot>) {
      for (snapshot in snapshots) {
        snapshot.file.delete()
      }
    }
  }
}
