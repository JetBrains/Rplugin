package com.intellij.r.psi.rinterop

import com.google.protobuf.Empty
import com.google.protobuf.Int64Value
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.r.psi.console.RConsoleRuntimeInfo
import com.intellij.r.psi.debugger.RSourcePosition
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.r.psi.interpreter.RInterpreterState
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.Promise
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.RowSorter
import kotlin.reflect.KProperty

private val isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode

data class RIExecutionResult(val stdout: String, val stderr: String, val exception: String? = null)

interface RInterop : Disposable, UserDataHolder {
  val isAlive: Boolean
  val currentEnvRef: RReference
  val project: Project
  val interpreter: RInterpreter
  val processHandler: ProcessHandler
  val consoleRuntimeInfo: RConsoleRuntimeInfo
  val workingDir: String

  val globalEnvLoader: RVariableLoader

  val state: RInterpreterState

  fun isLibraryLoaded(name: String): Boolean
  fun loadLibrary(name: String): CancellablePromise<Unit>
  fun unloadLibrary(name: String, withDynamicLibrary: Boolean): CancellablePromise<Unit>

  data class HttpdResponse(val content: String, val url: String)
  fun startHttpd(): Promise<Int>
  fun httpdRequest(path: String): HttpdResponse?
  fun getDocumentationForPackage(packageName: String): CancellablePromise<HttpdResponse?>
  fun getDocumentationForSymbol(symbol: String, packageName: String? = null): CancellablePromise<HttpdResponse?>
  fun convertRoxygenToHTML(functionName: String, functionText: String): RIExecutionResult

  fun replExecute(code: String, setLastValue: Boolean = false, debug: Boolean = false): CancellablePromise<RIExecutionResult>

  fun executeTask(f: () -> Unit): Promise<Unit>
  fun copyToPersistentRef(proto: RRef): CancellablePromise<CopyToPersistentRefResponse>
  fun loaderGetValueInfo(proto: RRef): CancellablePromise<ValueInfo>
  fun evaluateAsText(proto: RRef): CancellablePromise<StringOrError>
  fun getDistinctStrings(proto: RRef): CancellablePromise<StringList>
  fun loadObjectNames(proto: RRef): CancellablePromise<StringList>
  fun getObjectSizes(refs: List<RReference>): List<Long>
  fun getEqualityObject(proto: RRef): CancellablePromise<Int64Value>
  fun setValue(request: SetValueRequest): CancellablePromise<ValueInfo>
  fun disposePersistentRefs(list: PersistentRefList): CancellablePromise<Empty>

  fun loaderGetParentEnvs(proto: RRef): CancellablePromise<ParentEnvsResponse>
  fun loaderGetVariables(request: GetVariablesRequest): CancellablePromise<VariablesResponse>

  fun getFunctionPosition(rRef: RReference): CancellablePromise<Pair<RSourcePosition, String?>?>
  fun getFunctionSourcePosition(rRef: RRef): CancellablePromise<GetFunctionSourcePositionResponse>

  fun repoAddLibraryPath(path: String): RIExecutionResult
  fun repoCheckPackageInstalled(packageName: String): RIExecutionResult
  fun repoRemovePackage(packageName: String, libraryPath: String)
  fun repoGetPackageVersion(packageName: String): RIExecutionResult
  fun repoInstallPackage(packageName: String, fallbackMethod: String?, arguments: Map<String, String>)

  fun dataFrameGetInfo(ref: RReference): CancellablePromise<DataFrameInfoResponse>
  fun dataFrameGetData(ref: RReference, start: Int, end: Int): CancellablePromise<DataFrameGetDataResponse>
  fun dataFrameSort(ref: RReference, sortKeys: List<RowSorter.SortKey>, disposableParent: Disposable? = null): RPersistentRef
  fun dataFrameFilter(ref: RReference, f: DataFrameFilterRequest.Filter, disposableParent: Disposable? = null): RPersistentRef
  fun dataFrameRefresh(ref: RReference): CancellablePromise<Boolean>

  fun getSourceFileText(fileId: String): String
  fun getSourceFileName(fileId: String): String

  fun rInteropGrpcLoggerAsJson(withPending: Boolean = false): String

  fun invalidateCaches()

  fun setRStudioApiEnabled(enabled: Boolean)

  var saveOnExit: Boolean

  fun <T : Any> cached(defaultValue: T? = null, f: () -> T): Cached<T>
  interface Cached<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
  }

  fun<T> asyncCached(defaultValue: T, f: () -> CancellablePromise<T>): AsyncCached<T>
  interface AsyncCached<T> {
    val value: T
    fun safeGet(): T
    fun getAsync(): Promise<T>
  }
}

fun <T> Future<T>.getWithCheckCanceled(cancelOnInterrupt: Boolean = true): T {
  while (true) {
    try {
      ProgressManager.checkCanceled()
      return get(50, TimeUnit.MILLISECONDS)
    }
    catch (ignored: TimeoutException) {
    }
    catch (e: InterruptedException) {
      if (cancelOnInterrupt) cancel(true)
      throw e
    }
    catch (e: ProcessCanceledException) {
      if (cancelOnInterrupt) cancel(true)
      throw e
    }
    catch (e: ExecutionException) {
      throw (e.cause as? RInteropException) ?: e
    }
  }
}
