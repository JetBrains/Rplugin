/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.stubs.classes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.r.psi.psi.api.RS4GenericOrMethodHolder

class RS4MethodsIndex : StringStubIndexExtension<RS4GenericOrMethodHolder>() {
  override fun getKey(): StubIndexKey<String, RS4GenericOrMethodHolder> {
    return KEY
  }

  companion object {
    private val KEY = StubIndexKey.createIndexKey<String, RS4GenericOrMethodHolder>("R.s4methods")

    fun findDefinitionsByName(name: String, project: Project, scope: GlobalSearchScope?): Collection<RS4GenericOrMethodHolder> {
      return StubIndex.getElements(KEY, name, project, scope, RS4GenericOrMethodHolder::class.java)
    }

    fun sink(sink: IndexSink, name: String) {
      sink.occurrence(KEY, name)
    }
  }
}