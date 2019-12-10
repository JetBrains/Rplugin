/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*

internal class RDebugCodeGenerator(private val project: Project) {
  data class SourceRef(val file: VirtualFile, val lineOffset: Int)
  private val idToSource = mutableMapOf<String, SourceRef>()

  data class DebugSourceInfo(val code: String, val fileId: String)

  fun prepareDebugSource(file: VirtualFile, range: TextRange? = null): DebugSourceInfo {
    var lineOffset = 0
    val code = runReadAction {
      val rFile = if (range == null) {
        PsiManager.getInstance(project).findFile(file) as? RFile
      } else {
        FileDocumentManager.getInstance().getDocument(file)?.let { document ->
          lineOffset = document.getLineNumber(range.startOffset)
          val text = document.text.substring(range.startOffset, range.endOffset)
          RElementFactory.buildRFileFromText(project, text) as? RFile
        }
      }
      rFile?.let { addDebugCode(it) } ?: throw RDebuggerException(RBundle.message("debugger.failed.to.execute", file.canonicalPath.orEmpty()))
    }
    val id = "fileId${idToSource.size}"
    idToSource[id] = SourceRef(file, lineOffset)
    return DebugSourceInfo(code, id)
  }

  private fun addDebugCode(file: RFile): String? {
    val insertions = mutableListOf<Pair<Int, String>>()
    file.accept(object : RRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement?) {
        super.visitElement(element)
        if (element !is RExpression) return
        val parent = element.parent ?: return
        if (parent is RFile || parent is RBlockExpression ||
            (parent is RFunctionExpression && parent.expression == element) ||
            (parent is RIfStatement && parent.condition != element) ||
            (parent is RForStatement && parent.body == element) ||
            (parent is RWhileStatement && parent.body == element) ||
            (parent is RRepeatStatement && parent.body == element)) {
          insertions.add(element.textRange.startOffset to "\\<")
          insertions.add(element.textRange.endOffset to "\\>")
        }
      }

      override fun visitErrorElement(element: PsiErrorElement?) {
        throw RDebuggerException(RBundle.message("debugger.failed.to.execute.syntax.error", file.virtualFile.canonicalPath.orEmpty()))
      }
    })
    insertions.sortWith(Comparator.comparingInt<Pair<Int,String>> { it.first }.thenByDescending { it.second })
    val text = file.text
    insertions.add(text.length to "")
    val result = StringBuilder()
    var previousIndex = 0
    for ((index, string) in insertions) {
      result.append(text.substring(previousIndex, index).replace("\\", "\\\\")).append(string)
      previousIndex = index
    }
    return result.toString()
  }

  fun parseXSourcePosition(s: String): XSourcePosition? {
    val sharpIdx = s.lastIndexOf('#').takeIf { it > 0 } ?: return null
    val fileStr = s.substring(0, sharpIdx)
    val line = (s.substring(sharpIdx + 1, s.length).toIntOrNull() ?: return null) - 1
    val sourceRef = idToSource[fileStr] ?: SourceRef(LocalFileSystem.getInstance().findFileByPath(fileStr) ?: return null, 0)
    return XDebuggerUtil.getInstance().createPosition(sourceRef.file, sourceRef.lineOffset + line)
  }
}