/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ui.UIUtil
import junit.framework.TestCase
import org.jetbrains.r.console.UpdateGraphicsHandler
import org.jetbrains.r.rendering.chunk.ChunkGraphicsManager
import org.jetbrains.r.run.graphics.RGraphicsDevice
import org.jetbrains.r.run.graphics.RGraphicsRepository
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RSnapshot
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeoutException
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.math.max

class RGraphicsDeviceTest : RProcessHandlerBaseTestCase() {
  private lateinit var shadowDirectory: File
  private lateinit var graphicsDevice: RGraphicsDevice
  private lateinit var graphicsManager: ChunkGraphicsManager
  private lateinit var graphicsHandler: UpdateGraphicsHandler
  private val expectedSnapshotDirectoryPath = testDataPath

  @Volatile
  private var currentSnapshots: List<RSnapshot>? = null

  override fun setUp() {
    super.setUp()
    val screenDimension = DEFAULT_DIMENSION
    graphicsDevice = RGraphicsUtils.createGraphicsDevice(rInterop, screenDimension, null)
    RGraphicsRepository.getInstance(project).setActiveDevice(graphicsDevice)
    graphicsDevice.addListener { update ->
      currentSnapshots = update
    }
    graphicsHandler = UpdateGraphicsHandler(graphicsDevice)
    graphicsManager = ChunkGraphicsManager(project)
    currentSnapshots = null

    // Shadow directory for snapshots produced by current implementation
    val shadowPath = Paths.get(PathManager.getSystemPath(), "SavedSnapshots")
    shadowDirectory = shadowPath.toFile()
    shadowDirectory.mkdirs()
  }

  fun testPlot() {
    runAndCheckBasicSnapshot(PLOT_DRAWER)
  }

  fun testPoints() {
    runAndCheckBasicSnapshot(POINTS_DRAWER)
  }

  fun testGgPlot() {
    runAndCheckBasicSnapshot(GGPLOT_DRAWER)
  }

  fun testNonDistortingRescalePlot() {
    runAndCheckRescaleSnapshot(PLOT_DRAWER, NON_DISTORTING_DIMENSIONS)
  }

  fun testNonDistortingRescalePoints() {
    runAndCheckRescaleSnapshot(POINTS_DRAWER, NON_DISTORTING_DIMENSIONS)
  }

  fun testNonDistortingRescaleGgPlot() {
    runAndCheckRescaleSnapshot(GGPLOT_DRAWER, NON_DISTORTING_DIMENSIONS)
  }

  fun testDistortingRescalePlot() {
    runAndCheckRescaleSnapshot(PLOT_DRAWER, DISTORTING_DIMENSIONS)
  }

  fun testDistortingRescalePoints() {
    runAndCheckRescaleSnapshot(POINTS_DRAWER, DISTORTING_DIMENSIONS)
  }

  fun testDistortingRescaleGgPlot() {
    runAndCheckRescaleSnapshot(GGPLOT_DRAWER, DISTORTING_DIMENSIONS)
  }

  fun testCaptureTwoPlots() {
    val command = "require(ggplot2); plot(squares); ggplot(squares, aes(x = xs, y = ys)) + geom_line()"
    val expectedIndices = listOf(0, 2)
    val snapshots = runAndGetAllSnapshots(listOf(command))
    val candidates = getBasicCandidates()
    TestCase.assertEquals(snapshots.size, expectedIndices.size)
    for ((snapshot, expectedIndex) in snapshots.zip(expectedIndices)) {
      checkSimilar(snapshot, expectedIndex, candidates, "two")
    }
  }

  fun testRescaleGroupPlot() {
    runAndCheckRescaleGroup(PLOT_DRAWER, NON_DISTORTING_DIMENSIONS)
  }

  fun testRescaleGroupPoints() {
    runAndCheckRescaleGroup(POINTS_DRAWER, NON_DISTORTING_DIMENSIONS)
  }

  fun testRescaleGroupGgPlot() {
    runAndCheckRescaleGroup(GGPLOT_DRAWER, DISTORTING_DIMENSIONS)
  }

  private fun runAndGetSnapshot(commands: List<String>): RSnapshot {
    return runAndGetAllSnapshots(commands).last()
  }

  private fun runAndGetAllSnapshots(commands: List<String>): List<RSnapshot> {
    initTestDataFrame()
    TestCase.assertTrue(commands.isNotEmpty())
    return mutableListOf<RSnapshot>().also {
      for (command in commands) {
        execute(command)
        it.addAll(getAllSnapshots())
      }
    }
  }

