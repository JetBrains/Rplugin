package org.jetbrains.r.remote.filesystem

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile

class ROpenedRemoteFilesActivity : StartupActivity {
  override fun runActivity(project: Project) {
    val remoteFilesState = ROpenedRemoteFilesState.getInstance(project)
    val paths = remoteFilesState.remotePaths.toList()
    remoteFilesState.initListener()
    paths.forEach {
      val (host, path) = RRemoteVFS.getHostAndPath(it) ?: return@forEach
      RRemoteFilesystemUtil.editRemoteFile(project, host, path)
    }
  }
}

@State(name = "ROpenedRemoteFiles", storages = [Storage("rOpenedRemoteFiles.xml")])
class ROpenedRemoteFilesState(private val project: Project) : SimplePersistentStateComponent<ROpenedRemoteFilesState.State>(State()) {
  val remotePaths: MutableList<String>
    get() = state.remotePaths

  fun initListener() {
    remotePaths.clear()
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (file.fileSystem == RRemoteVFS.instance) {
          remotePaths.add(file.path)
        }
      }

      override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        if (file.fileSystem == RRemoteVFS.instance) {
          remotePaths.remove(file.path)
        }
      }
    })
  }

  class State : BaseState() {
    var remotePaths by list<String>()
  }

  companion object {
    fun getInstance(project: Project): ROpenedRemoteFilesState = project.getService(ROpenedRemoteFilesState::class.java)
  }
}