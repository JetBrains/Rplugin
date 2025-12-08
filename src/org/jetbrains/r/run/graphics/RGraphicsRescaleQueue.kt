package org.jetbrains.r.run.graphics

import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.rinterop.RInteropImpl
import com.intellij.r.psi.run.graphics.RGraphicsUtils
import java.util.*

class RGraphicsRescaleQueue(private val deviceId: Long, private val interop: RInteropImpl) {
  private var isTaskRunning = false
  private val queue = ArrayDeque<TaskWrapper>()

  @Synchronized
  fun submit(snapshotNumber: Int?, parameters: RGraphicsUtils.ScreenParameters?, task: () -> Promise<Unit>): Promise<Unit> {
    if (isTaskRunning) {
      // Schedule
      for (wrapper in queue) {
        if (wrapper.snapshotNumber == snapshotNumber && wrapper.parameters == parameters) {
          // The same task has already been scheduled
          return wrapper.promise
        }
      }
      val wrapper = TaskWrapper(snapshotNumber, parameters, task)
      queue.add(wrapper)
      return wrapper.promise
    } else {
      // Execute right now
      val wrapper = TaskWrapper(snapshotNumber, parameters, task)
      return execute(wrapper)
    }
  }

  @Synchronized
  private fun executeNextOrStop() {
    if (queue.isNotEmpty()) {
      execute(queue.pollFirst())
    } else {
      isTaskRunning = false
    }
  }

  private fun execute(wrapper: TaskWrapper): Promise<Unit> {
    isTaskRunning = true
    val promise = launchOrIgnore(wrapper.task)
    promise.processed(wrapper.promise)
    return promise.onProcessed {
      executeNextOrStop()
    }
  }

  private fun launchOrIgnore(task: () -> Promise<Unit>): Promise<Unit> {
    return if (interop.graphicsDeviceManager.currentDeviceId == deviceId) task() else resolvedPromise()  // Ignore
  }

  private data class TaskWrapper(
    val snapshotNumber: Int?,
    val parameters: RGraphicsUtils.ScreenParameters?,
    val task: () -> Promise<Unit>,
    val promise: AsyncPromise<Unit> = AsyncPromise()
  )
}