  private fun rescaleAndGetSnapshot(number: Int, dimension: Dimension): RSnapshot {
    graphicsDevice.apply {
      val parameters = configuration.screenParameters
      val newParameters = parameters.copy(dimension = dimension)
      configuration = configuration.copy(screenParameters = newParameters, snapshotNumber = number)
    }
    return getLastSnapshot()
  }

  private fun runAndCheckBasicSnapshot(drawerInfo: DrawerInfo) {
    val snapshot = runAndGetSnapshot(drawerInfo.commands)
    checkSimilar(snapshot, drawerInfo.expectedIndex, getBasicCandidates(), drawerInfo.name)
  }

  private fun runAndCheckRescaleSnapshot(drawerInfo: DrawerInfo, dimensions: List<Dimension>) {
    val originalSnapshot = runAndGetSnapshot(drawerInfo.commands)
    for (dimension in dimensions) {
      val snapshot = rescaleAndGetSnapshot(originalSnapshot.number, dimension)
      val suffix = "${drawerInfo.name}_${dimension.width}_${dimension.height}"
      checkSimilar(snapshot, drawerInfo.expectedIndex, getRescaleCandidates(dimension), suffix)
    }
  }

  private fun runAndCheckRescaleGroup(drawerInfo: DrawerInfo, dimensions: List<Dimension>) {
    val originalSnapshot = runAndGetSnapshot(drawerInfo.commands)
    val (copy, group) = createImageGroup(originalSnapshot)
    try {
      for (dimension in dimensions) {
        rescaleImage(copy, dimension)
        val snapshot = getLastSnapshot()
        val suffix = "${drawerInfo.name}_${dimension.width}_${dimension.height}_stored"
        checkSimilar(snapshot, drawerInfo.expectedIndex, getRescaleCandidates(dimension), suffix)
        snapshot.file.delete()
      }
    } finally {
      group.dispose()
    }
  }

  private fun createImageGroup(snapshot: RSnapshot): Pair<File, Disposable> {
    val result = graphicsManager.createImageGroup(snapshot.file.absolutePath)
    TestCase.assertNotNull(result)
    return result!!
  }

  private fun rescaleImage(file: File, dimension: Dimension) {
    graphicsManager.rescaleImage(file.absolutePath, dimension) { rescaled ->
      val snapshot = RSnapshot.from(rescaled)
      TestCase.assertNotNull(snapshot)
      currentSnapshots = listOf(snapshot!!)
    }
  }

  private fun getBasicCandidates(): List<BufferedImage> {
    return listOf(
      "snapshot_plot.png",
      "snapshot_points.png",
      "snapshot_ggplot.png"
    ).map { readImage(getExpectedBasicSnapshot(it)) }
  }

  private fun getRescaleCandidates(dimension: Dimension): List<BufferedImage> {
    return listOf("plot", "points", "ggplot").map { readImage(getExpectedRescaleSnapshot(it, dimension)) }
  }

  private fun checkSimilar(actual: RSnapshot, expectedIndex: Int, candidates: List<BufferedImage>, suffix: String) {
    val actualImage = readImage(actual.file)
    val actualIndex = actualImage.findMostSimilar(candidates)
    if (actualIndex != expectedIndex) {
      val actualShadowPath = Paths.get(shadowDirectory.absolutePath, "snapshot_${suffix}_actual.png")
      Files.copy(actual.file.toPath(), actualShadowPath)
      throw RuntimeException("Not similar '$suffix' plot, actual index = $actualIndex")
    }
  }

  private fun execute(command: String) {
    rInterop.executeCode(command)
    graphicsHandler.onCommandExecuted()
  }

  private fun getLastSnapshot(): RSnapshot {
    return getAllSnapshots().last()
  }

  private fun getAllSnapshots(): List<RSnapshot> {
    val start = System.currentTimeMillis()
    while (currentSnapshots == null) {
      if (System.currentTimeMillis() - start > TIMEOUT) {
        throw TimeoutException("Waiting for snapshot for $TIMEOUT ms")
      }
      Thread.sleep(20L)
    }
    val snapshots = currentSnapshots!!
    currentSnapshots = null
    return snapshots
  }

  private fun getExpectedBasicSnapshot(name: String): File {
    val osName = when {
      SystemInfo.isWindows -> "windows"
      SystemInfo.isMac -> "macos"
      else -> "linux"
    }
    return getExpectedSnapshot("basic", osName, name)
  }

