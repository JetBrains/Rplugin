package org.jetbrains.r.run.graphics

sealed class RViewport {
  /**
   * Might be (-1) if there is no parent at all
   */
  abstract val parentIndex: Int

  data class Fixed(val ratio: Float, val delta: Float, override val parentIndex: Int) : RViewport()

  data class Free(val from: Long, val to: Long, override val parentIndex: Int) : RViewport()
}
