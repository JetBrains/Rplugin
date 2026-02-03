package org.jetbrains.r.run.graphics

import java.util.Stack

class RGraphicsDeviceManager {
  private val previousIds = Stack<Long>()
  private var counter = 0L

  @Volatile
  var currentDeviceId: Long? = null
    private set

  @Synchronized
  fun registerNewDevice(): Long {
    return counter.also { id ->
      currentDeviceId?.let { previousId ->
        previousIds.push(previousId)
      }
      currentDeviceId = id
      counter++
    }
  }

  @Synchronized
  fun unregisterLastDevice() {
    currentDeviceId = if (previousIds.isNotEmpty()) previousIds.pop() else null
  }
}
