/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.google.protobuf.StringValue
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.r.RLanguage
import org.jetbrains.r.debugger.RSourcePosition
import org.jetbrains.r.run.debug.RLineBreakpointType
import org.jetbrains.r.util.thenCancellable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RSourceFileManager(private val rInterop: RInterop): Disposable {
  private val files = ConcurrentHashMap<String, VirtualFile>()
  private val fileToId = ConcurrentHashMap<VirtualFile, String>()
  private val cachedFunctionPositions by rInterop.Cached { ConcurrentHashMap<RRef, Optional<Pair<RSourcePosition, String?>>>() }

  init {
    Disposer.register(rInterop, this)
  }

  fun getFileId(file: VirtualFile): String {
    fileToId[file]?.let { return it }
    val fileId = rInterop.interpreter.getFilePathAtHost(file)?.let { R_LOCAL_PREFIX + it }
                 ?: IDE_PREFIX + file.url
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
    val text = rInterop.execute(rInterop.asyncStub::getSourceFileText, StringValue.of(fileId)).value
    if (text.isEmpty()) return null
    val name = rInterop.execute(rInterop.asyncStub::getSourceFileName, StringValue.of(fileId)).value
      .takeIf { it.isNotEmpty() } ?: "tmp"
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
    return rInterop.executeAsync(rInterop.asyncStub::getFunctionSourcePosition, rRef.proto).thenCancellable { response ->
      map.getOrPut(rRef.proto) {
        getFileById(response.position.fileId)
          ?.let { file -> RSourcePosition(file, response.position.line) to response.sourcePositionText.takeIf { it.isNotEmpty() }}
          .let { Optional.ofNullable(it) }
      }.orElse(null)
    }
  }

  override fun dispose() {
  }

  class MyVirtualFileSystem : DeprecatedVirtualFileSystem(), NonPhysicalFileSystem {
    private val files = ConcurrentHashMap<String, VirtualFile>()
    private val fileIndex = AtomicInteger(0)

    init {
      startEventPropagation()
    }

    override fun getProtocol() = PROTOCOL

    override fun findFileByPath(path: String) = files[path]

    override fun refreshAndFindFileByPath(path: String) = findFileByPath(path)

    override fun refresh(asynchronous: Boolean) {
    }

    internal fun createFile(name: String, text: String): VirtualFile {
      val file = object : ReadOnlyLightVirtualFile("${fileIndex.incrementAndGet()}/$name", RLanguage.INSTANCE, text) {
        override fun getFileSystem() = this@MyVirtualFileSystem
        override fun getName() = name
      }
      files[file.path] = file
      return file
    }

    internal fun removeFile(path: String) {
      files.remove(path)
    }
  }

  companion object {
    private const val IDE_PREFIX = "ide:"
    private const val R_LOCAL_PREFIX = "rlocal:"
    private const val PROTOCOL = "rwrapper"
    private val filesystem = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as MyVirtualFileSystem

    fun isTemporary(file: VirtualFile) = file.fileSystem == filesystem

    fun isInvalid(url: String): Boolean {
      if (VirtualFileManager.extractProtocol(url) == PROTOCOL) {
        return filesystem.findFileByPath(VirtualFileManager.extractPath(url)) == null
      }
      return false
    }
  }
}