/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.annotator

import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.impl.source.tree.injected.changesHandler.range
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.r.console.RConsoleBaseTestCase
import org.jetbrains.r.highlighting.*
import java.awt.Color

class RConsoleHighlightingTest : RConsoleBaseTestCase() {
  fun testAnnotatorHighlightingFromMultilineInput() {
    console.executeText("""
      some_var <- list(xxx = 10,
        yyy = 10,
        zzz = list(5))
    """.trimIndent())
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    console.flushDeferredText()

    val editor = console.editor
    val markup = DocumentMarkupModel.forDocument(editor.document, project, true)

    val result = StringBuilder()
    val content = editor.document.charsSequence
    val comparator = Comparator.comparingInt<RangeHighlighter> { it.startOffset }.thenComparingInt { it.endOffset }
    val highlighting = markup.allHighlighters.toList().sortedWith(comparator)

    for (it in highlighting) {
      result.append(it.range)
      result.append(": '")
      result.append(it.range.subSequence(content))
      result.append("' : ")
      result.append(it.textAttributes)
      result.append('\n')
    }

    val textAttributes = TextAttributes()
    textAttributes.foregroundColor = Color.black
    val promptAttributes = console.promptAttributes?.attributes

    val colorsScheme = console.consoleEditor.colorsScheme

    val funDeclarationAttributes = colorsScheme.getAttributes(FUNCTION_DECLARATION)
    val operatorAttributes = colorsScheme.getAttributes(OPERATION_SIGN)
    val commaAttributes = colorsScheme.getAttributes(COMMA)
    val parenthesesAttributes = colorsScheme.getAttributes(PARENTHESES)
    val numberAttributes = colorsScheme.getAttributes(NUMBER)
    val callAttributes = colorsScheme.getAttributes(FUNCTION_CALL)
    val namedArgumentAttributes = colorsScheme.getAttributes(NAMED_ARGUMENT)

    UsefulTestCase.assertSameLines("""
      (0,2): '> ' : $promptAttributes
      (2,10): 'some_var' : $funDeclarationAttributes
      (2,11): 'some_var ' : $textAttributes
      (11,13): '<-' : $operatorAttributes
      (13,18): ' list' : $textAttributes
      (14,18): 'list' : $callAttributes
      (18,19): '(' : $parenthesesAttributes
      (19,22): 'xxx' : $namedArgumentAttributes
      (19,25): 'xxx = ' : $textAttributes
      (25,27): '10' : $numberAttributes
      (27,28): ',' : $commaAttributes
      (28,29): '
      ' : $textAttributes
      (29,31): '+ ' : $promptAttributes
      (31,39): '  yyy = ' : $textAttributes
      (33,36): 'yyy' : $namedArgumentAttributes
      (39,41): '10' : $numberAttributes
      (41,42): ',' : $commaAttributes
      (42,43): '
      ' : $textAttributes
      (43,45): '+ ' : $promptAttributes
      (45,57): '  zzz = list' : $textAttributes
      (47,50): 'zzz' : $namedArgumentAttributes
      (53,57): 'list' : $callAttributes
      (57,58): '(' : $parenthesesAttributes
      (58,59): '5' : $numberAttributes
      (59,61): '))' : $parenthesesAttributes
      (61,62): '
      ' : $textAttributes
    """.trimIndent(), result.toString())
  }
}