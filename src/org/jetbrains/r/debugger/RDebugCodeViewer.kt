/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.application.subscribe
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage

internal class RDebugCodeViewer(val project: Project, parentDisposable: Disposable) {
  private class FileInfo(val file: CodeViewerFile) {
    var accessed = false
  }
  private var cache = mutableMapOf<Pair<String, String>, FileInfo>()

  init {
    FileEditorManagerListener.FILE_EDITOR_MANAGER.subscribe(parentDisposable, object: FileEditorManagerListener {
      override fun fileOpenedSync(source: FileEditorManager, file: VirtualFile,
                                  editors: com.intellij.openapi.util.Pair<Array<FileEditor>, Array<FileEditorProvider>>) {
        if (!isViewerFile(file)) return
        editors.first.forEach { editor ->
          val panel = EditorNotificationPanel()
          panel.setText(RBundle.message("debugger.code.viewer.notification.text"))
          FileEditorManager.getInstance(project).addTopComponent(editor, panel);
        }
      }
    })
  }

  fun calculatePosition(name: String, code: String, expr: String): XSourcePosition? {
    val fileInfo = cache.computeIfAbsent(name to code) { FileInfo(CodeViewerFile(name, code)) }
    fileInfo.accessed = true
    val file = fileInfo.file
    val position = findInCode(code, expr)
    if (position == -1) return XDebuggerUtil.getInstance().createPosition(file, 0)
    return XDebuggerUtil.getInstance().createPosition(file, StringUtil.offsetToLineNumber(code, position))
  }

  fun prepareCleanUp() {
    cache.values.forEach { it.accessed = false }
  }

  fun cleanUp() {
    val toClose = mutableListOf<VirtualFile>()
    cache = cache.filterValues {
      if (!it.accessed) {
        toClose.add(it.file)
        false
      } else {
        true
      }
    }.toMutableMap()
    ApplicationManager.getApplication().invokeLater {
      toClose.forEach { FileEditorManager.getInstance(project).closeFile(it) }
    }
  }

  private class CodeViewerFile(name: String, code: String) : ReadOnlyLightVirtualFile(name, RLanguage.INSTANCE, code)

  companion object {
    fun isViewerFile(file: VirtualFile) = file is CodeViewerFile

    private fun findInCode(code: String, expr: String): Int {
      val regex = expr.split(Regex("\\s+")).joinToString("\\s+") { Regex.escape(it) }.toRegex()
      return regex.find(code)?.range?.start ?: -1
    }
  }
}