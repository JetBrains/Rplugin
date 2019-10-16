package org.jetbrains.r.run.graphics

import java.io.File

enum class RSnapshotType {
  NORMAL,
  SKETCH,
  ZOOMED,
  EXPORT,  // TODO [mine]: to be implemented
}

// Note: perhaps there will be some other options in future
enum class RSnapshotError {
  MARGIN,
}

data class RSnapshotIdentity(
  val number: Int,
  val version: Int
)

data class RSnapshot(
  val file: File,
  val type: RSnapshotType,
  val error: RSnapshotError?,
  val identity: RSnapshotIdentity
) {
  val number: Int
    get() = identity.number

  val version: Int
    get() = identity.version

  companion object {
    // Note: for goodness' sake, don't move these literals to bundle!
    private const val SNAPSHOT_MAGIC = "snapshot"
    private const val NORMAL_SUFFIX = "normal"
    private const val SKETCH_SUFFIX = "sketch"
    private const val ZOOMED_SUFFIX = "zoomed"
    private const val MARGIN_SUFFIX = "margin"

    fun from(file: File): RSnapshot? {
      fun extractType(text: String): RSnapshotType {
        return when (text) {
          NORMAL_SUFFIX -> RSnapshotType.NORMAL
          SKETCH_SUFFIX -> RSnapshotType.SKETCH
          ZOOMED_SUFFIX -> RSnapshotType.ZOOMED
          else -> throw RuntimeException("Unsupported snapshot type: '$text'")
        }
      }

      fun extractError(text: String): RSnapshotError {
        // Note: perhaps will be extended later
        return when (text) {
          MARGIN_SUFFIX -> RSnapshotError.MARGIN
          else -> throw RuntimeException("Unsupported snapshot error tag: '$text'")
        }
      }

      val parts = file.nameWithoutExtension.split('_')
      val numParts = parts.size
      return if ((numParts == 4 || numParts == 5) && parts[0] == SNAPSHOT_MAGIC) {
        val type = extractType(parts[1])
        val error = if (numParts == 5) extractError(parts[2]) else null
        val identity = parts.subList(parts.size - 2, parts.size).let {
          RSnapshotIdentity(it[0].toInt(), it[1].toInt())
        }
        RSnapshot(file, type, error, identity)
      } else {
        null
      }
    }
  }
}
