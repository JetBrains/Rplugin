/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.r.psi.api.RCallExpression

abstract class LibraryClassNameIndex : StringStubIndexExtension<RCallExpression>() {
  override fun getKey(): StubIndexKey<String, RCallExpression> {
    return KEY
  }

  companion object {
    private val KEY = StubIndexKey.createIndexKey<String, RCallExpression>("R.libraryClass.shortName")

    fun findClassDefinitions(name: String, project: Project, scope: GlobalSearchScope?): Collection<RCallExpression> {
      return StubIndex.getElements(KEY, name, project, scope, RCallExpression::class.java)
    }

    fun sink(sink: IndexSink, name: String) {
      sink.occurrence(KEY, name)
    }
  }
}