/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.jetbrains.r.psi.api.RS4GenericOrMethodHolder

class RS4GenericIndex : StringStubIndexExtension<RS4GenericOrMethodHolder>() {
  override fun getKey(): StubIndexKey<String, RS4GenericOrMethodHolder> {
    return KEY
  }

  companion object {
    private val KEY = StubIndexKey.createIndexKey<String, RS4GenericOrMethodHolder>("R.s4generics")

    fun findDefinitionsByName(name: String, project: Project, scope: GlobalSearchScope?): Collection<RS4GenericOrMethodHolder> {
      return StubIndex.getElements(KEY, name, project, scope, RS4GenericOrMethodHolder::class.java)
    }

    fun processAll(project: Project, scope: GlobalSearchScope, processor: Processor<in RS4GenericOrMethodHolder>) {
      StubIndex.getInstance().getAllKeys(KEY, project).forEach {
        StubIndex.getInstance().processElements(KEY, it, project, scope, RS4GenericOrMethodHolder::class.java, processor)
      }
    }

    fun sink(sink: IndexSink, name: String) {
      sink.occurrence(KEY, name)
    }
  }
}