  private fun getExpectedRescaleSnapshot(name: String, dimension: Dimension): File {
    val osName = if (SystemInfo.isWindows) "windows" else "unix"
    val fullName = "reference_${name}_${dimension.width}_${dimension.height}_NA.png"
    return getExpectedSnapshot("rescale", osName, fullName)
  }

  private fun getExpectedSnapshot(subdirectoryName: String, osName: String, name: String): File {
    return Paths.get(expectedSnapshotDirectoryPath, "graphics", subdirectoryName, osName, name).toFile()
  }

  private fun initTestDataFrame() {
    execute("squares <- data.frame(xs = 1:5, ys = (1:5)^2)")
  }

  data class DrawerInfo(
    val name: String,
    val expectedIndex: Int,
    val commands: List<String>
  )

  companion object {
    private const val TIMEOUT = 5000L

    private val DEFAULT_DIMENSION = Dimension(640, 480)
    private val DISTORTING_DIMENSIONS = listOf(
      Dimension(640, 160),
      Dimension(640, 180),
      Dimension(640, 200)
    )
    private val NON_DISTORTING_DIMENSIONS = listOf(
      Dimension(640, 960),
      Dimension(1280, 480)
    )

    private val PLOT_DRAWER = DrawerInfo("plot", 0, listOf("plot(squares)"))
    private val POINTS_DRAWER = DrawerInfo("points", 1, listOf("plot(squares)", "points(squares $ xs, squares $ ys / 2)"))
    private val GGPLOT_DRAWER = DrawerInfo("ggplot", 2, listOf("require(ggplot2); ggplot(squares, aes(x = xs, y = ys)) + geom_line()"))

    private fun readImage(file: File): BufferedImage {
      fun BufferedImage.toGrayScale(): BufferedImage {
        return UIUtil.createImage(this.width, this.height, BufferedImage.TYPE_BYTE_GRAY).also {
          val graphics = it.graphics
          graphics.drawImage(this, 0, 0, null)
          graphics.dispose()
        }
      }

      return ImageIO.read(file).toGrayScale()
    }

    private fun BufferedImage.findMostSimilar(candidates: List<BufferedImage>): Int {
      fun calculatePixelWiseDistance(bufferA: DataBuffer, bufferB: DataBuffer): Double {
        data class Pixel(val a: Int, val r: Int, val g: Int, val b: Int)

        fun Int.toPixel(): Pixel {
          val a = (this ushr 24) and 0xff
          val r = (this ushr 16) and 0xff
          val g = (this ushr 8) and 0xff
          val b = this and 0xff
          return Pixel(a, r, g, b)
        }

        fun DataBuffer.findBrightestPixel(): Int {
          val size = this.size
          var brightest = 0
          for (i in 0 until size) {
            val current = this.getElem(i)
            brightest = max(brightest, current)
          }
          return brightest
        }

        fun calculateDifference(pixelA: Pixel, pixelB: Pixel, ratioA: Double, ratioB: Double): Double {
          val da = (pixelA.a - pixelB.a).absoluteValue.toDouble()
          val dr = (pixelA.r * ratioA - pixelB.r * ratioB).absoluteValue
          val dg = (pixelA.g * ratioA - pixelB.g * ratioB).absoluteValue
          val db = (pixelA.b * ratioA - pixelB.b * ratioB).absoluteValue
          return da + dr + dg + db
        }

        val ratioA = 255.0 / bufferA.findBrightestPixel()
        val ratioB = 255.0 / bufferB.findBrightestPixel()

        // Calculate total difference
        var totalDifference = 0.0
        for (i in 0 until bufferA.size) {
          val pixelA = bufferA.getElem(i).toPixel()
          val pixelB = bufferB.getElem(i).toPixel()
          totalDifference += calculateDifference(pixelA, pixelB, ratioA, ratioB)
        }

        return totalDifference / bufferA.size
      }

      var bestIndex = -1
      var bestDistance = Double.POSITIVE_INFINITY
      val actualBuffer = this.data.dataBuffer
      for ((index, candidate) in candidates.withIndex()) {
        val expectedBuffer = candidate.data.dataBuffer
        if (actualBuffer.size == expectedBuffer.size) {
          val distance = calculatePixelWiseDistance(actualBuffer, expectedBuffer)
          if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
          }
        }
      }
      return bestIndex
    }
  }
}