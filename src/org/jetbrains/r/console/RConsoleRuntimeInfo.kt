/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import icons.org.jetbrains.r.psi.TableManipulationColumn
import org.jetbrains.annotations.TestOnly
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.rinterop.RValue
import org.jetbrains.r.rinterop.Service.TableColumnsInfoRequest.TableType

interface RConsoleRuntimeInfo {
  val variables: Map<String, RValue>
  val loadedPackages: List<String>
  val rMarkdownChunkOptions: List<String>
  val workingDir: String
  fun loadPackage(name: String)
  fun loadValueAsList(expression: String): List<String>
  fun loadObjectNames(expression: String) : List<String>
  fun loadAllNamedArguments(expression: String) : List<String>
  fun getFormalArguments(expression: String) : List<String>
  fun loadTableColumns(expression: String, tableType: TableType): List<TableManipulationColumn>
  val rInterop: RInterop

  companion object {
    fun get(psiFile: PsiFile): RConsoleRuntimeInfo? {
      if (ApplicationManager.getApplication().isUnitTestMode) psiFile.getUserData(KEY)?.let { return it }

      return get(psiFile.project)
    }

    fun get(project: Project): RConsoleRuntimeInfo? {
      return RConsoleManager.getInstance(project).currentConsoleOrNull?.consoleRuntimeInfo
    }
  }
}

class RConsoleRuntimeInfoImpl(override val rInterop: RInterop) : RConsoleRuntimeInfo {
  private val objectNamesCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val valueAsListCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val allNamedArgumentsCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val formalArgumentsCache by rInterop.Cached { mutableMapOf<String, List<String>>() }
  private val tableColumnsCache by rInterop.Cached { mutableMapOf<Pair<String, TableType>, List<TableManipulationColumn>>() }

  override val rMarkdownChunkOptions by lazy { rInterop.rMarkdownChunkOptions }

  override val variables
    get() = rInterop.currentEnvLoader.variables.map { it.name to it.value }.toMap()
  override val loadedPackages
    get() = rInterop.loadedPackages
  override val workingDir
    get() = rInterop.workingDir

  override fun loadPackage(name: String) {
    rInterop.loadLibrary(name)
  }

  override fun loadValueAsList(expression: String): List<String> {
    return valueAsListCache.getOrPut(expression) { RRef.expressionRef(expression, rInterop).evaluateAsStringList() }
  }

  override fun loadObjectNames(expression: String): List<String> {
    return objectNamesCache.getOrPut(expression) { RRef.expressionRef(expression, rInterop).ls() }
  }

  override fun loadAllNamedArguments(expression: String): List<String> {
    return allNamedArgumentsCache.getOrPut(expression) { rInterop.findAllNamedArguments(RRef.expressionRef(expression, rInterop)) }
  }

  override fun getFormalArguments(expression: String): List<String> {
    return formalArgumentsCache.getOrPut(expression) { rInterop.getFormalArguments(RRef.expressionRef(expression, rInterop)) }
  }

  override fun loadTableColumns(expression: String, tableType: TableType): List<TableManipulationColumn> {
    return tableColumnsCache.getOrPut(expression to tableType) {
      rInterop.getTableColumnsInfo(RRef.expressionRef(expression, rInterop), tableType)
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
