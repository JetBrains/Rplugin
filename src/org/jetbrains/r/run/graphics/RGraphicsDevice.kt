/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.rinterop.RIExecutionResult
import org.jetbrains.r.rinterop.RInterop
import java.io.File

class RGraphicsDevice(
  private val rInterop: RInterop,
  private val shadowDirectory: File,
  initialParameters: RGraphicsUtils.ScreenParameters,
  inMemory: Boolean
) {

  private var lastNormal = emptyList<RSnapshot>()

  private val number2SnapshotInfos = mutableMapOf<Int, SnapshotInfo>()
  private val listeners = mutableListOf<(List<RSnapshot>) -> Unit>()
  private val devicePromise = AsyncPromise<Unit>()

  val lastUpdate: List<RSnapshot>
    get() = lastNormal

  var configuration: Configuration = Configuration(initialParameters, null)
    set(value) {
      field = value
      value.snapshotNumber?.let { number ->
        number2SnapshotInfos[number]?.parameters?.let { previousParameters ->
          val newParameters = value.screenParameters
          if (previousParameters != newParameters) {
            rescale(number, newParameters)
          }
        }
      }
    }

  init {
    val parameters = RGraphicsUtils.calculateInitParameters(initialParameters)
    val result = rInterop.graphicsInit(parameters, inMemory)
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
    number2SnapshotInfos.remove(number)
    notifyListenersOnUpdate()
  }

  fun clearAllSnapshots() {
    deleteSnapshots(lastNormal, true)
    lastNormal = listOf()
    number2SnapshotInfos.clear()
    for (listener in listeners) {
      listener(lastUpdate)
    }
  }

  fun shutdown() {
    rInterop.graphicsShutdown()
  }

  fun addListener(listener: (List<RSnapshot>) -> Unit) {
    listeners.add(listener)
    listener(lastUpdate)
  }

  fun removeListener(listener: (List<RSnapshot>) -> Unit) {
    listeners.remove(listener)
  }

  fun createDeviceGroupAsync(snapshot: RSnapshot): Promise<RDeviceGroup> {
    if (!rInterop.isAlive) {
      return rejectedPromise("Interop has already terminated")
    }
    val promise = createEmptyGroupAsync().then { groupId ->
      val recorded = snapshot.recordedFile.readBytes()
      rInterop.graphicsPushSnapshot(groupId, snapshot.number, recorded)
      RDeviceGroup(groupId, rInterop)
    }
    return promise.onError { e ->
      LOGGER.error("Cannot create device group", e)
    }
  }

  private fun createEmptyGroupAsync() = runAsync {
    val result = rInterop.graphicsCreateGroup()
    if (result.stderr.isNotBlank()) {
      throw RuntimeException("Cannot create empty group. Stderr was:\n${result.stderr}")
    }
    extractGroupIdFrom(result.stdout)
  }

  private fun extractGroupIdFrom(stdout: String): String {
    // "[1] 'groupId'\n"
    if (stdout.length < 8) {
      throw RuntimeException("Unexpected output from interop:\n$stdout")
    }
    return stdout.substring(5, stdout.length - 2)
  }

  fun rescale(snapshot: RSnapshot, group: RDeviceGroup, newParameters: RGraphicsUtils.ScreenParameters, onRescale: (File) -> Unit) {
    rescale(newParameters, object : RescaleStrategy {
      override val hint = "Recorded snapshot #${snapshot.number} at '${group.id}'"

      override fun rescale(interop: RInterop, parameters: RGraphicsUtils.ScreenParameters): RIExecutionResult {
        return interop.graphicsRescaleStored(group.id, snapshot.number, snapshot.version, parameters)
      }

      override fun onSuccessfulRescale() {
        pullStoredSnapshot(snapshot, group.id, hint)?.let { pulled ->
          onRescale(pulled.file)
        }
      }
    })
  }

  private fun rescale(snapshotNumber: Int?, newParameters: RGraphicsUtils.ScreenParameters) {
    rescale(newParameters, object : RescaleStrategy {
      override val hint = createHintFor(snapshotNumber)

      override fun rescale(interop: RInterop, parameters: RGraphicsUtils.ScreenParameters): RIExecutionResult {
        return interop.graphicsRescale(snapshotNumber, parameters)
      }

      override fun onSuccessfulRescale() {
        lookForNewSnapshots(snapshotNumber)
      }
    })
  }

  @Synchronized
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
          } else if (result.stderr.isBlank() && rInterop.isAlive) {
            LOGGER.error("Cannot get any output from graphics device")
          }
        }
      }
    }
  }

  private fun lookForNewSnapshots(tracedSnapshotNumber: Int?) {
    try {
      rInterop.graphicsPullChangedNumbers()?.let { numbers ->
        pullAndUpdateInMemorySnapshots(numbers)
        lastNormal = collectLatestSnapshots()
        postSnapshotNumber(tracedSnapshotNumber)
        notifyListenersOnUpdate()
      }
    } catch (e: Exception) {
      LOGGER.error("Cannot pull numbers of rescaled snapshots", e)
    }
  }

  private fun collectLatestSnapshots(): List<RSnapshot> {
    return number2SnapshotInfos.values.map { it.snapshot }.sortedBy { it.number }
  }

  private fun pullAndUpdateInMemorySnapshots(numbers: List<Int>) {
    for (number in numbers) {
      pullInMemorySnapshot(number)?.let { snapshot ->
        val snapshotInfo = SnapshotInfo(snapshot, configuration.screenParameters)
        number2SnapshotInfos[number] = snapshotInfo
      }
    }
  }

  private fun pullInMemorySnapshot(number: Int): RSnapshot? {
    val previous = number2SnapshotInfos[number]?.snapshot
    return pullSnapshotTo(shadowDirectory, createHintFor(number), previous) {
      val withRecorded = !number2SnapshotInfos.containsKey(number)
      rInterop.graphicsPullInMemorySnapshot(number, withRecorded)
    }
  }

  private fun pullStoredSnapshot(previous: RSnapshot, groupId: String, hint: String): RSnapshot? {
    return pullSnapshotTo(previous.file.parentFile, hint, previous) {
      rInterop.graphicsPullStoredSnapshot(previous.number, groupId)
    }
  }

  private fun pullSnapshotTo(directory: File, hint: String, previous: RSnapshot?, task: () -> RInterop.GraphicsPullResponse): RSnapshot? {
    return try {
      val response = task()
      RSnapshot.from(response.content, response.name, directory)?.also { snapshot ->
        response.recorded?.let { recorded ->
          snapshot.createRecordedFile(recorded)
        }
        previous?.file?.delete()
      }
    } catch (e: Error) {
      LOGGER.error("Cannot pull <$hint>", e)
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

  private data class SnapshotInfo(val snapshot: RSnapshot, val parameters: RGraphicsUtils.ScreenParameters)

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
        deleteSnapshots(snapshots, false)
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

    private fun removeSnapshotByNumber(snapshots: List<RSnapshot>, number: Int): List<RSnapshot> {
      val snapshot = snapshots.find { it.number == number }
      return if (snapshot != null) {
        if (snapshot.file.delete()) {
          snapshot.recordedFile.delete()
          snapshots.minus(snapshot)
        } else {
          snapshots
        }
      } else {
        snapshots
      }
    }

    private fun deleteSnapshots(snapshots: List<RSnapshot>, deleteRecorded: Boolean) {
      for (snapshot in snapshots) {
        if (deleteRecorded) {
          snapshot.recordedFile.delete()
        }
        snapshot.file.delete()
      }
    }

    private fun createHintFor(snapshotNumber: Int?): String {
      // Note: for error logging only. Not intended to be moved to RBundle
      return if (snapshotNumber != null) "In-Memory snapshot #${snapshotNumber}" else "Last in-memory snapshots"
    }
  }
}
