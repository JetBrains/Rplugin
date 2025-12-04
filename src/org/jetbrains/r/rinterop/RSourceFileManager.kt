/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.r.psi.debugger.RSourcePosition
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.r.psi.rinterop.RRef
import com.intellij.r.psi.rinterop.RReference
import com.intellij.r.psi.rinterop.RSourceFileManager
import com.intellij.r.psi.util.thenCancellable
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.r.run.debug.RLineBreakpointType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class RSourceFileManager(private val rInterop: RInterop): Disposable {
  private val files = ConcurrentHashMap<String, VirtualFile>()
  private val fileToId = ConcurrentHashMap<VirtualFile, String>()
  private val cachedFunctionPositions by rInterop.cached { ConcurrentHashMap<RRef, Optional<Pair<RSourcePosition, String?>>>() }

  init {
    Disposer.register(rInterop, this)
  }

  fun getFileId(file: VirtualFile): String {
    fileToId[file]?.let { return it }
    val fileId = rInterop.interpreter.getFilePathAtHost(file)?.let { R_LOCAL_PREFIX + it }
                 ?: (IDE_PREFIX + file.url)
    fileToId[file] = fileId
    files[fileId] = file
    return fileId
  }

  fun getFileById(fileId: String): VirtualFile? {
    if (fileId.isEmpty()) return null
    files[fileId]?.let { return it }
    if (fileId.startsWith(IDE_PREFIX)) {
      VirtualFileManager.getInstance().findFileByUrl(fileId.substring(IDE_PREFIX.length))?.let {
        fileToId[it] = fileId
        files[fileId] = it
        return it
      }
    } else if (fileId.startsWith(R_LOCAL_PREFIX)) {
      rInterop.interpreter.findFileByPathAtHost(fileId.substring(R_LOCAL_PREFIX.length))?.let {
        fileToId[it] = fileId
        files[fileId] = it
        return it
      }
    }
    val text = rInterop.getSourceFileText(fileId)
    if (text.isEmpty()) return null
    val name = rInterop.getSourceFileName(fileId)
      .takeIf { it.isNotEmpty() } ?: "tmp"
    val filesystem = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as RSourceFileManager.MyVirtualFileSystem
    val file = filesystem.createFile(name, text)
    fileToId[file] = fileId
    files[fileId] = file
    Disposer.register(this, Disposable {
      filesystem.removeFile(file.path)
      runInEdt {
        FileEditorManager.getInstance(rInterop.project).closeFile(file)
        runWriteAction {
          val breakpointManager = XDebuggerManager.getInstance(rInterop.project).breakpointManager
          val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
          breakpointManager.getBreakpoints(breakpointType)
            .filter { it.fileUrl == file.url }
            .forEach { breakpointManager.removeBreakpoint(it) }
        }
      }
    })
    return file
  }

  fun getFunctionPosition(rRef: RReference): CancellablePromise<Pair<RSourcePosition, String?>?> {
    val map = cachedFunctionPositions
    return rInterop.getFunctionSourcePosition(rRef.proto).thenCancellable { response ->
      map.getOrPut(rRef.proto) {
        getFileById(response.position.fileId)
          ?.let { file -> RSourcePosition(file, response.position.line) to response.sourcePositionText.takeIf { it.isNotEmpty() }}
          .let { Optional.ofNullable(it) }
      }.orElse(null)
    }
  }

  @TestOnly
  fun createFileForTest(name: String, text: String): VirtualFile {
    return (VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as RSourceFileManager.MyVirtualFileSystem).createFile(name, text)
  }

  override fun dispose() {
  }

  companion object {
    private const val IDE_PREFIX = "ide:"
    private const val R_LOCAL_PREFIX = "rlocal:"
    private const val PROTOCOL = "rwrapper"

    fun isTemporary(file: VirtualFile) = file.fileSystem == VirtualFileManager.getInstance().getFileSystem(PROTOCOL)

    fun isInvalid(url: String): Boolean {
      if (VirtualFileManager.extractProtocol(url) == PROTOCOL) {
        return VirtualFileManager.getInstance().getFileSystem(PROTOCOL).findFileByPath(VirtualFileManager.extractPath(url)) == null
      }
      return false
    }
  }
}