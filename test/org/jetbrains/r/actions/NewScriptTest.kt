// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2011 Holger Brandl
 *
 * This code is licensed under BSD. For details see
 * http://www.opensource.org/licenses/bsd-license.php
 */

package org.jetbrains.r.actions

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.psi.PsiDirectory
import icons.org.jetbrains.r.actions.TestableCreateFileFromTemplateAction
import org.jetbrains.r.RUsefulTestCase
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class NewScriptTest : RUsefulTestCase() {

  fun testRScriptCreate() {
    doTest(NewRScriptAction::class, "R", NewRScriptAction.NEW_R_SCRIPT_TEMPLATE_NAME, "Created by")
  }

  fun testRMarkdownCreate() {
    addLibraries()
    doTest(NewRMarkdownAction::class, "rmd", NewRMarkdownAction.NEW_R_MARKDOWN_TEMPLATE_NAME, "author")
  }

  private fun findSrcDir(): PsiDirectory {
    return myFixture.configureByText(FileTypes.PLAIN_TEXT, "dummy content").containingDirectory
  }

  private fun <T : TestableCreateFileFromTemplateAction> doTest(fileCreatorClass: KClass<T>,
                                                                extension: String,
                                                                templateName: String,
                                                                subString: String) {
    val fileName = "new_file"
    val newFile = runWriteAction {
      fileCreatorClass.createInstance().createTestFile(fileName, templateName, findSrcDir())
    }
    assertNotNull(newFile)
    assertEquals("$fileName.$extension", newFile!!.name)
    assertTrue(newFile.text.contains(subString))
  }
}