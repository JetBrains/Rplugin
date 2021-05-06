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

  fun testLibClassChainSlot() {
    doTest("""
      obj <- new('SealedMethodDefinition')
      obj@target@<caret>
    """.trimIndent(), ".Data" to "character", "names" to "character", "package" to "character")
  }

  fun testUserClassChainSlot() {
    doTest("""
      setClass('MyClass1', slots = c(slot1 = 'type1', slot2 = 'type2'))
      setClass('MyClass2', slots = c(slot3 = 'MyClass1'))
      setClass('MyClass3', slots = c(slot4 = 'MyClass2'))
      setClass('MyClass4', slots = c(slot5 = 'MyClass3'))
      obj <- new('MyClass4')
      obj@slot5@slot4@slot3@<caret>
    """.trimIndent(), "slot1" to "type1", "slot2" to "type2")
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
    doTest("""
      setClass('MyClass1', slots = c(aaa = 'aaaType'))
      setClass('MyClass2', slots = c(bbb = 'bbbType'), contains = "MyClass1")
      obj <- new('MyClass2')
      obj@<caret>
    """.trimIndent(), "aaa" to "aaaType", "bbb" to "bbbType")
  }

  fun testUserClassWithDifficultUserSuperClass() {
    // setClass("className", contains = "character", representation(package = "character"))
    doTest("""
      setClass('MyClass1', slots = c(aaa = 'aaaType'), contains = "className")
      setClass('MyClass2', slots = c(bbb = 'bbbType'), contains = "MyClass1")
      setClass('MyClass3', slots = c(ccc = 'cccType'), contains = "MyClass2")
      obj <- new('MyClass3')
      obj@<caret>
    """.trimIndent(), ".Data" to "character", "aaa" to "aaaType", "bbb" to "bbbType", "ccc" to "cccType", "package" to "character")
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
    """.trimIndent(), "MyClass1" to "foo.R", "MyClass2" to "foo.R")
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
    """.trimIndent(), "MyClass1" to "foo.R", "MyClass2" to "foo.R")
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
    """.trimIndent(), "MyClass1" to "foo.R", "MyClass2" to "foo.R")
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
    """.trimIndent(), "MyClass1" to "foo.R", "MyClass2" to "foo.R")
    doWrongVariantsTest("setClass('MyClass', representation = representation('class<caret>' = 'character'))",
                        "classGeneratorFunction", "className", "classPrototypeDef", "classRepresentation")
  }

  fun testClassNameWithoutQuotes() {
    val variants =
      listOf("classGeneratorFunction", "className", "classPrototypeDef", "classRepresentation").map { "\"$it\"" to "methods" }
    doTest("new(class<caret>)", *variants.toTypedArray(), strict = false)
    doTest("""
      setClass('MyClass1')
      setClass('MyClass2')
      setClass('MyClass3', contains = MyC<caret>)
    """.trimIndent(), "\"MyClass1\"" to "foo.R", "\"MyClass2\"" to "foo.R", strict = false)
    doTest("""
      setClass('MyClass1')
      setClass('MyClass2')
      setClass('MyClass3', representation = representation(MyC<caret>))
    """.trimIndent(), "\"MyClass1\"" to "foo.R", "\"MyClass2\"" to "foo.R", strict = false)
    doTest("""
      setClass('Test', slots = c(name = 'numeric'))
      new_test <- new(Tes<caret>)
    """.trimIndent(), "\"Test\"" to "foo.R", strict = false)
  }

  fun testClassNameInComplexSlot() {
    doTest("""
      setClass('MyClass1')
      setClass('MyClass2')
      setClass('MyClass3', slots = c(slot = c('type1', ext = 'My<caret>')))
    """.trimIndent(), "MyClass1" to "foo.R", "MyClass2" to "foo.R", strict = false)
  }

  fun testOmitSetClassName() {
    doWrongVariantsTest("setClass('MyClass', 'My<caret>')", "MyClass")
  }

  fun testComplexSlot() {
    doTest("""
      setClass('MyClass', slots = list(slot1 = 'type1', slot2 = c('type2', ext = 'type3', 'type4')))
      obj <- new('MyClass')
      obj@<caret>
    """.trimIndent(), "slot1" to "type1", "slot2.ext" to "type3", "slot21" to "type2", "slot23" to "type4")
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
    """.trimIndent(), "MyClass1" to "foo.R", "MyClass2" to "foo.R")
  }

  fun testUserClassInSubdirectory() {
    myFixture.addFileToProject("subdir/data.R", "setClass('DataClass')")
    doTest("""
      setClass('DataFoo', contains = 'DataClass')
      setClass('MyClass', 'Data<caret>')
    """.trimIndent(), "DataClass" to "subdir/data.R", "DataFoo" to "foo.R")
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

  fun testSlotInNew() {
    doTest("""
      setClass("Person", slots = c(person_xxx_name = "character", person_xxx_age = "<-"))
      new("Person", person_xxx_<caret>)
    """.trimIndent(), "person_xxx_age" to "<-", "person_xxx_name" to "character")
    doTest("""
      setClass('MyClass1', slots = c(class_xxx_id = "numeric"))
      setClass('MyClass2', contains = "MyClass1", slots = c(class_xxx_no = "character"))
      new('MyClass2', class_xxx_<caret>)
    """.trimIndent(), "class_xxx_id" to "numeric", "class_xxx_no" to "character")

    doWrongVariantsTest("""
      setClass("Person", slots = c(person_name = "character", person_age = "<-"))
      new(person_<caret>)
    """.trimIndent(), "person_age", "person_name")
    doWrongVariantsTest("""
      setClass("Person", slots = c(person_name = "character", person_age = "<-"))
      new("Person", person_age = person_<caret>)
    """.trimIndent(), "person_age", "person_name")
  }

  fun testSlotInConsoleNew() {
    rInterop.executeCode("setClass('OldDevice', slots = c(imei = 'character'))")
    doTest("new('OldDevice', ime<caret>)", "imei" to "character", strict = false, withRuntimeInfo = true, inConsole = true)
  }

  // Trying to test case when class names like "<-" place somewhere at the end of the completion list
  fun testLanguageClassNamePriority() {
    val result = doTestBase("setClass('MyClass', slots = c(slotName = '<caret>'))")
    val strangeClasses = listOf("{", "(", "<-")
    for (langClass in strangeClasses) {
      val numberInOrder = result.indexOfFirst { it.lookupString == langClass }
      assertTrue("""
        Language class "$langClass" not in the end of completion list
        Completion list size: ${result.size}
        Expected that it will be after ${result.size - 15} elements
        Number in order: $numberInOrder
      """.trimIndent(), numberInOrder > result.size - 15)
    }
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

  fun testApplyCompletionClassNameWithoutQuotes() {
    doApplyCompletionTest("""
      setClass('Test', slots = c(name = 'numeric'))
      new_test <- new(Tes<caret>)
    """.trimIndent(), "Test", """
      setClass('Test', slots = c(name = 'numeric'))
      new_test <- new("Test"<caret>)
    """.trimIndent())
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
      elementPresentation.itemText to elementPresentation.typeText
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
