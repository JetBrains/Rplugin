/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import icons.org.jetbrains.r.RBundle
import java.io.File

enum class RSnapshotType {
  NORMAL,
  SKETCH,
  ZOOMED,
}

data class RSnapshot(
  val file: File,
  val type: RSnapshotType,
  val error: String?,

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
  val version: Int
) {
  companion object {
    // Note: for goodness' sake, don't move these literals to bundle!
    private const val SNAPSHOT_MAGIC = "snapshot"
    private const val NORMAL_SUFFIX = "normal"
    private const val SKETCH_SUFFIX = "sketch"
    private const val ZOOMED_SUFFIX = "zoomed"
    private const val MARGIN_SUFFIX = "margin"

    private val MARGIN_ERROR_TEXT = RBundle.message("graphics.snapshot.error.margins")

    fun from(file: File): RSnapshot? {
      val parts = file.nameWithoutExtension.split('_')
      val numParts = parts.size
      return if ((numParts == 4 || numParts == 5) && parts[0] == SNAPSHOT_MAGIC) {
        val type = extractType(parts[1])
        val error = if (numParts == 5) extractError(parts[2]) else null
        val (number, version) = parts.subList(parts.size - 2, parts.size).let {
          Pair(it[0].toInt(), it[1].toInt())
        }
        RSnapshot(file, type, error, number, version)
      } else if (numParts == 3) {
        RSnapshot(file, RSnapshotType.NORMAL, null, parts[1].toInt(), parts[2].toInt())
      } else {
        null
      }
    }

    private fun extractType(text: String): RSnapshotType {
      return when (text) {
        NORMAL_SUFFIX -> RSnapshotType.NORMAL
        SKETCH_SUFFIX -> RSnapshotType.SKETCH
        ZOOMED_SUFFIX -> RSnapshotType.ZOOMED
        else -> throw RuntimeException("Unsupported snapshot type: '$text'")
      }
    }

    private fun extractError(text: String): String {
      return if (text == MARGIN_SUFFIX) MARGIN_ERROR_TEXT else throw RuntimeException("Unsupported snapshot error tag: '$text'")
    }
  }
}
