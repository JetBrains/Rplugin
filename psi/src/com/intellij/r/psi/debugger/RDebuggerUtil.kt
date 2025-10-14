package com.intellij.r.psi.debugger

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.r.psi.RBundle
import com.intellij.util.DocumentUtil

object RDebuggerUtilPsi {
  fun navigateAndCheckSourceChanges(project: Project, pair: Pair<RSourcePosition, String?>?) {
    runWriteAction {  }
    val (position, lineInR) = pair ?: return
    position.xSourcePosition.createNavigatable(project).navigate(true)
    if (lineInR == null) return
    val lineInFile = FileDocumentManager.getInstance().getDocument(position.file)?.let { document ->
      try {
        document.getText(DocumentUtil.getLineTextRange(document, position.line))
      } catch (e: IndexOutOfBoundsException) {
        null
      }
    }
    if (lineInFile == null || !StringUtil.equalsIgnoreWhitespaces(lineInFile, lineInR)) {
      val editor = (FileEditorManager.getInstance(project).getSelectedEditor(position.file) as? TextEditor)?.editor
      if (editor != null) {
        HintManager.getInstance().showInformationHint(editor, RBundle.message("debugger.file.has.changed.notification"))
      }
    }
  }
}