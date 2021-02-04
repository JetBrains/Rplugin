package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.util.io.FileUtilRt
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger

object UpdateUtils {

  fun createSharedCallback(numberOfTasks: Int, callback: () -> Unit): () -> Unit {
    val numberOfTasksFinished = AtomicInteger(0)
    return {
      if (numberOfTasksFinished.incrementAndGet() == numberOfTasks) {
        callback()
      }
    }
  }

  private val sizeFormat = DecimalFormat("#.#")

  fun showSizeMb(sizeBytes: Long): String = sizeFormat.format(sizeBytes / FileUtilRt.MEGABYTE.toDouble())
}
