package org.jetbrains.r.run.graphics

import java.io.File

enum class RSnapshotType {
  NORMAL,
  SKETCH,
  ZOOMED,
  EXPORT,  // TODO [mine]: to be implemented
}

data class RSnapshotIdentity(
  val number: Int,
  val version: Int
)

data class RSnapshot(
  val file: File,
  val type: RSnapshotType,
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

    fun from(file: File): RSnapshot? {
      val parts = file.nameWithoutExtension.split('_')
      return if (parts.size == 4 && parts[0] == SNAPSHOT_MAGIC) {
        val type = when (parts[1]) {
          NORMAL_SUFFIX -> RSnapshotType.NORMAL
          SKETCH_SUFFIX -> RSnapshotType.SKETCH
          ZOOMED_SUFFIX -> RSnapshotType.ZOOMED
          else -> throw RuntimeException("Unsupported snapshot type: '${parts[1]}'")
        }
        val identity = RSnapshotIdentity(parts[2].toInt(), parts[3].toInt())
        RSnapshot(file, type, identity)
      } else {
        null
      }
    }
  }
}
