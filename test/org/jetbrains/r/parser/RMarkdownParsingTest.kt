/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parser

import com.intellij.lang.ASTFactory
import com.intellij.lang.LanguageASTFactory
import com.intellij.psi.LanguageFileViewProviders
import com.intellij.testFramework.ParsingTestCase
import com.intellij.testFramework.TestDataPath
import com.jetbrains.python.*
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyPsiFacade
import com.jetbrains.python.psi.impl.PyPsiFacadeImpl
import com.jetbrains.python.psi.impl.PythonASTFactory
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import org.intellij.plugins.markdown.lang.parser.MarkdownParserDefinition
import org.intellij.plugins.markdown.lang.psi.MarkdownASTFactory
import org.jetbrains.r.parsing.RParserDefinition
import org.jetbrains.r.rmarkdown.RMarkdownFileViewProviderFactory
import org.jetbrains.r.rmarkdown.RMarkdownLanguage
import org.jetbrains.r.rmarkdown.RMarkdownParserDefinition

private val DATA_PATH = System.getProperty("user.dir") + "/testData/parser/rmd/"

@TestDataPath("/testData/parser/rmd")
class RMarkdownParsingTest : ParsingTestCase(
  "",
  "rmd",
  true,
  RMarkdownParserDefinition(),
  MarkdownParserDefinition(),
  PythonParserDefinition(),
  RParserDefinition()
) {

  override fun setUp() {
    super.setUp()
    registerExtensionPoint(PythonDialectsTokenSetContributor.EP_NAME, PythonDialectsTokenSetContributor::class.java)
    registerExtension(PythonDialectsTokenSetContributor.EP_NAME, PythonTokenSetContributor())
    addExplicitExtension(LanguageASTFactory.INSTANCE, PythonLanguage.getInstance(), PythonASTFactory())
    PythonDialectsTokenSetProvider.reset()
    addExplicitExtension(LanguageFileViewProviders.INSTANCE, RMarkdownLanguage, RMarkdownFileViewProviderFactory)
    addExplicitExtension<ASTFactory>(LanguageASTFactory.INSTANCE, MarkdownLanguage.INSTANCE, MarkdownASTFactory())
    project.registerService(PyPsiFacade::class.java, PyPsiFacadeImpl::class.java)
    // Any version can be used for this test but the psi-tree test answer may be different
    LanguageLevel.FORCE_LANGUAGE_LEVEL = LanguageLevel.PYTHON27
  }

  override fun getTestDataPath(): String {
    return DATA_PATH
  }

  fun testSimple() {
    doTest(true)
  }

  fun testMulticell() {
    doTest(true)
  }

  fun testDifferentCells() {
    doTest(true)
  }
}
