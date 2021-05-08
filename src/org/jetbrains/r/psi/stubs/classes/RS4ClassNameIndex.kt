/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs.classes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.jetbrains.r.classes.s4.RS4ClassInfo
import org.jetbrains.r.psi.api.RCallExpression

class RS4ClassNameIndex : StringStubIndexExtension<RCallExpression>() {
  override fun getKey(): StubIndexKey<String, RCallExpression> {
    return KEY
  }

  companion object {
    private val KEY = StubIndexKey.createIndexKey<String, RCallExpression>("R.s4class.shortName")

    fun processAllS4ClassInfos(project: Project,
                               scope: GlobalSearchScope?,
                               processor: Processor<Pair<RCallExpression, RS4ClassInfo>>) {
      val processingFunction = fun (declaration: RCallExpression) : Boolean =
        declaration.associatedS4ClassInfo?.let { processor.process(declaration to it) } ?: true

      return LibraryClassNameIndexUtil.processClassInfos(project, scope, KEY, processingFunction)
    }

    fun findClassInfos(name: String, project: Project, scope: GlobalSearchScope?): List<RS4ClassInfo> {
      val mapFunction = fun (declaration: RCallExpression) : RS4ClassInfo? = declaration.associatedS4ClassInfo

      return LibraryClassNameIndexUtil.findClassInfos<RS4ClassInfo>(KEY, mapFunction, name, project, scope)
    }

    fun findClassDefinitions(name: String, project: Project, scope: GlobalSearchScope?): Collection<RCallExpression> {
      return LibraryClassNameIndexUtil.findClassDefinitions(KEY, name, project, scope)
    }

    fun sink(sink: IndexSink, name: String) {
      return LibraryClassNameIndexUtil.sink(KEY, sink, name)
    }
  }
}