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
import com.intellij.util.Processor
import org.jetbrains.r.psi.api.RCallExpression

abstract class LibraryClassNameIndexBase<TClassInfo> : StringStubIndexExtension<RCallExpression>() {
  abstract val classKey: StubIndexKey<String, RCallExpression>

  abstract fun callProcessingForDeclaration(rCallExpression: RCallExpression, processor: Processor<Pair<RCallExpression, TClassInfo>>) : Boolean
  abstract fun getClassInfoFromRCallExpression(rCallExpression: RCallExpression) : TClassInfo?

  fun processAllClassInfos(project: Project, scope: GlobalSearchScope?, processor: Processor<Pair<RCallExpression, TClassInfo>>) {
    val stubIndex = StubIndex.getInstance()
    stubIndex.processAllKeys(classKey, project) { key ->
      stubIndex.processElements(classKey, key, project, scope, RCallExpression::class.java) {
        declaration -> callProcessingForDeclaration(declaration, processor)
      }
    }
  }

  fun findClassInfos(name: String, project: Project, scope: GlobalSearchScope?): List<TClassInfo> {
    return StubIndex.getElements(classKey, name, project, scope, RCallExpression::class.java).mapNotNull { getClassInfoFromRCallExpression(it) }
  }

  fun findClassDefinitions(name: String, project: Project, scope: GlobalSearchScope?): Collection<RCallExpression> {
    return StubIndex.getElements(classKey, name, project, scope, RCallExpression::class.java)
  }

  fun sink(sink: IndexSink, name: String) {
    sink.occurrence(classKey, name)
  }
}