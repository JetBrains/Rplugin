/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4

import org.jetbrains.r.classes.RClassesUtilTestsBase
import org.jetbrains.r.psi.api.RCallExpression

class RS4ClassInfoUtilTests : RClassesUtilTestsBase() {
  private val setClassCode = """
      setClass("Person",
        contains = "Being",
        slots = c(
          name = "character",
          age = "numeric"
        )
      ) 
      """.trimIndent()

  fun testGetAssociatedClassName(){
    val setClassExpression = getRootElementOfPsi(setClassCode) as RCallExpression
    val className = RS4ClassInfoUtil.getAssociatedClassName(setClassExpression)

    assertEquals("Person", className)
  }

  fun testGetAllAssociatedSlots(){
    val setClassExpression = getRootElementOfPsi(setClassCode) as RCallExpression
    val classSlots = RS4ClassInfoUtil.getAllAssociatedSlots(setClassExpression)

    assertEquals(2, classSlots.size)
    assertEquals(classSlots[0].name, "name")
    assertEquals(classSlots[0].type, "character")
    assertEquals(classSlots[1].name, "age")
    assertEquals(classSlots[1].type, "numeric")
  }

  fun testGetAllAssociatedSuperClasses(){
    val setClassExpression = getRootElementOfPsi(setClassCode) as RCallExpression
    val superClasses = RS4ClassInfoUtil.getAllAssociatedSuperClasses(setClassExpression)

    assertEquals(1, superClasses.size)
    assertEquals(superClasses[0], "Being")
  }
}