package org.jetbrains.r.remote.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.AppUIUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.all
import org.jetbrains.plugins.notebooks.jupyter.remote.vfs.JupyterVFileUploadFinishedEvent
import org.jetbrains.plugins.notebooks.jupyter.remote.vfs.JupyterVFileUploadQueuedEvent
import org.jetbrains.r.remote.host.RRemoteHost

class RVirtualFileUploadingListener(private val remoteHost: RRemoteHost) : BulkFileListener {
  private val activeUploads = mutableMapOf<VirtualFile, AsyncPromise<Unit>>()

  override fun after(events: MutableList<out VFileEvent>) {
    events.forEach { event ->
      val file = event.file ?: return@forEach
      if (RRemoteVFS.getHostAndPath(file)?.first != remoteHost) return@forEach
      when (event) {
        is JupyterVFileUploadQueuedEvent -> activeUploads[file] = AsyncPromise()
        is JupyterVFileUploadFinishedEvent -> activeUploads.remove(file)?.setResult(Unit)
      }
    }
  }

  fun ensureCurrentUploadsFinished(): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    AppUIUtil.invokeOnEdt {
      activeUploads.values.all().onProcessed { promise.setResult(Unit) }
    }
    return promise
  }
}