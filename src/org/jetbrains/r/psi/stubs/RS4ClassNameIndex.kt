/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.jetbrains.r.classes.RS4ClassInfo
import org.jetbrains.r.psi.api.RCallExpression

  class RS4ClassNameIndex : StringStubIndexExtension<RCallExpression>() {
  override fun getKey(): StubIndexKey<String, RCallExpression> {
    return KEY
  }

  companion object {
    private val KEY = StubIndexKey.createIndexKey<String, RCallExpression>("R.s4class.shortName")

    fun processAllS4ClassInfos(project: Project, scope: GlobalSearchScope?, processor: Processor<in RS4ClassInfo>) {
      val stubIndex = StubIndex.getInstance()
      stubIndex.processAllKeys(KEY, project) { key ->
        stubIndex.processElements(KEY, key, project, scope, RCallExpression::class.java) { call ->
          call.associatedS4ClassInfo?.let { processor.process(it) } ?: true
        }
      }
    }

    fun findClassInfos(name: String, project: Project, scope: GlobalSearchScope?): List<RS4ClassInfo> {
      return StubIndex.getElements(KEY, name, project, scope, RCallExpression::class.java).mapNotNull { it.associatedS4ClassInfo }
    }

    fun findClassDefinitions(name: String, project: Project, scope: GlobalSearchScope?): Collection<RCallExpression> {
      return StubIndex.getElements(KEY, name, project, scope, RCallExpression::class.java)
    }

    fun sink(sink: IndexSink, name: String) {
      sink.occurrence(KEY, name)
    }
  }
}