package org.jetbrains.r.run.graphics

data class RSnapshotsUpdate(
  val normal: List<RSnapshot>,
  val zoomed: List<RSnapshot>
) {
  companion object {
    val empty: RSnapshotsUpdate
      get() = RSnapshotsUpdate(listOf(), listOf())
  }
}

interface RGraphicsDevice {
  val lastUpdate: RSnapshotsUpdate
  var configuration: Configuration
  fun update()
  fun reset()
  fun clearSnapshot(number: Int)
  fun clearAllSnapshots()
  fun addListener(listener: (RSnapshotsUpdate) -> Unit)
  fun removeListener(listener: (RSnapshotsUpdate) -> Unit)

  data class Configuration(
    val screenParameters: RGraphicsUtils.ScreenParameters,
    val snapshotNumber: Int?
  )
}
