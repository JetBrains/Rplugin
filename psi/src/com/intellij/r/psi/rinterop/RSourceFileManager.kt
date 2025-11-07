package com.intellij.r.psi.rinterop

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.r.psi.RLanguage
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RSourceFileManager {
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

    fun createFile(name: String, text: String): VirtualFile {
      val file = object : ReadOnlyLightVirtualFile("${fileIndex.incrementAndGet()}/$name", RLanguage.INSTANCE, text) {
        override fun getFileSystem() = this@MyVirtualFileSystem
        override fun getName() = name
      }
      files[file.path] = file
      return file
    }

    fun removeFile(path: String) {
      files.remove(path)
    }
  }

  companion object {
    private const val PROTOCOL = "rwrapper"
    private val filesystem = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as MyVirtualFileSystem

    fun isTemporary(file: VirtualFile) = file.fileSystem == filesystem
  }
}