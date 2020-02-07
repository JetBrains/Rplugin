/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.diff.util.IntPair
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.intellij.util.ui.UIUtil
import org.intellij.datavis.r.inlays.InlayDescriptorProvider
import org.intellij.datavis.r.inlays.InlayDimensions
import org.intellij.datavis.r.inlays.InlayElementDescriptor
import org.intellij.datavis.r.inlays.InlayOutput
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.r.rendering.chunk.RunChunkNavigator.createRunChunkActionGroup
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import org.jetbrains.r.run.graphics.RGraphicsDevice
import java.awt.*
import java.awt.event.MouseEvent
import java.io.File
import java.util.concurrent.Future
import javax.swing.ImageIcon

class ChunkDescriptorProvider : InlayDescriptorProvider {
  override fun getInlayDescriptor(editor: Editor): InlayElementDescriptor? {
    return runReadAction {
      val psiFile = PsiDocumentManager.getInstance(editor.project ?: return@runReadAction null).getPsiFile(editor.document)
       if (psiFile?.virtualFile?.fileType is RMarkdownFileType) RMarkdownInlayDescriptor(psiFile, editor) else null
    }
  }
}

class RMarkdownInlayDescriptor(override val psiFile: PsiFile, private val editor: Editor) : InlayElementDescriptor {
  private val highlighters: ArrayList<RangeHighlighter> = ArrayList()

  override fun cleanup(psi: PsiElement): Future<Void> {
    val cacheDirectory = ChunkPathManager.getCacheDirectory(psi)!!
    return FileUtil.asyncDelete(File(cacheDirectory))
  }

  override fun isInlayElement(psi: PsiElement): Boolean {
    return psi is LeafPsiElement && psi.elementType == MarkdownTokenTypes.CODE_FENCE_END &&
           (psi.prevSibling?.prevSibling?.let { it is LeafPsiElement && it.elementType === R_FENCE_ELEMENT_TYPE } == true)
  }

  override fun getInlayOutputs(psi: PsiElement): List<InlayOutput> {
    return getImages(psi) + getUrls(psi) + getTables(psi) + getOutputs(psi)
  }

  override fun onUpdateHighlighting(toolbarElements: Collection<PsiElement>) {
    val markupModel = editor.markupModel
    highlighters.forEach { markupModel.removeHighlighter(it) }
    highlighters.clear()
    editor.colorsScheme.getAttributes(RMARKDOWN_CHUNK).backgroundColor?.let { backgroundColor ->
      toolbarElements.forEach { fillChunkArea(it.parent.textRange, backgroundColor, markupModel) }
    }
  }

  override fun getToolbarActions(psi: PsiElement): ActionGroup? = if (isChunkFenceLang(psi)) createRunChunkActionGroup(psi) else null

  override fun isToolbarActionElement(psi: PsiElement): Boolean = isChunkFenceLang(psi)

  private fun fillChunkArea(textRange: TextRange, backgroundColor: Color, markupModel: MarkupModel) {
    highlighters.add(markupModel.addRangeHighlighter(textRange.startOffset,
                                                     textRange.endOffset,
                                                     HighlighterLayer.ADDITIONAL_SYNTAX + 1,
                                                     TextAttributes(null,
                                                                    backgroundColor,
                                                                    null,
                                                                    EffectType.ROUNDED_BOX,
                                                                    Font.PLAIN),
                                                     HighlighterTargetArea.LINES_IN_RANGE))
  }

  companion object {
    fun getImages(psi: PsiElement): List<InlayOutput> {
      val inlays = ChunkPathManager.getImagesDirectory(psi)?.let { imagesDirectory ->
        RGraphicsDevice.fetchLatestNormalSnapshots(File(imagesDirectory))?.map { snapshot ->
          val bytes = FileUtil.loadFileBytes(snapshot.file)
          val imageIcon = ImageIcon(bytes, "preview")
          val preview = IconUtil.scale(imageIcon, null, InlayDimensions.lineHeight * 4.0f / imageIcon.iconHeight)
          val text = snapshot.file.absolutePath
          InlayOutput(text, "IMG", preview = preview)
        }
      }
      return inlays ?: emptyList()
    }

    private val preferredWidth
      get() = (InlayDimensions.lineHeight * 8.0f).toInt()

    fun getUrls(psi: PsiElement): List<InlayOutput> {
      val imagesDirectory = ChunkPathManager.getHtmlDirectory(psi) ?: return emptyList()
      return getFilesByExtension(imagesDirectory, ".html")?.map { html ->
        InlayOutput("file://" + html.absolutePath.toString(),
                    "URL",
                    title = "HTML",
                    preferredWidth = preferredWidth)
      } ?: emptyList()
    }

    fun getTables(psi: PsiElement): List<InlayOutput> {
      val dataDirectory = ChunkPathManager.getDataDirectory(psi) ?: return emptyList()
      return getFilesByExtension(dataDirectory, ".csv")?.map { csv ->
        InlayOutput(csv.readText(),
                    "TABLE",
                    title = "Table",
                    preferredWidth = preferredWidth)
      } ?: emptyList()
    }

    fun getOutputs(psi: PsiElement): List<InlayOutput> {
      return ChunkPathManager.getOutputFile(psi)?.let { File(it) }?.takeIf { it.exists() }?.let {
        listOf(InlayOutput(it.absolutePath,
                           "Output",
                           title = "R Console",
                           preferredWidth = preferredWidth))
      } ?: emptyList()
    }

    private fun getFilesByExtension(imagesDirectory: String, extension: String): Array<File>? =
      File(imagesDirectory).takeIf { it.exists() }?.listFiles { _, name ->
        name.endsWith(extension)
      }?.apply { sortBy { it.lastModified() } }
  }
}

