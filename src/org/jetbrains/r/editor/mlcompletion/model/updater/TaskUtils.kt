package org.jetbrains.r.editor.mlcompletion.model.updater

import java.util.concurrent.atomic.AtomicInteger

object TaskUtils {

  fun createSharedCallback(numberOfTasks: Int, callback: () -> Unit): () -> Unit {
    val numberOfTasksFinished = AtomicInteger(0)
    return {
      if (numberOfTasksFinished.incrementAndGet() == numberOfTasks) {
        callback()
      }
    }
  }

}
