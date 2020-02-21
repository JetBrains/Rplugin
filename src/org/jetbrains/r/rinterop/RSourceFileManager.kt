/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.google.protobuf.StringValue
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.*
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import com.jetbrains.rd.util.AtomicInteger
import com.jetbrains.rd.util.concurrentMapOf
import org.jetbrains.r.RLanguage
import org.jetbrains.r.debugger.RSourcePosition
import java.util.*

class RSourceFileManager(private val rInterop: RInterop): Disposable {
  private val files = concurrentMapOf<String, VirtualFile>()
  private val cachedFunctionPositions by rInterop.Cached { concurrentMapOf<Service.RRef, Optional<RSourcePosition>>() }

  init {
    Disposer.register(rInterop, this)
  }

  fun getFileId(file: VirtualFile): String {
    file.getUserData(FILE_ID)?.let { return it }
    val fileId = file.canonicalPath!!
    files[fileId] = file
    return fileId
  }

  fun getFileById(fileId: String): VirtualFile? {
    if (fileId.isEmpty()) return null
    files[fileId]?.let { return it }
    LocalFileSystem.getInstance().findFileByPath(fileId)?.let {
      files[fileId] = it
      return it
    }
    val text = rInterop.execute(rInterop.stub::getSourceFileText, StringValue.of(fileId)).value
    if (text.isEmpty()) return null
    val name = rInterop.execute(rInterop.stub::getSourceFileName, StringValue.of(fileId)).value
    val file = filesystem.createFile(name, text)
    file.putUserData(FILE_ID, fileId)
    files[fileId] = file
    return file
  }

  fun getFunctionPosition(rRef: RRef): RSourcePosition? {
    return cachedFunctionPositions.getOrPut(rRef.proto) {
      val position = rInterop.execute(rInterop.stub::getFunctionSourcePosition, rRef.proto)
      rInterop.sourceFileManager.getFileById(position.fileId)?.let { RSourcePosition(it, position.line) }.let { Optional.ofNullable(it) }
    }.orElse(null)
  }

  override fun dispose() {
    for (file in files.keys) {
      filesystem.removeFile(file)
    }
  }

  class MyVirtualFileSystem : DeprecatedVirtualFileSystem(), NonPhysicalFileSystem {
    private val files = concurrentMapOf<String, VirtualFile>()
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
    private val FILE_ID = Key<String>("org.jetbrains.r.rinterop.SourceFileId")
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