private object ChunkProgressRenderer : LineMarkerRenderer {
  private fun paint(editor: Editor, g: Graphics) {
    val chunkExecutionState = editor.chunkExecutionState ?: return
    val clipBounds = g.clipBounds

    val visibleLineStart = editor.xyToLogicalPosition(Point(0, clipBounds.y)).line
    val visibleLineEnd = editor.xyToLogicalPosition(Point(0, clipBounds.y + clipBounds.height)).line
    val visualLineRange = IntRange(visibleLineStart, visibleLineEnd)

    chunkExecutionState.pendingLineRanges.mapNotNull { it.intersect(visualLineRange) }.forEach {
      paintIntRange((g as Graphics2D), editor, it, Color(180, 250 , 180))
    }
    chunkExecutionState.currentLineRange?.intersect(visualLineRange)?.let {
      paintIntRange((g as Graphics2D), editor, it, Color(100, 240, 100))
    }
  }

  private fun paintIntRange(g: Graphics2D,
                            editor: Editor,
                            block: IntRange,
                            gutterColor: Color) {
    val editorImpl = editor as EditorImpl
    val area = getGutterArea(editor)
    val x = area.val1
    val endX = area.val2
    val start = editorImpl.visualLineToY(block.first + 1)
    val end = editorImpl.visualLineToY(block.last)
    paintRect(g, gutterColor, null, x, start, endX, end)
  }

  override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
    paint(editor, g)
  }

  private fun isInPendingLineRange(line: Int, chunkExecutionState: ChunkExecutionState): Boolean {
    val pendingLineRanges = chunkExecutionState.pendingLineRanges
    val index = pendingLineRanges.binarySearch {
      when {
        it.first > line -> 1
        it.last < line -> -1
        else -> 0
      }
    }
    return (index >= 0 && index <= pendingLineRanges.size && pendingLineRanges[index].contains(line))
  }

  private fun isInCurrentLineRange(line: Int, chunkExecutionState: ChunkExecutionState) =
    chunkExecutionState.currentLineRange?.contains(line) == true

  private fun getGutterArea(editor: Editor): IntPair {
    val gutter = (editor as EditorEx).gutterComponentEx
    val x = gutter.lineMarkerFreePaintersAreaOffset + 1 // leave 1px for brace highlighters
    val endX = gutter.whitespaceSeparatorOffset
    return IntPair(x, endX)
  }

  fun isInsideMarkerArea(e: MouseEvent): Boolean {
    val gutter = e.component as EditorGutterComponentEx
    return e.x > gutter.lineMarkerFreePaintersAreaOffset
  }

  private fun paintRect(g: Graphics2D,
                        color: Color?,
                        borderColor: Color?,
                        x1: Int,
                        y1: Int,
                        x2: Int,
                        y2: Int) {
    if (color != null) {
      g.color = color
      g.fillRect(x1, y1, x2 - x1, y2 - y1)
    }
    if (borderColor != null) {
      val oldStroke = g.stroke
      g.stroke = BasicStroke(JBUIScale.scale(1).toFloat())
      g.color = borderColor
      UIUtil.drawLine(g, x1, y1, x2 - 1, y1)
      UIUtil.drawLine(g, x1, y1, x1, y2 - 1)
      UIUtil.drawLine(g, x1, y2 - 1, x2 - 1, y2 - 1)
      g.stroke = oldStroke
    }
  }
}

private fun IntRange.intersect(another: IntRange): IntRange? =
  IntRange(kotlin.math.max(first, another.first), kotlin.math.min(last, another.last)).takeIf { it.last >= it.first }

private val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK")
