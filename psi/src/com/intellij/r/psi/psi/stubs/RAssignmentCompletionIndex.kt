/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.stubs

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.r.psi.psi.api.RAssignmentStatement

/**
 * This index is used to complete simple symbol names and with a package prefix by **::** operator
 * (`package_name::symbol_name<caret>` symbols).
 *
 * Completion for **:::** operator is defined in [RInternalAssignmentCompletionIndex] index
 */
class RAssignmentCompletionIndex : StringStubIndexExtension<RAssignmentStatement>() {

  override fun getKey(): StubIndexKey<String, RAssignmentStatement> {
    return KEY
  }

  companion object : CompletionIndexAccessBase("R.function.completion")
}
