/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class S4ClassCompletionTest : RProcessHandlerBaseTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testBaseClass() {
    doTest("""
      obj <- new('classRepresentation')
      obj@<caret>
    """.trimIndent(), "access" to "list", "className" to "character",
           "contains" to "list", "package" to "character",
           "prototype" to "ANY", "sealed" to "logical",
           "slots" to "list", "subclasses" to "list",
           "validity" to "OptionalFunction", "versionKey" to "externalptr",
           "virtual" to "logical")
  }

  fun testNotLoadedLibrary() {
    doTest("""
      obj <- new('grouped_df')
      obj@<caret>
    """.trimIndent(), ".Data" to "list", ".S3Class" to "character", "names" to "character", "row.names" to "data.frameRowLabels")
  }

  fun testUserClassWithSingleSlot() {
    doTest("""
      setClass('MyClass', slots = 'someSlot')
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), "someSlot" to "ANY")
  }

  fun testUserClassWithSlots() {
    doTest("""
      setClass('MyClass', slots = c('slot_n1', 'some_slot.n2'))
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), "slot_n1" to "ANY", "some_slot.n2" to "ANY")
  }

  fun testUserClassWithRepresentation() {
    doTest("""
      setClass('MyClass', representation = representation(mySlot = "character"))
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), "mySlot" to "character")

    doTest("""
      setClass('MyClass', representation = list(mySlot = "character"))
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), "mySlot" to "character")
  }

  fun testUserClassWithTypedSlots() {
    // In General, you can't mix typed and untyped slots at all, but why not try to produce a reasonable result?
    doTest("""
      setClass('MyClass', slots = c(aaa = "aaaType", bbb = "bbbType", "ccc")) 
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), "aaa" to "aaaType", "bbb" to "bbbType", "ccc" to "ANY")
  }

  fun testClassWithSuperClass() {
    // setClass("data.frame", representation(names = "character", row.names = "data.frameRowLabels"),
    //            contains = "list", prototype = unclass(data.frame()), where = envir)
    doTest("""
      obj <- new('data.frame')
      obj@<caret>
    """.trimIndent(), ".Data" to "list", ".S3Class" to "character", "names" to "character", "row.names" to "data.frameRowLabels")
  }

  fun testUserClassWithSuperClass() {
    // setClass("className", contains = "character", representation(package = "character"))
    doTest("""
      setClass('MyClass', slots = c(aaa = 'aaaType'), contains = "className")
      obj<- new('MyClass')
      obj@<caret>
    """.trimIndent(), ".Data" to "character", "aaa" to "aaaType", "package" to "character")
  }

  fun testUserClassWithUserSuperClass() {
    // setClass("className", contains = "character", representation(package = "character"))
    doTest("""
      setClass('MyClass1', slots = c(aaa = 'aaaType'))
      setClass('MyClass2', slots = c(bbb = 'bbbType'), contains = "MyClass1")
      obj <- new('MyClass2')
      obj@<caret>
    """.trimIndent(), "aaa" to "aaaType", "bbb" to "bbbType")
  }

  fun testSlotsAsList() {
    doTest("""
      setClass("students", slots = list(name = "character", gpa = "numeric"))
      obj <- new("students", name = "johnny", gpa = 4.0)
      obj@<caret>
    """.trimIndent(), "gpa" to "numeric", "name" to "character")
  }

  fun testImplicitDataSlot() {
    fun doDataSlotTest(className: String) {
      doTest("""
      setClass('MyClass', contains = "$className")
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), ".Data" to className, strict = false)
    }

    // methods:::.classTable[["character"]]@slots is empty
    doDataSlotTest("character")
    doDataSlotTest("S4")
    doDataSlotTest("integer")
    doDataSlotTest("ANY")
  }

  fun testImplicitXDataSlot() {
    fun doXDataSlotTest(className: String) {
      doTest("""
      setClass('MyClass', contains = "$className")
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), ".xData" to className, strict = false)
    }

    // methods:::.classTable[["environment"]]@slots is empty
    doXDataSlotTest("environment")
    doXDataSlotTest("externalptr")
    doXDataSlotTest("name")
    doXDataSlotTest("NULL")
  }

  fun testImplicitDataSlotInhVector() {
    fun doDataSlotInhVectorTest(className: String, classSlot: String) {
      doTest("""
      setClass('MyClass', contains = "$className")
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), ".Data" to classSlot, strict = false)
    }

    doDataSlotInhVectorTest("vector", "vector")
    doDataSlotInhVectorTest("matrix", "matrix")
    doDataSlotInhVectorTest("data.frame", "list")
    doDataSlotInhVectorTest("ts", "vector")
    doDataSlotInhVectorTest("factor", "integer")
  }

  fun testClassName() {
    val variants =
      listOf("classGeneratorFunction", "className", "classPrototypeDef", "classRepresentation").map { it to "methods" }
    doTest("new('class<caret>')", *variants.toTypedArray(), strict = false)
  }

  fun testUserClassName() {
    doTest("""
      setClass('MyClass1')
      new('My<caret>')
      setClass('MyClass2')
    """.trimIndent(), "MyClass1" to "", "MyClass2" to "")
  }

  fun testVirtualClassName() {
    doWrongVariantsTest("new('tb<caret>')", "tbl", "tbl_df")
    doWrongVariantsTest("new('POSIX<caret>')", "POSIXct", "POSIXlt", "POSIXt")
    doWrongVariantsTest("""
      setClass('MyVirtual', contains = "VIRTUAL")
      new('My<caret>')
    """.trimIndent(), "MyVirtual")
  }

  fun testClassNameAsSlotType() {
    val variants = listOf("POSIXct", "POSIXlt", "POSIXt").map { it to "methods" }
    doTest("setClass('MyClass', slots = c(slot = 'POSIX<caret>'))", *variants.toTypedArray())
    doTest("""
      setClass('MyClass1')
      setClass('MyClass2')
      setClass('MyClass3', slots = c(slot = 'My<caret>'))
    """.trimIndent(), "MyClass1" to "", "MyClass2" to "")
    doWrongVariantsTest("setClass('MyClass', slots = c('POSIX<caret>'))", "POSIXct", "POSIXlt", "POSIXt")
    doWrongVariantsTest("setClass('MyClass', slots = c('POSIX<caret>' = 'character'))", "POSIXct", "POSIXlt", "POSIXt")
    doWrongVariantsTest("setClass('MyClass', slot = 'class<caret>')",
                        "classGeneratorFunction", "className", "classPrototypeDef", "classRepresentation")
  }

  fun testClassNameAsSuperClass() {
    val variants = listOf("POSIXct", "POSIXlt", "POSIXt").map { it to "methods" }.toTypedArray()
    doTest("setClass('MyClass', contains = 'POSIX<caret>')", *variants)
    doTest("setClass('MyClass', contains = c(aaa = 'POSIX<caret>'))", *variants)
    doTest("""
      setClass('MyClass1')
      setClass('MyClass2')
      setClass('MyClass3', contains = c('My<caret>'))
    """.trimIndent(), "MyClass1" to "", "MyClass2" to "")
    doWrongVariantsTest("setClass('MyClass', contains = c('class<caret>' = 'character'))",
                        "classGeneratorFunction", "className", "classPrototypeDef", "classRepresentation")
  }

  fun testClassNameAsRepresentation() {
    val variants = listOf("POSIXct", "POSIXlt", "POSIXt").map { it to "methods" }.toTypedArray()
    doTest("setClass('MyClass', representation = representation('POSIX<caret>'))", *variants)
    doTest("setClass('MyClass', representation = list(aaa = 'POSIX<caret>'))", *variants)
    doTest("""
      setClass('MyClass1')
      setClass('MyClass2')
      setClass('MyClass3', 'My<caret>')
    """.trimIndent(), "MyClass1" to "", "MyClass2" to "")
    doWrongVariantsTest("setClass('MyClass', representation = representation('class<caret>' = 'character'))",
                        "classGeneratorFunction", "className", "classPrototypeDef", "classRepresentation")
  }

  fun testOmitSetClassName() {
    doWrongVariantsTest("setClass('MyClass', 'My<caret>')", "MyClass")
  }

  fun testUserClassWithPackageProject() {
    myFixture.addFileToProject("DESCRIPTION", """
      Package: TestPackage
      Title: Test package
      Version: 1.0
      Date: 2020-03-09
      Author: Who wrote it
      Description: Test package
      License: GPL (>= 2)
    """.trimIndent())

    doTest("""
      setClass('MyClass1')
      setClass('MyClass2')
      setClass('MyClass3', 'My<caret>')
    """.trimIndent(), "MyClass1" to "TestPackage", "MyClass2" to "TestPackage")
  }

  fun testConsole() {
    rInterop.executeCode("""
      setClass('MyClass', slots = c(aaa = 'aaaType', bbb = 'character'))
      obj <- new('MyClass')
    """.trimIndent())
    doTest("obj@<caret>", "aaa" to "aaaType", "bbb" to "character", withRuntimeInfo = true, inConsole = true)
  }

  fun testEditorPreferableToConsole() {
    rInterop.executeCode("setClass('MyClass', slots = c(aaa = 'aaaType'))")
    doTest("""
      setClass('MyClass', slots = c(bbb = 'bbbType'))
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), "bbb" to "bbbType", withRuntimeInfo = true)
  }

  fun testConsolePreferableToEditor() {
    myFixture.addFileToProject("def.R", "setClass('MyClass', slots = c(bbb = 'bbbType'))")
    rInterop.executeCode("""
      setClass('MyClass', slots = c(aaa = 'aaaType'))
      obj <- new('MyClass')
    """.trimIndent())
    doTest("obj@<caret>", "aaa" to "aaaType", withRuntimeInfo = true, inConsole = true)
  }

  fun testLoadedObj() {
    rInterop.executeCode("obj <- methods:::.classTable[['list']]")
    doTest("obj@<caret>", "className" to "character", "contains" to "list",
           "package" to "character", "slots" to "list", "virtual" to "logical",
           withRuntimeInfo = true, strict = false)
  }

  fun testConsoleClassName() {
    rInterop.executeCode("setClass('MyClass', slots = (aaa = 'aaaType'))")
    rInterop.executeCode("setClass('MyClass1', slots = (bbb = 'bbbType'))")
    doTest("new('My<caret>')'", "MyClass" to ".GlobalEnv", "MyClass1" to ".GlobalEnv", withRuntimeInfo = true)
  }

  fun testApplyCompletionField() {
    doApplyCompletionTest("""
      obj <- new('classRepresentation')
      obj@<caret>
    """.trimIndent(), "virtual", """
      obj <- new('classRepresentation')
      obj@virtual<caret>
    """.trimIndent())
  }

  fun testApplyCompletionClassName() {
    doApplyCompletionTest("setClass('MyClass', slots = c(cl = 'class<caret>'))", "classRepresentation",
                          "setClass('MyClass', slots = c(cl = 'classRepresentation'<caret>))")
  }

  private fun doWrongVariantsTest(text: String, vararg variants: String, withRuntimeInfo: Boolean = false, inConsole: Boolean = false) {
    val result = doTestBase(text, withRuntimeInfo, inConsole)
    assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    assertDoesntContain(lookupStrings, *variants)
  }

  private fun doTest(text: String,
                     vararg variants: Pair<String, String>, // <name, type>
                     strict: Boolean = true,
                     withRuntimeInfo: Boolean = false,
                     inConsole: Boolean = false) {
    val result = doTestBase(text, withRuntimeInfo, inConsole)
    assertNotNull(result)
    val lookupStrings = result.map {
      val elementPresentation = LookupElementPresentation()
      it.renderElement(elementPresentation)
      it.lookupString to elementPresentation.typeText
    }
    if (strict) {
      assertOrderedEquals(lookupStrings, *variants)
    }
    else {
      assertContainsOrdered(lookupStrings, *variants)
    }
  }

  private fun doTestBase(text: String, withRuntimeInfo: Boolean = false, inConsole: Boolean = false): Array<LookupElement> {
    myFixture.configureByText("foo.R", text)
    if (inConsole) {
      myFixture.file.putUserData(RConsoleView.IS_R_CONSOLE_KEY, true)
    }
    if (withRuntimeInfo) {
      myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    }
    return myFixture.completeBasic()
  }
}
