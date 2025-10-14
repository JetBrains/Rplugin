/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6

import com.intellij.psi.util.childrenOfType
import com.intellij.r.psi.classes.r6.R6ClassInfoUtil
import org.jetbrains.r.classes.RClassesUtilTestsBase
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RCallExpression

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
    val rCallExpression = doTestBase(fullClassCodeDefinition)
    val className = R6ClassInfoUtil.getAssociatedClassNameFromR6ClassCall(rCallExpression)
    assertEquals("Car", className)
  }

  fun testGetAssociatedClassNameFromInstantiationCall(){
    val call = doTestBase("obj <- MyClass${'$'}new()")
    val className = R6ClassInfoUtil.getAssociatedClassNameFromInstantiationCall(call)
    assertEquals("MyClass", className)
  }

  fun testGetAssociatedSuperClasses(){
    val rCallExpression = doTestBase("""
      SuperParentClass <- R6Class("SuperParentClass")
      ParentClass <- R6Class("ParentClass", inherit = SuperParentClass)
      ChildClass <- R6Class("ChildClass", inherit = ParentClass)
    """.trimIndent())

    val superClassNames = R6ClassInfoUtil.getAssociatedSuperClassesHierarchy(rCallExpression)
    assertNotNull(superClassNames)
    assertContainsElements(superClassNames!!, "ParentClass", "SuperParentClass")
  }

  fun testClassContainsFields() = doFieldsTest(fullClassCodeDefinition, "weight" to true, "speed" to true, "engine_rpm" to false)
  fun testClassContainsMethods() = doMethodsTest(fullClassCodeDefinition, "accelerate" to true, "slowDown" to true, "maximize" to false)
  fun testGetAssociatedActiveBindings(){
    val rCallExpression = doTestBase(fullClassCodeDefinition)
    val classActiveBindings = R6ClassInfoUtil.getAssociatedActiveBindings(rCallExpression)

    assertNotNull(classActiveBindings)
    assertEquals(classActiveBindings!!.size, 1)
    assertContainsElements(classActiveBindings.map { it.name }, "random")
  }

  fun testGetShortenedClassAssociatedFields() = doFieldsTest(shortedClassCodeDefinition, "weight" to true, "speed" to true)
  fun testGetShortenedClassAssociatedMethods() = doMethodsTest(shortedClassCodeDefinition, "accelerate" to true, "slowDown" to true)
  fun testGetMembers(){
    val rCallExpression = doTestBase(fullClassCodeDefinition)
    val classMethods = R6ClassInfoUtil.getAssociatedMembers(rCallExpression)

    assertNotNull(classMethods)
    assertContainsElements(classMethods!!.map { it.name }, "weight", "speed", "engine_rpm", "maximize", "accelerate", "slowDown")
  }

  private fun doClassNameTest(text: String, className: String) {
    val call = doTestBase(text)
    val actualClassName = R6ClassInfoUtil.getAssociatedClassNameFromR6ClassCall(call)
    assertEquals(className, actualClassName)
  }

  private fun doFieldsTest(text: String, vararg fields: Pair<String, Boolean>) {
    val rCallExpression = doTestBase(text)
    val classFields = R6ClassInfoUtil.getAssociatedFields(rCallExpression)

    assertNotNull(classFields)
    assertContainsElements(classFields!!.map { it.name }, fields.map { it.first })
    assertEquals(classFields.map { it.isPublic }, fields.map { it.second })
  }

  private fun doMethodsTest(text: String, vararg fields: Pair<String, Boolean>) {
    val rCallExpression = doTestBase(text)
    val classMethods = R6ClassInfoUtil.getAssociatedMethods(rCallExpression)

    assertNotNull(classMethods)
    assertContainsElements(classMethods!!.map { it.name }, fields.map { it.first })
    assertEquals(classMethods.map { it.isPublic }, fields.map { it.second })
  }

  private fun doTestBase(text: String): RCallExpression {
    val rFile = myFixture.configureByText("foo.R", text)
    return rFile.childrenOfType<RAssignmentStatement>().last().assignedValue as RCallExpression
  }
}