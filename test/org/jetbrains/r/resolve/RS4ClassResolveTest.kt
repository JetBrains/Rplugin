/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.RFileType
import org.jetbrains.r.classes.s4.*
import org.jetbrains.r.classes.s4.RS4ClassInfoUtil.toSlot
import org.jetbrains.r.console.RConsoleBaseTestCase
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RStringLiteralExpression

class RS4ClassResolveTest : RConsoleBaseTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testUserClassInNew() =
    doClassNameTest("setClass('MyClass')", "new('My<caret>Class')")

  fun testLibClassInNew() = doClassNameTest("""
    setClass('numeric',
             contains = c('vector')
    )
  """.trimIndent(), "new('nume<caret>ric')", isUserDefined = false)

  fun testClassNotFirstArgument() =
    doClassNameTest("setClass(slots = c(), Class = 'MyClass')", "new('My<caret>Class')")

  fun testContains() =
    doClassNameTest("setClass('MyClass')", "setClass('MyClass1', contains = 'MyCl<caret>ass')")

  fun testInsideContains() =
    doClassNameTest("setClass('MyClass')", "setClass('MyClass1', contains = c('MyCl<caret>ass'))")

  fun testRepresentationContains() =
    doClassNameTest("setClass('MyClass')", "setClass('MyClass1', representation = list('MyCl<caret>ass'))")

  fun testRepresentationSlots() =
    doClassNameTest("setClass('MyClass')", "setClass('MyClass1', representation = list(slot = 'MyCl<caret>ass'))")

  fun testSlotType() =
    doClassNameTest("setClass('MyClass')", "setClass('MyClass1', slots = c(slot = 'MyCl<caret>ass'))")

  fun testComplexSlotType() =
    doClassNameTest("setClass('MyClass')", "setClass('MyClass1', slots = c(slot = c('MyCl<caret>ass')))")

  fun testComplexSlotTypeWithExt() =
    doClassNameTest("setClass('MyClass')", "setClass('MyClass1', slots = c(slot = c(ext = 'MyCl<caret>ass')))")

  fun testNoClassInSlotWithoutType() =
    doNoResolveTest("setClass('MyClass')", "setClass('MyClass1', slots = c('MyCl<caret>ass'))")

  fun testNoResolveInSetClassName() =
    doNoResolveTest("setClass('MyClass', slots = c(slot = 'numeric')", "setClass('MyCl<caret>ass')")

  fun testHugeLibClass() =
    doClassNameTest("""
      setClass('MethodDefinition',
               contains = c(
                 'function',
                 'PossibleMethod'
               ),
               slots = c(
                 target = 'signature',
                 defined = 'signature',
                 generic = 'character',
                 .Data = 'function'
               )
      )
    """.trimIndent(), "new('MethodDefini<caret>tion')", isUserDefined = false)

  fun testClassInAnotherFile() {
    val another = myFixture.addFileToProject("subdir/data.R", "setClass('MyClass')")
    myFixture.configureByText(RFileType, "new('MyClas<caret>s')")
    val resolve = resolve().mapNotNull { it.element }
    assertEquals(1, resolve.size)
    val literal = ((resolve.single() as PomTargetPsiElement).target as RStringLiteralPomTarget).literal
    assertEquals((another.firstChild as RCallExpression).classNameLiteral, literal)
  }

  fun testUserClassSlot() =
    doSlotTest("setClass('MyClass', slots = c(slot = 'numeric'))", """
      obj <- new('MyClass')
      obj@slo<caret>t
    """.trimIndent(), RS4ClassSlot("slot", "numeric", "MyClass"))

  fun testUserClassComplexSlot() {
    val classDeclaration = "setClass('MyClass', slots = c(slot = c('numeric', ext = 'character')))"
    val obj = "obj <- new('MyClass')"
    doSlotTest(classDeclaration, "$obj\n obj@slo<caret>t1", RS4ClassSlot("slot1", "numeric", "MyClass"))
    doSlotTest(classDeclaration, "$obj\n obj@slo<caret>t.ext", RS4ClassSlot("slot.ext", "character", "MyClass"))
    doNoResolveTest(classDeclaration, "$obj\n obj@slo<caret>t")
    doNoResolveTest(classDeclaration, "$obj\n obj@slo<caret>t2")
  }

  fun testSlotInSuperClass() =
    doSlotTest("""
      setClass('MyClass', slots = c(slot = 'numeric'))
      setClass('MyClass1', slots = c(slot1 = 'character'), contains = 'MyClass')
    """.trimIndent(), """
      obj <- new('MyClass1')
      obj@sl<caret>ot
    """.trimIndent(), RS4ClassSlot("slot", "numeric", "MyClass"))

  fun testSlotInSuperClassRedeclaration() =
    doSlotTest("""
      setClass('MyClass', slots = c(slot = 'ANY'))
      setClass('MyClass1', slots = c(slot = 'numeric'), contains = 'MyClass')
    """.trimIndent(), """
      obj <- new('MyClass1')
      obj@sl<caret>ot
    """.trimIndent(), RS4ClassSlot("slot", "numeric", "MyClass1"))

  fun testSlotInNew() =
    doSlotTest("setClass('MyClass', slots = c(slot = 'numeric'))",
               "new('MyClass', sl<caret>ot = 10)",
               RS4ClassSlot("slot", "numeric", "MyClass"))

  fun testSlotInNewWithoutValue() =
    doSlotTest("setClass('MyClass', slots = c(slot = 'numeric'))",
               "new('MyClass', sl<caret>ot)",
               RS4ClassSlot("slot", "numeric", "MyClass"))

  fun testStringSlot() =
    doSlotTest("setClass('MyClass', slots = c('slot'))",
               "new('MyClass', sl<caret>ot)",
               RS4ClassSlot("slot", "ANY", "MyClass"))

  fun testLibClassSlot() =
    doSlotTest("""
      setClass('MethodDefinition',
               contains = c(
                 'function',
                 'PossibleMethod'
               ),
               slots = c(
                 target = 'signature',
                 defined = 'signature',
                 generic = 'character',
                 .Data = 'function'
               )
      )
    """.trimIndent(), """
     new('MethodDefinition', targ<caret>et = ) 
    """.trimIndent(), RS4ClassSlot("target", "signature", "MethodDefinition"))

  fun testNoSlotResolveOnDeclaration() =
    doNoResolveTest("setClass('MyClass', slots = c(slot = 'numeric'))",
                    "setClass('MyClass', slots = c(sl<caret>ot = 'numeric'))")

  fun testRuntimeClassName() {
    val decl = """
      setClass('MyClass',
               slots = c(eee = 'numeric')
      )
    """.trimIndent()
    doNoResolveTest(decl, "new('MyCl<caret>ass')", isUserDefined = false, inConsole = true)
    rInterop.executeCode(decl)
    doClassNameTest(decl, "new(Class = 'MyCl<caret>ass')", isUserDefined = false, inConsole = true)
  }

  fun testRuntimeSlot() {
    val decl = """
      setClass('MyClass',
               slots = c(eee = 'numeric')
      )
    """.trimIndent()
    doNoResolveTest(decl, "obj <- new('MyClass')\nobj@e<caret>ee", isUserDefined = false, inConsole = true)
    rInterop.executeCode(decl)
    doSlotTest(decl, "obj <- new(Class = 'MyClass')\nobj@e<caret>ee",
               RS4ClassSlot("eee", "numeric", "MyClass"), isUserDefined = false, inConsole = true)
  }

  private fun doSlotTest(classDeclaration: String,
                         text: String,
                         slotInfo: RS4ClassSlot,
                         isUserDefined: Boolean = true,
                         inConsole: Boolean = false) {
    val className = slotInfo.declarationClass
    val resolveResults = doBaseTest(classDeclaration, text, isUserDefined, inConsole)
    assertEquals(1, resolveResults.size)
    val result = resolveResults.single()
    val slot = if (isUserDefined) {
      assertTrue(result is RNamedArgument || result is PomTargetPsiElement)
      val decl = getDeclarationWithClassName(className)
      val argInfo = RParameterInfoUtil.getArgumentInfo(decl)!!
      val slotDecl = when (result) {
        is RNamedArgument -> result
        is PomTargetPsiElement -> when (val target = result.target) {
          is RS4ComplexSlotPomTarget -> target.slotDefinition
          is RStringLiteralPomTarget -> target.literal
          else -> error("Unexpected resolve target type: $target")
        }
        else -> error("Unexpected resolve type: $result")
      }
      assertTrue(PsiTreeUtil.isAncestor(argInfo.getArgumentPassedToParameter("slots"), slotDecl, false))
      when (result) {
        is RNamedArgument -> result.toSlot(className)
        is PomTargetPsiElement -> when (val target = result.target) {
          is RS4ComplexSlotPomTarget -> target.slot
          is RStringLiteralPomTarget -> target.literal.toSlot(className)
          else -> error("Unexpected resolve target type: $target")
        }
        else -> error("Unexpected resolve type: $result")
      }
    }
    else {
      val (decl, slotName) = when (result) {
        is PomTargetPsiElement -> {
          val target = result.target
          assertInstanceOf(target, RSkeletonS4SlotPomTarget::class.java)
          (target as RSkeletonS4ClassPomTarget).setClass to target.name
        }
        is RStringLiteralExpression, is RNamedArgument -> {
          assertTrue(RS4SourceManager.isS4ClassSourceElement(result))
          result.containingFile.firstChild as RCallExpression to (result as PsiNamedElement).name!!
        }
        else -> error("Unexpected resolve type: $result")
      }
      decl.associatedS4ClassInfo!!.slots.first { it.name == slotName }
    }
    assertEquals(slotInfo, slot)
  }

  private fun doClassNameTest(classDeclaration: String,
                              text: String,
                              isUserDefined: Boolean = true,
                              inConsole: Boolean = false,
                              className: String = "MyClass") {
    val resolveResults = doBaseTest(classDeclaration, text, isUserDefined, inConsole)
    assertEquals(1, resolveResults.size)
    val result = resolveResults.single()
    if (isUserDefined) {
      assertInstanceOf(result, PomTargetPsiElement::class.java)
      val target = (result as PomTargetPsiElement).target
      assertInstanceOf(target, RStringLiteralPomTarget::class.java)
      assertEquals(getDeclarationWithClassName(className).classNameLiteral, (target as RStringLiteralPomTarget).literal)
    }
    else {
      assertInstanceOf(result, PomTargetPsiElement::class.java)
      val declText = when (val target = (result as PomTargetPsiElement).target) {
        is RStringLiteralPomTarget -> {
          assertTrue(RS4SourceManager.isS4ClassSourceElement(target.literal))
          result.containingFile.text
        }
        is RSkeletonS4ClassPomTarget -> target.setClass.text
        else -> error("Unexpected resolve target type: $target")
      }
      assertEquals(classDeclaration, declText)
    }
  }

  private fun doNoResolveTest(classDeclaration: String, text: String, isUserDefined: Boolean = true, inConsole: Boolean = false) {
    assertEquals(0, doBaseTest(classDeclaration, text, isUserDefined, inConsole).size)
  }

  private fun doBaseTest(classDeclaration: String, text: String, isUserDefined: Boolean, isConsole: Boolean): List<PsiElement> {
    val fileText =
      if (isUserDefined) "$classDeclaration\n\n$text"
      else text
    myFixture.configureByText(RFileType, fileText)
    if (isConsole) {
      myFixture.file.putUserData(RConsoleView.IS_R_CONSOLE_KEY, true)
      myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    }
    return resolve().mapNotNull { it.element }
  }

  private fun getDeclarationWithClassName(className: String): RCallExpression {
    return myFixture.file.children.first {
      val call = it as? RCallExpression ?: return@first false
      call.classNameLiteral?.name == className
    } as RCallExpression
  }

  private val RCallExpression.classNameLiteral: RStringLiteralExpression?
    get() {
      val argumentInfo = RParameterInfoUtil.getArgumentInfo(this)!!
      return argumentInfo.getArgumentPassedToParameter("Class") as? RStringLiteralExpression
    }
}