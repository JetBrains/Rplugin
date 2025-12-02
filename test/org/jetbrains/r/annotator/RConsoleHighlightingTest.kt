/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.annotator

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.impl.DefaultColorsScheme
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.r.console.RConsoleBaseTestCase
import com.intellij.r.psi.highlighting.*
import java.awt.Color
import java.awt.Font

class RConsoleHighlightingTest : RConsoleBaseTestCase() {
  fun testAnnotatorHighlightingFromMultilineInput() {
    val scheme: EditorColorsScheme = object : EditorColorsSchemeImpl(DefaultColorsScheme()) {
      init {
        initFonts()
      }
    }
    fun color(x : Int) = TextAttributes(Color(x), Color(x), null, null, Font.PLAIN)
    scheme.setAttributes(LOCAL_VARIABLE, color(1))
    scheme.setAttributes(FUNCTION_CALL, color(2))
    scheme.setAttributes(NAMED_ARGUMENT, color(3))

    (console.consoleEditor as EditorEx).colorsScheme = scheme

    console.appendCommandText("""
      some_var <- list(xxx = 10,
        yyy = 10,
        zzz = list(5))
    """.trimIndent())
    console.flushDeferredText()

    IdeEventQueue.getInstance().flushQueue()

    val editor = console.editor
    val markup = DocumentMarkupModel.forDocument(editor!!.document, project, true)

    val result = StringBuilder()
    val content = editor!!.document.charsSequence
    val comparator = Comparator.comparingInt<RangeHighlighter> { it.startOffset }.thenComparingInt { it.endOffset }
    val highlighting = markup.allHighlighters.toList().sortedWith(comparator)

    for (it in highlighting) {
      result.append(it.textRange)
      result.append(": '")
      result.append(it.textRange.subSequence(content))
      result.append("' : ")
      result.append(it.textAttributes)
      result.append('\n')
    }

    val textAttributes = TextAttributes()
    textAttributes.foregroundColor = Color.black
    val promptAttributes = console.promptAttributes?.attributes

    val colorsScheme = console.consoleEditor.colorsScheme

    val localVariableAttributes = colorsScheme.getAttributes(LOCAL_VARIABLE)
    val operatorAttributes = colorsScheme.getAttributes(OPERATION_SIGN)
    val commaAttributes = colorsScheme.getAttributes(COMMA)
    val parenthesesAttributes = colorsScheme.getAttributes(PARENTHESES)
    val numberAttributes = colorsScheme.getAttributes(NUMBER)
    val callAttributes = colorsScheme.getAttributes(FUNCTION_CALL)
    val namedArgumentAttributes = colorsScheme.getAttributes(NAMED_ARGUMENT)

    UsefulTestCase.assertSameLines("""
      (0,2): '> ' : $promptAttributes
      (2,10): 'some_var' : $localVariableAttributes
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