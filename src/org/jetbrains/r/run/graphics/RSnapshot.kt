/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import java.io.File
import java.nio.file.Paths

enum class RSnapshotType {
  NORMAL,
  SKETCH,
}

data class RSnapshot(
  val file: File,
  val type: RSnapshotType,

  /**
   * Number of the snapshot.
   * It's guaranteed that snapshots produced by different R commands
   * will get different numbers
   */
  val number: Int,

  /**
   * Version of the snapshot.
   * Due to rescaling, graphics device can produce multiple rescaled versions
   * of the snapshot with the same [number]
   */
  val version: Int,

  /**
   * This might be `null` in two cases:
   * 1) the graphics device is outdated and doesn't specify a snapshot's resolution at all
   * 2) a resolution wasn't specified explicitly before a snapshot's rescale
   */
  val resolution: Int?
) {
  val recordedFile: File
    get() = Paths.get(file.parent, "recorded_${number}.snapshot").toFile()

  companion object {
    // Note: for goodness' sake, don't move these literals to bundle!
    private const val SNAPSHOT_MAGIC = "snapshot"
    private const val NORMAL_SUFFIX = "normal"
    private const val SKETCH_SUFFIX = "sketch"

    fun from(file: File): RSnapshot? {
      val parts = file.nameWithoutExtension.split('_')
      if (parts.isEmpty() || parts[0] != SNAPSHOT_MAGIC) {
        return null
      }
      return when (val numParts = parts.size) {
        4, 5 -> {
          // format: snapshot_type_number_version[_resolution]
          val type = extractType(parts[1])
          val number = parts[2].toInt()
          val version = parts[3].toInt()
          val resolution = if (numParts == 5) parts[4].toResolutionOrNull() else null
          RSnapshot(file, type, number, version, resolution)
        }
        3 -> {
          // format: snapshot_number_version (for backward compatibility)
          RSnapshot(file, RSnapshotType.NORMAL, parts[1].toInt(), parts[2].toInt(), null)
        }
        else -> null
      }
    }

    private fun extractType(text: String): RSnapshotType {
      return when (text) {
        NORMAL_SUFFIX -> RSnapshotType.NORMAL
        SKETCH_SUFFIX -> RSnapshotType.SKETCH
        else -> throw RuntimeException("Unsupported snapshot type: '$text'")
      }
    }

    private fun String.toResolutionOrNull(): Int? {
      return toInt().takeIf { it > 0 }?.let { RGraphicsUtils.downscaleForRetina(it) }
    }
  }
}
