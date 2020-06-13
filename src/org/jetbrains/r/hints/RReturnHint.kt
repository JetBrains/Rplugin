/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints

import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.api.*

sealed class RReturnHint(hintPrefix: String, function: RFunctionExpression) {
  val hintText: String = hintPrefix + getFunctionName(function)

  private fun getFunctionName(function: RFunctionExpression): String {
    val parent = function.parent
    if (parent is RNamedArgument) return parent.name

    val lambdaName = RBundle.message("inlay.hints.function.return.expression.lambda")
    if (parent !is RAssignmentStatement) return lambdaName

    val nameIdentifier = parent.assignee ?: return lambdaName
    val name = when (nameIdentifier) {
      is RMemberExpression -> {
        val owner = nameIdentifier.leftExpr as? RIdentifierExpression
        val member = nameIdentifier.rightExpr as? RIdentifierExpression
        if (owner != null && member != null) owner.name + nameIdentifier.listSubsetOperator.name + member.name
        else null
      }
      else -> nameIdentifier.name
    }
    return name ?: lambdaName
  }
}

class RExplicitReturnHint(function: RFunctionExpression)
  : RReturnHint(RBundle.message("inlay.hints.function.return.expression.explicit.prefix"), function)

class RImplicitReturnHint(function: RFunctionExpression)
  : RReturnHint(RBundle.message("inlay.hints.function.return.expression.implicit.prefix"), function)