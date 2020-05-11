/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.editor.completion.NAMED_ARGUMENT_PRIORITY
import org.jetbrains.r.editor.completion.RLookupElement

class RMarkdownCompletionContributor : CompletionContributor() {
  init {
    addChunkOptionsCompletion()
  }

  private fun addChunkOptionsCompletion() {
    extend(CompletionType.BASIC, psiElement()
      .withLanguage(MarkdownLanguage.INSTANCE)
      .withElementType(MarkdownTokenTypes.FENCE_LANG), ChunkOptionsCompletionProvider())
  }

  private class ChunkOptionsCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, contextt: ProcessingContext, result: CompletionResultSet) {
      val info = parameters.originalFile.runtimeInfo ?: return
      val options = info.rMarkdownChunkOptions
      val prefixMatcher = result.prefixMatcher
      for (option in options) {
        result
          .withPrefixMatcher(prefixMatcher.cloneWithPrefix(modifyPrefix(result.prefixMatcher.prefix)))
          .consume(createChunkOptionLookupElement(option))
      }
    }

    companion object {
      private fun modifyPrefix(prefix: String): String {
        return if (prefix.takeWhile { it.isLetterOrDigit() }.compareTo("r", true) != 0) prefix
        else prefix.dropWhile { it.isLetterOrDigit() }.takeLastWhile { it != ',' }.trim()
      }

      private fun createChunkOptionLookupElement(lookupString: String): LookupElement {
        val icon = AllIcons.Nodes.Parameter
        return PrioritizedLookupElement.withInsertHandler(
          PrioritizedLookupElement.withPriority(RLookupElement(lookupString, true, icon, tailText = " = "), NAMED_ARGUMENT_PRIORITY),
          InsertHandler<LookupElement> { context, _ ->
            val document = context.document
            val startOffset = context.startOffset
            if (startOffset > 1 && !document.text[startOffset - 1].isWhitespace()) {
              document.insertString(startOffset, " ")
            }
            document.insertString(context.tailOffset, " = ")
            context.editor.caretModel.moveCaretRelatively(3, 0, false, false, false)
          }
        )
      }
    }
  }
}
