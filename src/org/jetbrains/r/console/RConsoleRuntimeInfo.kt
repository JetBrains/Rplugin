/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.r.psi.TableInfo
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.rinterop.RValue
import org.jetbrains.r.rinterop.getWithCheckCanceled

interface RConsoleRuntimeInfo {
  val variables: Map<String, RValue>
  val loadedPackages: Map<String, Int>
  val rMarkdownChunkOptions: List<String>
  val workingDir: String
  fun loadPackage(name: String)
  fun loadDistinctStrings(expression: String): List<String>
  fun loadObjectNames(expression: String) : List<String>
  fun loadInheritorNamedArguments(baseFunctionName: String) : List<String>
  fun getFormalArguments(expression: String) : List<String>
  fun loadTableColumns(expression: String): TableInfo
  val rInterop: RInterop

  companion object {
    fun get(psiFile: PsiFile): RConsoleRuntimeInfo? {
      if (ApplicationManager.getApplication().isUnitTestMode) psiFile.getUserData(KEY)?.let { return it }

      return get(psiFile.project)
    }

    fun get(project: Project): RConsoleRuntimeInfo? {
      val console = RConsoleManager.getInstance(project).currentConsoleOrNull ?: return null
      return console.consoleRuntimeInfo.takeIf { console.rInterop.isAlive }
    }
  }
}

class RConsoleRuntimeInfoImpl(override val rInterop: RInterop) : RConsoleRuntimeInfo {
  private val objectNamesCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val distinctStringsCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val inheritorNamedArgumentsCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val formalArgumentsCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val tableColumnsCache by rInterop.Cached { mutableMapOf<String, TableInfo>() }

  override val rMarkdownChunkOptions by lazy { rInterop.rMarkdownChunkOptions }

  override val variables
    get() = rInterop.currentEnvLoader.variables.map { it.name to it.value }.toMap()
  override val loadedPackages
    get() = rInterop.loadedPackages.getWithCheckCancel()
  override val workingDir
    get() = rInterop.workingDir

  override fun loadPackage(name: String) {
    rInterop.loadLibrary(name).getWithCheckCanceled()
  }

  override fun loadDistinctStrings(expression: String): List<String> {
    return distinctStringsCache.getOrPut(expression) { RRef.expressionRef(expression, rInterop).getDistinctStrings() }
  }

  override fun loadObjectNames(expression: String): List<String> {
    return objectNamesCache.getOrPut(expression) { RRef.expressionRef(expression, rInterop).ls() }
  }

  override fun loadInheritorNamedArguments(baseFunctionName: String): List<String> {
    return inheritorNamedArgumentsCache.getOrPut(baseFunctionName) {
      rInterop.findInheritorNamedArguments(RRef.expressionRef("'$baseFunctionName'", rInterop))
    }
  }

  override fun getFormalArguments(expression: String): List<String> {
    return formalArgumentsCache.getOrPut(expression) { rInterop.getFormalArguments(RRef.expressionRef(expression, rInterop)) }
  }

  override fun loadTableColumns(expression: String): TableInfo {
    return tableColumnsCache.getOrPut(expression) {
      rInterop.getTableColumnsInfo(RRef.expressionRef(expression, rInterop))
    }
  }
}

@TestOnly
private val KEY: Key<RConsoleRuntimeInfo> = Key.create("org.jetbrains.r.console.runtimeInfo")

@TestOnly
internal fun PsiFile.addRuntimeInfo(runtimeInfo: RConsoleRuntimeInfo) {
  putUserData(KEY, runtimeInfo)
}

val PsiFile.runtimeInfo: RConsoleRuntimeInfo?
  get() = RConsoleRuntimeInfo.get(this)
