/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6

import com.intellij.psi.util.PsiTreeUtil
import junit.framework.TestCase
import org.jetbrains.r.classes.RClassesUtilTestsBase
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RCallExpression

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
        ),
        active = list(
          asd = 0,
          random = function(value) {
            if (missing(value)) {
              runif(1)  
            } else {
              stop("Can't set `${"$"}random`", call. = FALSE)
            }
          }
        )
      )
      """.trimIndent()

  private val shortedClassCodeDefinition = """
      Car <- R6Class("Car",
        inherit = Vehicle,
        list(
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
        )
      )
      """.trimIndent()

  fun testGetAssociatedClassName(){
    val rAssignmentStatement = getRootElementOfPsi(fullClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val className = R6ClassInfoUtil.getAssociatedClassNameFromR6ClassCall(rCallExpression!!)
    assertEquals("Car", className)
  }

  fun testGetAssociatedClassNameFromInstantiationCall(){
    val rAssignmentStatement = getRootElementOfPsi("""
      obj <- MyClass${'$'}new()
    """.trimIndent()) as RAssignmentStatement

    val call = rAssignmentStatement.lastChild as RCallExpression
    val className = R6ClassInfoUtil.getAssociatedClassNameFromInstantiationCall(call)
    assertEquals("MyClass", className)
  }

  fun testGetAssociatedSuperClasses(){
    val psiTree = getRootElementOfPsi("""
      SuperParentClass <- R6Class("SuperParentClass")
      ParentClass <- R6Class("ParentClass", inherit = SuperParentClass)
      ChildClass <- R6Class("ChildClass", inherit = ParentClass)
    """.trimIndent()).parent

    val lastClassAssignment = PsiTreeUtil.getChildrenOfType(psiTree, RAssignmentStatement::class.java).last() as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(lastClassAssignment)
    val superClassNames = R6ClassInfoUtil.getAssociatedSuperClassesHierarchy(rCallExpression!!)

    assertNotNull(superClassNames)
    assert(superClassNames!!.contains("ParentClass"))
    assert(superClassNames.contains("SuperParentClass"))
  }

  fun testGetAssociatedFields(){
    val rAssignmentStatement = getRootElementOfPsi(fullClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val classFields = R6ClassInfoUtil.getAssociatedFields(rCallExpression!!)

    assertNotNull(classFields)
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

  fun testGetAssociatedActiveBindings(){
    val rAssignmentStatement = getRootElementOfPsi(fullClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val classActiveBindings = R6ClassInfoUtil.getAssociatedActiveBindings(rCallExpression!!)

    TestCase.assertNotNull(classActiveBindings)
    assertEquals(classActiveBindings!!.size, 1)

    val classActiveBindingsNames = classActiveBindings.map { it.name }
    assertContainsElements(classActiveBindingsNames, "random")
  }

  fun testGetShortenedClassAssociatedFields(){
    val rAssignmentStatement = getRootElementOfPsi(shortedClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val classFields = R6ClassInfoUtil.getAssociatedFields(rCallExpression!!)

    TestCase.assertNotNull(classFields)
    assertEquals(classFields!!.size, 2)

    val classFieldsNames = classFields.map { it.name }
    assertContainsElements(classFieldsNames, "weight")
    assertContainsElements(classFieldsNames, "speed")
    classFields.forEach { assertEquals(true, it.isPublic) }
  }

  fun testGetShortenedClassAssociatedMethods(){
    val rAssignmentStatement = getRootElementOfPsi(shortedClassCodeDefinition) as RAssignmentStatement
    val rCallExpression = getRCallExpressionFromAssignment(rAssignmentStatement)
    val classMethods = R6ClassInfoUtil.getAssociatedMethods(rCallExpression!!)

    TestCase.assertNotNull(classMethods)
    assertEquals(classMethods!!.size, 2)

    val classMethodsNames = classMethods.map { it.name }
    assertContainsElements(classMethodsNames, "accelerate")
    assertContainsElements(classMethodsNames, "slowDown")
    classMethods.forEach { assertEquals(true, it.isPublic) }
  }
}