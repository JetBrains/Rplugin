package org.jetbrains.r.remote.filesystem

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.remote.host.RRemoteHost

@State(name = "ROpenedRemoteFiles", storages = [Storage("rOpenedRemoteFiles.xml")])
class ROpenedRemoteFilesState(private val project: Project) : SimplePersistentStateComponent<ROpenedRemoteFilesState.State>(State()) {
  fun initialize() {
    state.openedFiles.forEach { path ->
      val host = path.split('/').firstOrNull { it.isNotEmpty() } ?: return@forEach
      state.savedFilesByHost.getOrPut(host) { mutableListOf() }.add(path)
    }
    state.openedFiles.clear()
    initListener()
  }

  private fun initListener() {
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (file.fileSystem == RRemoteVFS.instance) {
          state.openedFiles.add(file.path)
        }
      }

      override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        if (file.fileSystem == RRemoteVFS.instance) {
          state.openedFiles.remove(file.path)
        }
      }
    })
  }

  fun remoteHostAdded(remoteHost: RRemoteHost) {
    state.savedFilesByHost.remove(remoteHost.configId)?.forEach {
      val (host, path) = RRemoteVFS.getHostAndPath(it) ?: return@forEach
      RRemoteFilesystemUtil.editRemoteFile(project, host, path)
    }
  }

  fun remoteHostRemoved(remoteHost: RRemoteHost) {
    FileEditorManager.getInstance(project).allEditors
      .mapNotNull { it.file }
      .filter { RRemoteVFS.getHostAndPath(it)?.first == remoteHost }
      .forEach {
        state.savedFilesByHost.getOrPut(remoteHost.configId) { mutableListOf() }.add(it.path)
        FileEditorManager.getInstance(project).closeFile(it)
      }
  }

  class State : BaseState() {
    var openedFiles by list<String>()
    var savedFilesByHost by map<String, MutableList<String>>()
  }

  companion object {
    fun getInstance(project: Project): ROpenedRemoteFilesState = project.getService(ROpenedRemoteFilesState::class.java)
  }
}