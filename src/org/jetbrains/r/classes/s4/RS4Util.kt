package org.jetbrains.r.classes.s4

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.Contract
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.psi.isFunctionFromLibrarySoft

object RS4Util {

  @Contract("null -> null")
  fun parseCharacterVector(expr: RExpression?): List<RExpression>? = when (expr) {
    null -> null
    is RCallExpression -> {
      // Any function that returns a `vector` of `characters` is suitable.
      // Arbitrary function returns vector is some rare use case which difficult to analyse statically
      if (expr.isFunctionFromLibrarySoft("c", "base") ||
          expr.isFunctionFromLibrarySoft("list", "base") ||
          expr.isFunctionFromLibrarySoft("representation", "methods") ||
          expr.isFunctionFromLibrarySoft("signature", "methods")) {
        expr.argumentList.expressionList
      }
      else if (expr.isFunctionFromLibrarySoft("character", "base") &&
               expr.argumentList.text.let { it == "()" || it == "(0)" }) {
        emptyList()
      }
      else null
    }
    is RStringLiteralExpression -> listOf(expr)
    else -> null
  }

  internal fun Project.getProjectCachedAssignment(key: Key<RAssignmentStatement>, def: String): RAssignmentStatement {
      var definition = getUserData(key)
      if (definition == null || !definition.isValid) {
        val setClassDefinition = RElementFactory.createRPsiElementFromText(this, def) as RAssignmentStatement
        definition = setClassDefinition.also { putUserData(key, it) }
      }
      return definition
    }
}