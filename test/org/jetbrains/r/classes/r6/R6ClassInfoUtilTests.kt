/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6

import junit.framework.TestCase
import org.jetbrains.r.classes.RClassesUtilTestsBase
import org.jetbrains.r.psi.api.RAssignmentStatement

class R6ClassInfoUtilTests : RClassesUtilTestsBase() {
  private val fullClassCodeDefinition = """
      Car <- R6Class("Car", 
        inherit = Vehicle,
        public = list(
          weight = 0,
          speed = 0,
          accelerate = function(acc = 1){
            self${"$"}speed <- self${"$"}speed + acc
            invisible(self)
          },
          slowDown = function(neg_acc = 1){
            self${"$"}speed <- self${"$"}speed - acc
            invisible(self)
          }
        ),
        private = list(
          engine_rpm = 1000,
          maximize = function(rmp = 1000) {
            self${"$"}engine_rpm <- self${"$"}engine_rpm + rmp
            invisible(self)
          }
        )
      )
      """.trimIndent()

  fun testGetAssociatedClassName(){
    val rAssignmentStatement = getRootElementOfPsi(fullClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val className = R6ClassInfoUtil.getAssociatedClassName(rCallExpression!!)
    assertEquals("Car", className)
  }

  fun testGetAssociatedSuperClassName(){
    val rAssignmentStatement = getRootElementOfPsi(fullClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val superClassName = R6ClassInfoUtil.getAssociatedSuperClassName(rCallExpression!!)
    assertEquals("Vehicle", superClassName)
  }

  fun testGetAssociatedFields(){
    val rAssignmentStatement = getRootElementOfPsi(fullClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val classFields = R6ClassInfoUtil.getAssociatedFields(rCallExpression!!)

    TestCase.assertNotNull(classFields)
    assertEquals(classFields!!.size, 3)

    val classFieldsNames = classFields.map { it.name }
    assertContainsElements(classFieldsNames, "weight")
    assertContainsElements(classFieldsNames, "speed")
    assertContainsElements(classFieldsNames, "engine_rpm")
  }

  fun testGetAssociatedMethods(){
    val rAssignmentStatement = getRootElementOfPsi(fullClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val classMethods = R6ClassInfoUtil.getAssociatedMethods(rCallExpression!!)

    TestCase.assertNotNull(classMethods)
    assertEquals(classMethods!!.size, 3)

    val classMethodsNames = classMethods.map { it.name }
    assertContainsElements(classMethodsNames, "accelerate")
    assertContainsElements(classMethodsNames, "slowDown")
    assertContainsElements(classMethodsNames, "maximize")
  }
}