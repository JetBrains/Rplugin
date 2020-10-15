/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.*
import org.jetbrains.r.rinterop.RIExecutionResult
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.settings.RGraphicsSettings
import java.io.File

class RGraphicsDevice(
  private val rInterop: RInterop,
  private val shadowDirectory: File,
  initialParameters: RGraphicsUtils.ScreenParameters,
  private val inMemory: Boolean
) {

  private var lastOutputs = emptyList<RGraphicsOutput>()
  private var deviceDirectory: String? = null

  private val directory2GroupPromises = mutableMapOf<String, Promise<DeviceGroup>>()
  private val number2OutputInfos = mutableMapOf<Int, OutputInfo>()
  private val listeners = mutableListOf<(RGraphicsUpdate) -> Unit>()
  private val devicePromise = AsyncPromise<Unit>()

  private val deviceId = rInterop.graphicsDeviceManager.registerNewDevice()
  private val queue = RGraphicsRescaleQueue(deviceId, rInterop)

  val project: Project
    get() = rInterop.project

  @Volatile
  var lastUpdate: RGraphicsUpdate = RGraphicsCompletedUpdate(lastOutputs)
    private set

  var configuration: Configuration = Configuration(initialParameters, null)
    set(value) {
      field = value
      value.snapshotNumber?.let { number ->
        if (!RGraphicsSettings.isStandalone(rInterop.project)) {
          number2OutputInfos[number]?.let { info ->
            if (info.output.snapshot == null || info.parameters != value.screenParameters) {
              rescaleInMemoryAsync(number, value.screenParameters)
            }
          }
        }
      }
    }

  init {
    try {
      val parameters = RGraphicsUtils.createParameters(initialParameters)
      val result = rInterop.graphicsInit(parameters, inMemory)
      val stderr = result.stderr
      if (stderr.isNotBlank()) {
        devicePromise.setError(stderr)
        LOGGER.error(stderr)
      } else {
        deviceDirectory = extractGroupIdFrom(result.stdout)
        devicePromise.setResult(Unit)
      }
    } catch (e: Exception) {
      devicePromise.setError(e)
      LOGGER.error(e)
    }
  }

  fun update() {
    dumpLastAsync()
  }

  fun reset() {
    // Nothing to do here
  }

  fun setParametersAsync(parameters: RGraphicsUtils.ScreenParameters) {
    rInterop.graphicsSetParametersAsync(parameters)
  }

  fun clearOutput(number: Int) {
    lastOutputs = removeOutputByNumber(lastOutputs, number)
    number2OutputInfos.remove(number)
    postCompletedUpdate()
  }

  fun clearAllOutputs() {
    deleteOutputs(lastOutputs)
    lastOutputs = listOf()
    number2OutputInfos.clear()
    postCompletedUpdate()
  }

  fun dumpAndShutdownAsync(): Promise<Unit> {
    return dumpLastAsync().then<Unit> {  // Note: explicit cast `RIExecutionResult` -> `Unit`
      rInterop.graphicsShutdown()
    }.onProcessed {
      rInterop.graphicsDeviceManager.unregisterLastDevice()
    }
  }

  fun addListener(listener: (RGraphicsUpdate) -> Unit) {
    listeners.add(listener)
    listener(lastUpdate)
  }

  fun removeListener(listener: (RGraphicsUpdate) -> Unit) {
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

  fun rescaleStoredAsync(snapshot: RSnapshot, parameters: RGraphicsUtils.ScreenParameters): Promise<RSnapshot?> {
    return createDeviceGroupIfNeededAsync(snapshot.file.parentFile).thenAsync { group ->
      val hint = createHintForStored(snapshot.number)
      val promise = executeWithLogAsync(hint) {
        pushStoredSnapshotIfNeeded(snapshot, group)
        val result = rInterop.graphicsRescaleStored(group.id, snapshot.number, snapshot.version, parameters)
        parseExecutionResult(result, hint)
      }
      promise.then { isRescaled ->
        if (isRescaled) pullStoredSnapshot(snapshot, group.id) else null
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
      rInterop.interpreter.uploadFileToHost(snapshot.recordedFile, group.id)
    }
  }

  private fun rescaleInMemoryAsync(snapshotNumber: Int, parameters: RGraphicsUtils.ScreenParameters): Promise<Unit> {
    return queue.submit(snapshotNumber, parameters) {
      val hint = createHintForInMemory(snapshotNumber)
      val promise = executeWithLogAsync(hint) {
        val result = rInterop.graphicsRescale(snapshotNumber, parameters)
        parseExecutionResult(result, hint)
      }
      promise.thenIfTrue {
        pullInMemorySnapshot(snapshotNumber)?.let { pulled ->
          onNewSnapshot(pulled, parameters, snapshotNumber)
        }
      }
    }
  }

  private fun dumpLastAsync(): Promise<Unit> {
    return queue.submit(null, null) {
      val promise = executeWithLogAsync(createHintForInMemory(null)) {
        rInterop.graphicsDump()
      }
      promise.then { number2Parameters ->
        if (number2Parameters.isEmpty()) {
          return@then
        }
        val pulled = pullInMemoryOutputs(number2Parameters)
        if (pulled.isNotEmpty()) {
          onNewOutputs(pulled, null, null)
        }
      }
    }
  }

  private fun <R> executeWithLogAsync(hint: String, task: () -> R): Promise<R> {
    if (!rInterop.isAlive) {
      // Note: `rejectedPromise()` will spam the error log
      return cancelledPromise()
    }
    return executeAsync(task).onError { e ->
      LOGGER.error("Cannot execute task for <$hint>", e)
    }
  }

  private fun <R> executeAsync(task: () -> R): Promise<R> {
    return devicePromise.thenAsync {
      runAsync(task)
    }
  }

  private fun parseExecutionResult(result: RIExecutionResult, hint: String): Boolean {
    if (result.stderr.isNotBlank()) {
      // Note: This might be due to large margins and therefore shouldn't be treated as a fatal error
      LOGGER.warn("Task for <$hint> has failed:\n${result.stderr}")
    }
    if (result.stdout.isNotBlank()) {
      val output = result.stdout.let { it.substring(4, it.length - 1) }
      return output == "TRUE"
    } else {
      if (result.stderr.isNotBlank()) {
        throw RuntimeException("Cannot get stdout from graphics device. Stderr was:\n${result.stderr}")
      } else {
        throw RuntimeException("Cannot get any output from graphics device")
      }
    }
  }

  private fun onNewSnapshot(snapshot: RSnapshot, parameters: RGraphicsUtils.ScreenParameters, tracedSnapshotNumber: Int) {
    number2OutputInfos[snapshot.number]?.let { info ->  // Note: must always be not null
      val output = info.output.copy(snapshot = snapshot)
      onNewOutputs(listOf(output), parameters, tracedSnapshotNumber)
    }
  }

  private fun onNewOutputs(outputs: List<RGraphicsOutput>, parameters: RGraphicsUtils.ScreenParameters?, tracedSnapshotNumber: Int?) {
    for (output in outputs) {
      val outputInfo = OutputInfo(output, parameters)
      number2OutputInfos[output.number] = outputInfo
    }
    lastOutputs = collectLatestOutputs()
    postSnapshotNumber(tracedSnapshotNumber)
    postCompletedUpdate()
  }

  private fun collectLatestOutputs(): List<RGraphicsOutput> {
    return number2OutputInfos.values.map { it.output }.sortedBy { it.number }
  }

  private fun pullInMemoryOutputs(number2Parameters: Map<Int, RGraphicsUtils.ScreenParameters>): List<RGraphicsOutput> {
    return try {
      number2Parameters.asIterable().withIndex().mapNotNull { (index, entry) ->
        postLoadingUpdate(index, number2Parameters.size)
        val number = entry.key
        val recordedFileName = RSnapshot.createRecordedFileName(number)
        deviceDirectory?.let { remoteDirectory ->
          pullFile(recordedFileName, remoteDirectory, shadowDirectory)?.let {
            val fetched = rInterop.graphicsFetchPlot(number)
            if (!inMemory) {
              RPlotUtil.writeTo(shadowDirectory, fetched, number)
              pullInMemorySnapshot(number)
              null
            } else {
              val plot = RPlotUtil.convert(fetched, number)
              RGraphicsOutput(number, null, plot)
            }
          }
        }
      }
    } catch (e: Exception) {
      LOGGER.error("Cannot pull last in-memory outputs", e)
      emptyList()
    }
  }

  private fun pullInMemorySnapshot(number: Int): RSnapshot? {
    val previous = number2OutputInfos[number]?.output?.snapshot
    return pullSnapshotTo(shadowDirectory, number, previous, groupId = null)
  }

  private fun pullStoredSnapshot(previous: RSnapshot, groupId: String): RSnapshot? {
    return pullSnapshotTo(previous.file.parentFile, previous.number, previous, groupId)
  }

  private fun pullSnapshotTo(directory: File, number: Int, previous: RSnapshot?, groupId: String?): RSnapshot? {
    return try {
      rInterop.graphicsGetSnapshotPath(number, groupId)?.let { response ->
        /*
         * Note: one of the most important questions of the graphics machinery
         * is how to keep snapshot groups (i.e. folders) consistent
         * which means they mustn't contain different versions
         * of the same snapshot at the same time.
         * The most straight forward and safe approach is to insert
         * clean up functions at every "synchronization" point
         * which will scan folder and delete old versions
         * (right after either rescale or dump).
         * However, this approach has serious disadvantages:
         *  1) It requires a big amount of additional code which is hard to maintain
         *  2) It has a linear time complexity.
         * The suggested solution (delete a snapshot right after it's pulled)
         * is not the best one for sure since it makes pull request
         * not re-entrant but it's very easy to implement
         * and also highly computationally effective
         */
        pullFile(response.name, response.directory, directory, deleteRemoteAfterPull = true)?.let { file ->
          RSnapshot.from(file)?.also { snapshot ->
            if (previous != null && previous.identity != snapshot.identity) {
              previous.file.delete()
            }
          }
        }
      }
    } catch (e: Exception) {
      val hint = if (groupId != null) createHintForStored(number) else createHintForInMemory(number)
      LOGGER.error("Cannot pull <$hint>", e)
      null
    }
  }

  private fun pullFile(name: String, remoteDirectory: String, localDirectory: File, deleteRemoteAfterPull: Boolean = false): File? {
    val localPath = "${localDirectory.absolutePath}/$name"
    val remotePath = "$remoteDirectory/$name"
    rInterop.interpreter.apply {
      downloadFileFromHost(remotePath, localPath)
      if (deleteRemoteAfterPull) {
        deleteFileOnHost(remotePath)
      }
    }
    return File(localPath).takeIf { it.exists() }
  }

  private fun postSnapshotNumber(number: Int?) {
    configuration = configuration.copy(snapshotNumber = number)
  }

  private fun postCompletedUpdate() {
    lastUpdate = RGraphicsCompletedUpdate(lastOutputs)
    notifyListenersOnUpdate()
  }

  private fun postLoadingUpdate(loadedCount: Int, totalCount: Int) {
    lastUpdate = RGraphicsLoadingUpdate(loadedCount, totalCount)
    notifyListenersOnUpdate()
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

  private data class OutputInfo(val output: RGraphicsOutput, val parameters: RGraphicsUtils.ScreenParameters?)

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

    private fun removeOutputByNumber(outputs: List<RGraphicsOutput>, number: Int): List<RGraphicsOutput> {
      val output = outputs.find { it.number == number }
      return if (output != null) {
        output.snapshot?.apply {
          recordedFile.delete()
          file.delete()
        }
        outputs.minus(output)
      } else {
        outputs
      }
    }

    private fun deleteOutputs(outputs: List<RGraphicsOutput>) {
      for (output in outputs) {
        output.snapshot?.apply {
          recordedFile.delete()
          file.delete()
        }
      }
    }

    private fun deleteSnapshots(snapshots: List<RSnapshot>) {
      for (snapshot in snapshots) {
        snapshot.file.delete()
      }
    }

    private fun createHintForInMemory(snapshotNumber: Int?): String {
      // Note: for error logging only. Not intended to be moved to RBundle
      return if (snapshotNumber != null) "In-Memory snapshot #${snapshotNumber}" else "Last in-memory snapshots"
    }

    private fun createHintForStored(snapshotNumber: Int): String {
      // Note: for error logging only. Not intended to be moved to RBundle
      return "Recorded snapshot #$snapshotNumber"
    }

    private fun Promise<Boolean>.thenIfTrue(task: () -> Unit) = then { isDone ->
      if (isDone) {
        task()
      }
    }
  }
}
