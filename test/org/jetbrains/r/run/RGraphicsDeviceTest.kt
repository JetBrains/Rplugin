/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ui.UIUtil
import junit.framework.TestCase
import org.jetbrains.r.console.UpdateGraphicsHandler
import org.jetbrains.r.run.graphics.RGraphicsState
import org.jetbrains.r.run.graphics.RGraphicsUtils
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
  private lateinit var graphicsHandler: UpdateGraphicsHandler
  private val expectedSnapshotDirectoryPath = File("testData").absolutePath

  @Volatile
  private var currentSnapshots: List<File>? = null

  private val listener = object : RGraphicsState.Listener {
    override fun onCurrentChange(snapshots: List<File>) {
      currentSnapshots = snapshots
    }

    override fun onReset() {
      // Nothing to do here
    }
  }

  override fun setUp() {
    super.setUp()
    val screenParameters = RGraphicsUtils.ScreenParameters(640, 480, null)
    val graphicsState = RGraphicsUtils.createGraphicsState(screenParameters)
    graphicsState.addListener(listener)
    graphicsHandler = UpdateGraphicsHandler(myFixture.project, rInterop, graphicsState)
    currentSnapshots = null

    // Shadow directory for snapshots produced by current implementation
    val shadowPath = Paths.get(PathManager.getSystemPath(), "SavedSnapshots")
    shadowDirectory = shadowPath.toFile()
    shadowDirectory.mkdirs()
  }

  fun testPlot() {
    initTestDataFrame()

    // Plot test data frame
    execute("plot(squares)")

    // Check this out!
    val plotSnapshot = getLastSnapshot()
    TestCase.assertNotNull(plotSnapshot)
    checkSimilar(plotSnapshot!!, 0, getCandidates(), "plot")
  }

  fun testPoints() {
    initTestDataFrame()

    // Plot test data frame
    execute("plot(squares)")
    getLastSnapshot()  // flush this plot

    // Add some points to make it a little bit interesting
    execute("points(squares $ xs, squares $ ys / 2)")

    // Check this out!
    val pointsSnapshot = getLastSnapshot()
    TestCase.assertNotNull(pointsSnapshot)
    checkSimilar(pointsSnapshot!!, 1, getCandidates(), "points")
  }

  fun testGgPlot() {
    initTestDataFrame()

    // Gg plot data frame
    execute("require(ggplot2)")
    execute("ggplot(squares, aes(x = xs, y = ys)) + geom_line()")

    // Check this out!
    val ggPlotSnapshot = getLastSnapshot()
    TestCase.assertNotNull(ggPlotSnapshot)
    checkSimilar(ggPlotSnapshot!!, 2, getCandidates(), "ggplot")
  }

  private fun getCandidates(): List<BufferedImage> {
    return listOf(
      "snapshot_plot.png",
      "snapshot_points.png",
      "snapshot_ggplot.png"
    ).map { readImage(getExpectedSnapshot(it)) }
  }

  private fun checkSimilar(actual: File, expectedIndex: Int, candidates: List<BufferedImage>, suffix: String) {
    val actualImage = readImage(actual)
    val actualIndex = actualImage.findMostSimilar(candidates)
    if (actualIndex != expectedIndex) {
      val actualShadowPath = Paths.get(shadowDirectory.absolutePath, "snapshot_${suffix}_actual.png")
      Files.copy(actual.toPath(), actualShadowPath)
      throw RuntimeException("Not similar '$suffix' plot, actual index = $actualIndex")
    }
  }

  private fun execute(command: String) {
    rInterop.executeCode(command)
    graphicsHandler.onCommandExecuted()
  }

  private fun getLastSnapshot(): File? {
    val start = System.currentTimeMillis()
    while (currentSnapshots == null) {
      if (System.currentTimeMillis() - start > TIMEOUT) {
        throw TimeoutException("Waiting for snapshot for $TIMEOUT ms")
      }
      Thread.sleep(20L)
    }
    val snapshots = currentSnapshots!!
    currentSnapshots = null
    return snapshots.lastOrNull()
  }

  private fun getExpectedSnapshot(name: String): File {
    val subdirectoryName = when {
      SystemInfo.isWindows -> "windows"
      SystemInfo.isMac -> "macos"
      else -> "linux"
    }
    val path = Paths.get(expectedSnapshotDirectoryPath, "graphics", subdirectoryName, name)
    return path.toFile()
  }

  private fun initTestDataFrame() {
    execute("squares <- data.frame(xs = 1:5, ys = (1:5)^2)")
  }


  companion object {
    private const val TIMEOUT = 5000L

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