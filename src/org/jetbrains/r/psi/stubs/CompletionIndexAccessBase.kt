/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.jetbrains.r.psi.api.RAssignmentStatement

open class CompletionIndexAccessBase(name: String) {
  val KEY = StubIndexKey.createIndexKey<String, RAssignmentStatement>(name)

  fun sink(sink: IndexSink, name: String) {
    sink.occurrence(KEY, name)
  }

  fun process(name: String, project: Project, scope: GlobalSearchScope, processor: Processor<in RAssignmentStatement>): Boolean {
    return StubIndex.getInstance().processElements(KEY, name, project, scope, RAssignmentStatement::class.java, processor)
  }
}