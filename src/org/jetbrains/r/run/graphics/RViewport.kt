package org.jetbrains.r.run.graphics

sealed class RViewport {
  /**
   * Might be (-1) if there is no parent at all
   */
  abstract val parentIndex: Int

  data class Fixed(val ratio: Double, val delta: Double, override val parentIndex: Int) : RViewport()

  data class Free(val from: RAffinePoint, val to: RAffinePoint, override val parentIndex: Int) : RViewport()
}
