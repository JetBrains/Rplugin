/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.concurrency.*
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

  private val directory2GroupPromises = mutableMapOf<String, Promise<DeviceGroup>>()
  private val number2SnapshotInfos = mutableMapOf<Int, SnapshotInfo>()
  private val listeners = mutableListOf<(List<RSnapshot>) -> Unit>()
  private val devicePromise = AsyncPromise<Unit>()
  private val queue = RGraphicsRescaleQueue()

  val lastUpdate: List<RSnapshot>
    get() = lastNormal

  var configuration: Configuration = Configuration(initialParameters, null)
    set(value) {
      field = value
      value.snapshotNumber?.let { number ->
        number2SnapshotInfos[number]?.parameters?.let { previousParameters ->
          val newParameters = value.screenParameters
          if (previousParameters != newParameters) {
            rescaleInMemoryAsync(number, newParameters)
          }
        }
      }
    }

  init {
    val parameters = RGraphicsUtils.createParameters(initialParameters)
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
    rescaleInMemoryAsync(null, configuration.screenParameters)
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

  fun dumpAndShutdownAsync(): Promise<Unit> {
    return dumpAllAsync().then<Unit> {  // Note: explicit cast `RIExecutionResult` -> `Unit`
      rInterop.graphicsShutdown()
    }
  }

  fun addListener(listener: (List<RSnapshot>) -> Unit) {
    listeners.add(listener)
    listener(lastUpdate)
  }

  fun removeListener(listener: (List<RSnapshot>) -> Unit) {
    listeners.remove(listener)
  }

  fun createDeviceGroupAsync(directory: File): Promise<Disposable> {
    return createDeviceGroupIfNeededAsync(directory).then { group ->
      Disposable {
        if (rInterop.isAlive) {  // Note: the group is removed automatically on interop termination
          runAsync {  // Note: prevent execution on EDT
            rInterop.graphicsRemoveGroup(group.id)
          }
        }
      }
    }
  }

  private fun createEmptyGroupAsync(): Promise<DeviceGroup> {
    if (!rInterop.isAlive) {
      return cancelledPromise()
    }
    val promise = runAsync {
      val result = rInterop.graphicsCreateGroup()
      if (result.stderr.isNotBlank()) {
        throw RuntimeException("Cannot create empty group. Stderr was:\n${result.stderr}")
      }
      val id = extractGroupIdFrom(result.stdout)
      DeviceGroup(id)
    }
    return promise.onError { e ->
      LOGGER.error("Cannot create device group", e)
    }
  }

  private fun extractGroupIdFrom(stdout: String): String {
    // "[1] 'groupId'\n"
    if (stdout.length < 8) {
      throw RuntimeException("Unexpected output from interop:\n$stdout")
    }
    return stdout.substring(5, stdout.length - 2)
  }

  private fun dumpAllAsync(): Promise<Unit> {
    val promise = executeWithLogAsync(createHintFor(null)) {
      rInterop.graphicsDump()
    }
    return promise.thenIfTrue {
      pullInMemorySnapshots()
    }
  }

  fun rescaleStoredAsync(snapshot: RSnapshot, parameters: RGraphicsUtils.ScreenParameters): Promise<RSnapshot?> {
    return createDeviceGroupIfNeededAsync(snapshot.file.parentFile).thenAsync { group ->
      val hint = "Recorded snapshot #${snapshot.number} at '${group.id}'"
      val promise = executeWithLogAsync(hint) {
        pushStoredSnapshotIfNeeded(snapshot, group)
        rInterop.graphicsRescaleStored(group.id, snapshot.number, snapshot.version, parameters)
      }
      promise.then { isRescaled ->
        if (isRescaled) pullStoredSnapshot(snapshot, group.id, hint) else null
      }
    }
  }

  @Synchronized
  private fun createDeviceGroupIfNeededAsync(directory: File): Promise<DeviceGroup> {
    return directory2GroupPromises.getOrPut(directory.absolutePath) {
      createEmptyGroupAsync()
    }
  }

  @Synchronized
  private fun pushStoredSnapshotIfNeeded(snapshot: RSnapshot, group: DeviceGroup) {
    if (group.snapshotNumbers.add(snapshot.number)) {
      val recorded = snapshot.recordedFile.readBytes()
      rInterop.graphicsPushSnapshot(group.id, snapshot.number, recorded)
    }
  }

  private fun rescaleInMemoryAsync(snapshotNumber: Int?, parameters: RGraphicsUtils.ScreenParameters): Promise<Unit> {
    return queue.submit(snapshotNumber, parameters) {
      val promise = executeWithLogAsync(createHintFor(snapshotNumber)) {
        rInterop.graphicsRescale(snapshotNumber, parameters)
      }
      promise.thenIfTrue {
        val pulled = pullInMemorySnapshots()
        if (pulled.isNotEmpty()) {
          onNewSnapshots(pulled, snapshotNumber)
        }
      }
    }
  }

  private fun executeWithLogAsync(hint: String, task: () -> RIExecutionResult): Promise<Boolean> {
    if (!rInterop.isAlive) {
      // Note: `rejectedPromise()` will spam the error log
      return cancelledPromise()
    }
    return executeAsync(hint, task).onError { e ->
      LOGGER.error("Cannot execute task for <$hint>", e)
    }
  }

  private fun executeAsync(hint: String, task: () -> RIExecutionResult): Promise<Boolean> {
    return devicePromise.thenAsync {
      runAsync {
        val result = task()
        if (result.stderr.isNotBlank()) {
          // Note: This might be due to large margins and therefore shouldn't be treated as a fatal error
          LOGGER.warn("Task for <$hint> has failed:\n${result.stderr}")
        }
        if (result.stdout.isNotBlank()) {
          val output = result.stdout.let { it.substring(4, it.length - 1) }
          return@runAsync output == "TRUE"
        } else {
          if (result.stderr.isNotBlank()) {
            throw RuntimeException("Cannot get stdout from graphics device. Stderr was:\n${result.stderr}")
          } else {
            throw RuntimeException("Cannot get any output from graphics device")
          }
        }
      }
    }
  }

  private fun onNewSnapshots(pulledSnapshots: List<RSnapshot>, tracedSnapshotNumber: Int?) {
    for (snapshot in pulledSnapshots) {
      val snapshotInfo = SnapshotInfo(snapshot, configuration.screenParameters)
      number2SnapshotInfos[snapshot.number] = snapshotInfo
    }
    lastNormal = collectLatestSnapshots()
    postSnapshotNumber(tracedSnapshotNumber)
    notifyListenersOnUpdate()
  }

  private fun collectLatestSnapshots(): List<RSnapshot> {
    return number2SnapshotInfos.values.map { it.snapshot }.sortedBy { it.number }
  }

  private fun pullInMemorySnapshots(): List<RSnapshot> {
    return try {
      rInterop.graphicsPullChangedNumbers().mapNotNull { number ->
        pullInMemorySnapshot(number)
      }
    } catch (e: Exception) {
      LOGGER.error("Cannot pull numbers of rescaled snapshots", e)
      emptyList()
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

  private fun pullSnapshotTo(directory: File, hint: String, previous: RSnapshot?, task: () -> RInterop.GraphicsPullResponse?): RSnapshot? {
    return try {
      task()?.let { response ->
        RSnapshot.from(response.content, response.name, directory)?.also { snapshot ->
          response.recorded?.let { recorded ->
            snapshot.createRecordedFile(recorded)
          }
          if (previous != null && previous.identity != snapshot.identity) {
            previous.file.delete()
          }
        }
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

  /*
   * Abstraction of (potentially remote) directory where a graphics device
   * can store its plots
   */
  private data class DeviceGroup(val id: String, val snapshotNumbers: MutableSet<Int> = mutableSetOf())

  companion object {
    private val LOGGER = Logger.getInstance(RGraphicsDevice::class.java)

    private val RSnapshot.identity: Pair<Int, Int>
      get() = Pair(number, version)

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

    private fun Promise<Boolean>.thenIfTrue(task: () -> Unit) = then { isDone ->
      if (isDone) {
        task()
      }
    }
  }
}
