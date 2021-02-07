/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6

import org.jetbrains.r.classes.RClassesUtilTestsBase
import org.jetbrains.r.psi.api.RAssignmentStatement

class R6ClassInfoUtilTests : RClassesUtilTestsBase() {
  private val instantiateClassCode = """
      Car <- R6Class("Car", 
        inherit = Vehicle,
        list(
          weight,
          speed,
          accelerate = function(acc = 1){
            self${"$"}speed <- self${"$"}speed + acc
            invisible(self)
          },
          slowDown = function(neg_acc = 1){
            self${"$"}speed <- self${"$"}speed - acc
            invisible(self)
          }
      ))
      """.trimIndent()

  fun testGetAssociatedClassName(){
    val rAssignmentStatement = getRootElementOfPsi(instantiateClassCode) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val className = R6ClassInfoUtil.getAssociatedClassName(rCallExpression!!)
    assertEquals("Car", className)
  }

  fun testGetAssociatedSuperClassName(){
    val rAssignmentStatement = getRootElementOfPsi(instantiateClassCode) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val superClassName = R6ClassInfoUtil.getAssociatedSuperClassName(rCallExpression!!)
    assertEquals("Vehicle", superClassName)
  }
}