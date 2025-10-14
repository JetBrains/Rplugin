package org.jetbrains.r.hints

import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val rMarkdownLanguages = listOf(RLanguage.INSTANCE, RMarkdownLanguage)

class RReturnHintEditorFactoryListener : EditorFactoryListener {
  override fun editorCreated(event: EditorFactoryEvent) {
    val editorEx = event.editor as? EditorEx ?: return
    val project = editorEx.project ?: return
    val virtualFile = FileDocumentManager.getInstance().getFile(editorEx.document)

    project.service<RReturnHintsModel>().coroutineScope.launch {
      val file = withContext(Dispatchers.IO) {
        runReadAction {
          PsiUtilCore.findFileSystemItem(project, virtualFile)
        }
      } ?: return@launch
      if (file.language in rMarkdownLanguages) {
        withContext(Dispatchers.EDT) {
          editorEx.registerLineExtensionPainter(RReturnHintLineExtensionPainter(project, editorEx.document)::getLineExtensions)
        }
      }
    }
  }

  override fun editorReleased(event: EditorFactoryEvent) {
    val project = event.editor.project ?: return
    if (project.isDisposed) return
    RReturnHintsModel.getInstance(project).clearDocumentInfo(event.editor.document)
  }
}

@Service(Service.Level.PROJECT)
class RReturnHintsModel(private val project: Project, internal val coroutineScope: CoroutineScope) {

  private val documentModels = ConcurrentHashMap<Document, DocumentReturnExtensionInfo>()
  private val hintsSetting = InlayHintsSettings.instance()

  fun getExtensionInfo(document: Document, offset: Int): List<RReturnHint>? {
    if (!hintsSetting.hintsEnabled(RReturnHintInlayProvider.settingsKey, RLanguage.INSTANCE)) {
      clearDocumentInfo(document)
    }

    return documentModels[document]?.getExtensionAtOffset(offset)
  }

  fun clearDocumentInfo(document: Document) {
    documentModels.remove(document)?.dispose()
  }

  fun activeDocuments(vararg extraDocuments: Document): List<Document> {
    return documentModels.keys.toList() + listOfNotNull(*extraDocuments)
  }

  fun update(document: Document, actualHints: Map<PsiElement, RReturnHint>) {
    if (actualHints.isEmpty()) {
      clearDocumentInfo(document)
      return
    }

    val updatedModel = runReadAction {
      val model = DocumentReturnExtensionInfo(document)
      for ((element, hint) in actualHints) {
        // hack to prevent move inlay to the next line that element contains trailing whitespaces
        val endWhitespaceLen = element.text.takeLastWhile { it.isWhitespace() }.length
        val lineNumber = document.getLineNumber(element.textRange.endOffset - endWhitespaceLen)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        model.markEndOfLine(lineEndOffset, hint)
      }
      model
    }

    documentModels.put(document, updatedModel)?.dispose()
  }

  private class DocumentReturnExtensionInfo(private val document: Document) {
    private val lineEndMarkers = ConcurrentHashMap<RangeMarker, RReturnHint>()

    fun markEndOfLine(lineEndOffset: Int, hint: RReturnHint) {
      val endLineMarker = document.createRangeMarker(lineEndOffset, lineEndOffset)
      endLineMarker.isGreedyToRight = true

      lineEndMarkers[endLineMarker] = hint
    }

    fun getExtensionAtOffset(offset: Int): List<RReturnHint> {
      // Protect operations working with the document offsets
      return runReadAction {
        val values = lineEndMarkers.entries.filter { (marker, _) ->
          val textRange = marker.range ?: return@filter false
          if (offset < textRange.startOffset || offset > textRange.endOffset) return@filter false
          if (textRange.endOffset > document.textLength) return@filter false

          val document = marker.document
          if (!document.getText(textRange).contains('\n')) {
            textRange.endOffset == offset
          } else {
            // New line may appear after session of fast typing with one or several enter hitting.
            // We can't believe startOffset too because typing session may had started with
            // typing adding several chars at the original line.
            val originalLineNumber = document.getLineNumber(textRange.startOffset)
            val currentOriginalLineEnd = document.getLineEndOffset(originalLineNumber)

            currentOriginalLineEnd == offset
          }
        }.map { it.value }
        listOfNotNull(values.firstOrNull { it is RExplicitReturnHint }, values.firstOrNull { it is RImplicitReturnHint })
      }
    }

    fun dispose() {
      val keys = lineEndMarkers.keys()
      ApplicationManager.getApplication().invokeLater {
        for (marker in keys) marker.dispose()
      }
    }
  }

  companion object {
    fun getInstance(project: Project): RReturnHintsModel =
      project.serviceOrNull() ?: error("Component 'RReturnHintsModel' is expected to be registered")
  }
}

private class RReturnHintLineExtensionPainter(private val project: Project, private val document: Document) {
  private val returnHintsModel = RReturnHintsModel.getInstance(project)

  fun getLineExtensions(lineNumber: Int): List<LineExtensionInfo> {
    val hint = getLineHint(lineNumber) ?: return emptyList()
    val textAttributes =
      EditorColorsManager.getInstance().globalScheme.getAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT)
    val result = mutableListOf<LineExtensionInfo>()
    hint.map { LineExtensionInfo(it, textAttributes) }.forEach {
      result.add(SPACE_LINE_EXTENSION_INFO)
      result.add(it)
    }

    return result
  }

  private fun getLineHint(lineNumber: Int): List<String>? {
    if (lineNumber >= document.lineCount) return null
    if (project.isDisposed()) return null
    val lineEndOffset = document.getLineEndOffset(lineNumber)
    return returnHintsModel.getExtensionInfo(document, lineEndOffset)?.map { it.hintText }
  }

  companion object {
    val SPACE_LINE_EXTENSION_INFO = LineExtensionInfo(" ", TextAttributes())
  }
}

private val RangeMarker.range: TextRange?
  get() =
    if (isValid) {
      val start = startOffset
      val end = endOffset
      if (start in 0..end) TextRange(start, end)
      else null // Probably a race condition had happened and range marker is invalidated
    }
    else null