/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints

import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RFunctionExpression

sealed class RReturnHint(hintPrefix: String, function: RFunctionExpression) {
  val hintText = hintPrefix + ((function.parent as? RAssignmentStatement)?.name ?: RBundle.message(
    "inlay.hints.function.return.expression.lambda"))
}

class RExplicitReturnHint(function: RFunctionExpression)
  : RReturnHint(RBundle.message("inlay.hints.function.return.expression.explicit.prefix"), function)

class RImplicitReturnHint(function: RFunctionExpression)
  : RReturnHint(RBundle.message("inlay.hints.function.return.expression.implicit.prefix"), function)