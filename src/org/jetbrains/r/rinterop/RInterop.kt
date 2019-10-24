/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.google.common.util.concurrent.ListenableFuture
import com.google.protobuf.Empty
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Int32Value
import com.google.protobuf.StringValue
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Version
import com.intellij.util.ConcurrencyUtil
import com.jetbrains.rd.util.getOrCreate
import icons.org.jetbrains.r.psi.TableManipulationColumn
import io.grpc.*
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.interpreter.RVersion
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.visualize.RDataFrameViewer
import org.jetbrains.r.run.visualize.RDataFrameViewerImpl
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.RowSorter
import javax.swing.SortOrder
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty

data class RIExecutionResult(val stdout: String, val stderr: String)

private const val DEADLINE_TEST = 20L

class RInterop(val processHandler: ProcessHandler, address: String, port: Int, val project: Project) : Disposable {
  private val channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build()
  private val isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode

  internal val stub = RPIServiceGrpc.newBlockingStub(channel).let {
    if (isUnitTestMode) it.withDeadline(Deadline.after(DEADLINE_TEST, TimeUnit.SECONDS)) else it
  }
  internal val asyncStub = RPIServiceGrpc.newFutureStub(channel).let {
    if (isUnitTestMode) it.withDeadline(Deadline.after(DEADLINE_TEST, TimeUnit.SECONDS)) else it
  }
  private val executor = ConcurrencyUtil.newSingleThreadExecutor("RInterop")
  private val replListeners = HashSet<ReplListener>()
  @Volatile private var finished = false
  private var replProcessingStarted = false
  private val cacheIndex = AtomicInteger(0)
  private val dataFrameViewerCache = ConcurrentHashMap<Int, RDataFrameViewer>()

  val rInteropTestGenerator: RInteropTestGenerator? = if (ApplicationManager.getApplication().isInternal) RInteropTestGenerator() else null

  val rVersion: Version
  val globalEnvRef = RRef(Service.RRef.newBuilder().setGlobalEnv(Empty.getDefaultInstance()).build(), this)
  val globalEnvLoader = globalEnvRef.createVariableLoader()
  val currentEnvRef = RRef(Service.RRef.newBuilder().setCurrentEnv(Empty.getDefaultInstance()).build(), this)
  val currentEnvLoader = currentEnvRef.createVariableLoader()

  fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> execute(
    f: KFunction1<Request, Response>,
    request: Request
  ) : Response {
    val nextStubNumber = rInteropTestGenerator?.nextStubNumber()
    rInteropTestGenerator?.onStubMessageRequest(nextStubNumber!!, request, f.name)
    val response = f.invoke(request)
    rInteropTestGenerator?.onStubMessageResponse(nextStubNumber!!, response)
    return response
  }

  fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> executeAsync(
    f: KFunction1<Request, ListenableFuture<Response>>,
    request: Request
  ) : ListenableFuture<Response> {
    val nextStubNumber = rInteropTestGenerator?.nextStubNumber()
    rInteropTestGenerator?.onStubMessageRequest(nextStubNumber!!, request, f.name)
    val future = f.invoke(request)
    future.addListener(Runnable { rInteropTestGenerator?.onStubMessageResponse(nextStubNumber!!, future.get()) }, executor)
    return future
  }

  fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> executeWithCheckCancel(
    f: KFunction1<Request, ListenableFuture<Response>>,
    request: Request) : Response {
    val nextStubNumber = rInteropTestGenerator?.nextStubNumber()
    rInteropTestGenerator?.onStubMessageRequest(nextStubNumber!!, request, f.name)
    var result: Response? = null
    try {
        f.invoke(request).getWithCheckCanceled().also { result = it; return it }
    } finally {
      rInteropTestGenerator?.onStubMessageResponse(nextStubNumber!!, result)
    }
  }

  val workingDir: String by Cached {
    executeWithCheckCancel(asyncStub::getWorkingDir, Empty.getDefaultInstance()).value
  }

  val loadedPackages: List<String> by Cached {
    executeWithCheckCancel(asyncStub::loaderGetLoadedNamespaces, Empty.getDefaultInstance()).listList
  }

  val rMarkdownChunkOptions: List<String> by Cached {
    executeWithCheckCancel(asyncStub::getRMarkdownChunkOptions, Empty.getDefaultInstance()).listList
  }

  init {
    val info = execute(stub::getInfo, Empty.getDefaultInstance())
    rVersion = RVersion.forceParse(info.rVersion)
  }

  fun executeTask(f: () -> Unit) {
    executor.execute(f)
  }

  fun init(rScriptsPath: String, projectDir: String): RIExecutionResult {
    val request = Service.Init.newBuilder().setRScriptsPath(rScriptsPath).setProjectDir(projectDir).build()
    return executeRequest(RPIServiceGrpc.getInitMethod(), request)
  }

  fun setWorkingDir(dir: String) {
    executeWithCheckCancel(asyncStub::setWorkingDir, StringValue.of(dir))
    invalidateCaches()
  }

  fun loadLibrary(name: String) {
    executeWithCheckCancel(asyncStub::loadLibrary, StringValue.of(name))
    invalidateCaches()
  }

  fun executeCode(code: String, withCheckCancelled: Boolean = false): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getExecuteCodeMethod(), buildCodeRequest(code), withCheckCancelled)
  }

  fun sourceFile(file: String) {
    asyncStub.sourceFile(StringValue.of(file))
  }

  fun executeCodeAsync(code: String, consumer: ((String, ProcessOutputType) -> Unit)? = null): CancellablePromise<Unit> {
    return executeRequestAsync(RPIServiceGrpc.getExecuteCodeMethod(), buildCodeRequest(code), consumer)
  }

  fun addReplListener(listener: ReplListener) {
    replListeners.add(listener)
  }

  fun removeReplListener(listener: ReplListener) {
    replListeners.remove(listener)
  }

  fun replExecute(code: String) {
    executeWithCheckCancel(asyncStub::replExecute, StringValue.of(code))
  }

  fun replSendReadLn(s: String) {
    executeWithCheckCancel(asyncStub::replSendReadLn, StringValue.of(s))
  }

  fun replSendDebuggerCommand(command: Service.DebuggerCommand) {
    executeWithCheckCancel(asyncStub::replSendDebuggerCommand, Service.DebuggerCommandRequest.newBuilder().setCommand(command).build())
  }

  fun replInterrupt() {
    executeWithCheckCancel(asyncStub::replInterrupt, Empty.getDefaultInstance())
  }

  fun graphicsInit(properties: RGraphicsUtils.InitProperties): RIExecutionResult {
    val parameters = properties.screenParameters
    val screenParametersMessage = Service.GraphicsInitRequest.ScreenParameters.newBuilder()
      .setWidth(parameters.width)
      .setHeight(parameters.height)
      .setResolution(parameters.resolution ?: -1)
      .build()
    val request = Service.GraphicsInitRequest.newBuilder()
      .setSnapshotDirectory(properties.snapshotDirectory)
      .setScreenParameters(screenParametersMessage)
      .setScaleFactor(properties.scaleFactor)
      .build()
    return executeRequest(RPIServiceGrpc.getGraphicsInitMethod(), request)
  }

  fun graphicsDump(): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getGraphicsDumpMethod(), Empty.getDefaultInstance())
  }

  fun graphicsReset(): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getGraphicsResetMethod(), Empty.getDefaultInstance())
  }

  fun htmlViewerInit(tracedFilePath: String) {
    executeRequest(RPIServiceGrpc.getHtmlViewerInitMethod(), StringValue.of(tracedFilePath))
  }

  fun htmlViewerReset() {
    executeRequest(RPIServiceGrpc.getHtmlViewerResetMethod(), Empty.getDefaultInstance())
  }

  fun runBeforeChunk(rmarkdownParameters: String, chunkText: String, outputDirectory: String, width: Int, height: Int): RIExecutionResult {
    val request = Service.ChunkParameters.newBuilder().setRmarkdownParameters(rmarkdownParameters)
                                                      .setChunkText(chunkText)
                                                      .setOutputDirectory(outputDirectory)
                                                      .setWidth(width)
                                                      .setHeight(height).build()
    return executeRequest(RPIServiceGrpc.getBeforeChunkExecutionMethod(), request)
  }

  fun runAfterChunk(): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getAfterChunkExecutionMethod(), Empty.getDefaultInstance())
  }

  fun repoGetPackageVersion(packageName: String): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getRepoGetPackageVersionMethod(), StringValue.of(packageName))
  }

  fun repoInstallPackage(packageName: String, arguments: Map<String, String>) {
    val request = Service.RepoInstallPackageRequest.newBuilder()
      .setPackageName(packageName)
      .putAllArguments(arguments)
      .build()
    executeAsync(asyncStub::repoInstallPackage, request)
  }

  fun repoCheckPackageInstalled(packageName: String): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getRepoCheckPackageInstalledMethod(), StringValue.of(packageName))
  }

  fun repoRemovePackage(packageName: String) {
    executeAsync(asyncStub::repoRemovePackage, StringValue.of(packageName))
  }

  fun dataFrameGetViewer(ref: RRef): RDataFrameViewer {
    RDataFrameViewerImpl.ensureDplyrInstalled(project)
    val index = executeWithCheckCancel(asyncStub::dataFrameRegister, ref.proto).value
    return dataFrameViewerCache.getOrCreate(index) {
      val persistentRef = RPersistentRef(index, this)
      Disposer.register(persistentRef, Disposable {
        dataFrameViewerCache.remove(index)
        executeWithCheckCancel(asyncStub::dataFrameDispose, Int32Value.of(index))
      })
      val viewer = RDataFrameViewerImpl(persistentRef)
      viewer
    }
  }

  fun dataFrameGetInfo(ref: RRef): Service.DataFrameInfoResponse {
    return executeWithCheckCancel(asyncStub::dataFrameGetInfo, ref.proto)
  }

  fun dataFrameGetData(ref: RRef, start: Int, end: Int): Promise<Service.DataFrameGetDataResponse> {
    val request = Service.DataFrameGetDataRequest.newBuilder().setRef(ref.proto).setStart(start).setEnd(end).build()
    return executeAsync(asyncStub::dataFrameGetData, request).toPromise(executor)
  }

  fun dataFrameSort(ref: RRef, sortKeys: List<RowSorter.SortKey>, disposableParent: Disposable? = null): RPersistentRef {
    val keysProto = sortKeys.map {
      Service.DataFrameSortRequest.SortKey.newBuilder()
        .setColumnIndex(it.column)
        .setDescending(it.sortOrder == SortOrder.DESCENDING)
        .build()
    }
    val request = Service.DataFrameSortRequest.newBuilder().setRef(ref.proto).addAllKeys(keysProto).build()
    return RPersistentRef(executeWithCheckCancel(asyncStub::dataFrameSort, request).value, this, disposableParent)
  }

  fun dataFrameFilter(ref: RRef, f: Service.DataFrameFilterRequest.Filter, disposableParent: Disposable? = null): RPersistentRef {
    val request = Service.DataFrameFilterRequest.newBuilder().setRef(ref.proto).setFilter(f).build()
    return RPersistentRef(executeWithCheckCancel(asyncStub::dataFrameFilter, request).value, this, disposableParent)
  }

  fun findAllNamedArguments(function: RRef): List<String> {
    return executeWithCheckCancel(asyncStub::findAllNamedArguments, function.proto).listList
  }

  fun getFormalArguments(function: RRef): List<String> {
    return executeWithCheckCancel(asyncStub::getFormalArguments, function.proto).listList
  }

  fun getTableColumnsInfo(table: RRef, tableType: Service.TableColumnsInfoRequest.TableType): List<TableManipulationColumn> {
    val request = Service.TableColumnsInfoRequest.newBuilder().setRef(table.proto).setTableType(tableType).build()
    return executeWithCheckCancel(asyncStub::getTableColumnsInfo, request).columnsList.map { TableManipulationColumn(it.name, it.type) }
  }

  fun convertRd2HTML(outputFilePath: String, rdFilePath: String = "", dbPath: String = "", dbPage: String = "", topicPackage: String = ""): RIExecutionResult {
    assert(rdFilePath.isEmpty() || (dbPage.isEmpty() && dbPath.isEmpty()))
    val builder = Service.ConvertRd2HTMLRequest.newBuilder()
      .setOutputFilePath(outputFilePath)
      .setTopicPackage(topicPackage)
    if (rdFilePath.isNotEmpty()) {
      builder.setRdFilePath(rdFilePath)
    }
    else {
      assert(dbPage.isNotEmpty() && dbPath.isNotEmpty())
      val dbRequest = Service.ConvertRd2HTMLRequest.DBRequest.newBuilder()
        .setDbPath(dbPath)
        .setDbPage(dbPage)
        .build()
      builder.setDbRequest(dbRequest)
    }

    return executeRequest(RPIServiceGrpc.getConvertRd2HTMLMethod(), builder.build())
  }

  fun makeRdFromRoxygen(functionName: String, functionText: String, outputFilePath: String): RIExecutionResult {
    val request = Service.MakeRdFromRoxygenRequest.newBuilder()
      .setFunctionName(functionName)
      .setFunctionText(functionText)
      .setOutputFilePath(outputFilePath)
      .build()
    return executeRequest(RPIServiceGrpc.getMakeRdFromRoxygenMethod(), request)
  }

  fun findPackagePathByTopic(topic: String, searchSpace: String): RIExecutionResult {
    val request = Service.FindPackagePathByTopicRequest.newBuilder()
      .setTopic(topic)
      .setSearchSpace(searchSpace)
      .build()
    return executeRequest(RPIServiceGrpc.getFindPackagePathByTopicMethod(), request)
  }

  fun findPackagePathByPackageName(packageName: String): RIExecutionResult {
    val request = Service.FindPackagePathByPackageNameRequest.newBuilder()
      .setPackageName(packageName)
      .build()
    return executeRequest(RPIServiceGrpc.getFindPackagePathByPackageNameMethod(), request)
  }

  fun debugWhere(): String {
    return execute(stub::debugWhere, Empty.getDefaultInstance()).value
  }

  fun updateSysFrames(): Int {
    return executeWithCheckCancel(asyncStub::updateSysFrames, Empty.getDefaultInstance()).value
  }

  fun debugGetSysFunctionCode(index: Int): String {
    return executeWithCheckCancel(asyncStub::debugGetSysFunctionCode, Int32Value.of(index)).value
  }

  fun debugExecute(code: String, fileId: String) {
    executeWithCheckCancel(asyncStub::debugExecute, Service.DebugExecuteRequest.newBuilder().setCode(code).setFileId(fileId).build())
  }

  fun clearEnvironment(env: RRef) {
    executeWithCheckCancel(asyncStub::clearEnvironment, env.proto)
    invalidateCaches()
  }

  private fun <TRequest : GeneratedMessageV3> executeRequest(
    methodDescriptor: MethodDescriptor<TRequest, Service.CommandOutput>,
    request: TRequest,
    withCheckCancelled: Boolean = false
  ) : RIExecutionResult {
    val stdoutBuffer = StringBuilder()
    val stderrBuffer = StringBuilder()
    val promise = executeRequestAsync(methodDescriptor, request) { text, type ->
      when (type) {
        ProcessOutputType.STDOUT -> stdoutBuffer.append(text)
        ProcessOutputType.STDERR -> stderrBuffer.append(text)
      }
    }
    if (withCheckCancelled) {
      while (true) {
        try {
          ProgressManager.checkCanceled()
          promise.blockingGet(50)
          break
        } catch (ignored: TimeoutException) {
        } catch (e: InterruptedException) {
          promise.cancel(true)
          throw ProcessCanceledException()
        } catch (e: ProcessCanceledException) {
          promise.cancel(true)
          throw e
        }
      }
    } else {
      promise.blockingGet(Int.MAX_VALUE)
    }
    return RIExecutionResult(stdoutBuffer.toString(), stderrBuffer.toString())
  }

  private fun <TRequest : GeneratedMessageV3> executeRequestAsync(
    methodDescriptor: MethodDescriptor<TRequest, Service.CommandOutput>,
    request: TRequest,
    consumer: ((String, ProcessOutputType) -> Unit)? = null
  ): CancellablePromise<Unit> {
    val number = rInteropTestGenerator?.nextStubNumber()
    rInteropTestGenerator?.onExecuteRequestAsync(number!!, methodDescriptor, request)
    val callOptions = if (isUnitTestMode) CallOptions.DEFAULT.withDeadlineAfter(DEADLINE_TEST, TimeUnit.SECONDS) else CallOptions.DEFAULT
    val call = channel.newCall(methodDescriptor, callOptions)
    val promise = object : AsyncPromise<Unit>() {
      override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (super.cancel(mayInterruptIfRunning)) {
          call.cancel("Interrupt of execution", null)
          return true
        }
        return false
      }
    }
    ClientCalls.asyncServerStreamingCall(call, request, object : StreamObserver<Service.CommandOutput> {
      override fun onNext(value: Service.CommandOutput) {
        rInteropTestGenerator?.onOutputAvailable(number!!, value)
        if (consumer == null) return
        when (value.type) {
          Service.CommandOutput.Type.STDOUT -> consumer(value.text.toStringUtf8(), ProcessOutputType.STDOUT)
          Service.CommandOutput.Type.STDERR -> consumer(value.text.toStringUtf8(), ProcessOutputType.STDERR)
          else -> {
          }
        }
      }

      override fun onError(t: Throwable?) {
        promise.setResult(Unit)
      }

      override fun onCompleted() {
        rInteropTestGenerator?.onExecuteRequestFinish(number!!)
        promise.setResult(Unit)
      }
    })
    return promise
  }

  private fun buildCodeRequest(code: String): Service.ExecuteCodeRequest {
    return Service.ExecuteCodeRequest.newBuilder().setCode(code).setWithEcho(true).build()
  }

  private fun processReplEvent(event: Service.ReplEvent) {
    when (event.eventCase) {
      Service.ReplEvent.EventCase.BUSY -> {
        replListeners.forEach { it.onBusy() }
      }
      Service.ReplEvent.EventCase.TEXT -> {
        val text = event.text.text
        val type = when (event.text.type) {
          Service.CommandOutput.Type.STDOUT -> ProcessOutputType.STDOUT
          Service.CommandOutput.Type.STDERR -> ProcessOutputType.STDERR
          else -> return
        }
        replListeners.forEach { it.onText(text.toStringUtf8(), type) }
      }
      Service.ReplEvent.EventCase.REQUESTREADLN -> {
        invalidateCaches()
        val prompt = event.requestReadLn.prompt
        replListeners.forEach { it.onRequestReadLn(prompt) }
      }
      Service.ReplEvent.EventCase.PROMPT -> {
        invalidateCaches()
        val isDebug = event.prompt.isDebug
        replListeners.forEach { it.onPrompt(isDebug) }
      }
      Service.ReplEvent.EventCase.TERMINATION -> {
        replListeners.forEach { it.onTermination() }
      }
      else -> {
      }
    }
  }

  fun replStartProcessing() {
    if (replProcessingStarted) return
    replProcessingStarted = true
    processReplEvents()
  }

  private fun processReplEvents() {
    val future = asyncStub.replGetNextEvent(Empty.getDefaultInstance())
    future.addListener(Runnable {
      try {
        val event = future.get()
        processReplEvent(event)
        if (event.eventCase == Service.ReplEvent.EventCase.TERMINATION) {
          return@Runnable
        }
      } catch (ignored: CancellationException) {
      } catch (ignored: ExecutionException) {
      }
      processReplEvents()
    }, executor)
  }

  override fun dispose() {
    finished = true
    executor.execute {
      try {
        executeAsync(asyncStub::quit, Empty.newBuilder().build()).get(1000, TimeUnit.MILLISECONDS)
      } catch (e: TimeoutException) {
      } catch (e: StatusRuntimeException) {
      } catch (e: ExecutionException) {
      }
      channel.shutdownNow()
      channel.awaitTermination(1000, TimeUnit.MILLISECONDS)
      executor.shutdownNow()
    }
    processHandler.destroyProcess()
  }

  fun invalidateCaches() {
    cacheIndex.incrementAndGet()
  }

  inner class Cached<T>(val f: () -> T) {
    private val cached = object : AtomicClearableLazyValue<T>() {
      override fun compute() = f()
    }
    private var cacheIndex = 0

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
      val currentCacheIndex = this@RInterop.cacheIndex.get()
      if (cacheIndex < currentCacheIndex) {
        cached.drop()
        cacheIndex = currentCacheIndex
      }
      return cached.value
    }
  }

  interface ReplListener {
    fun onText(text: String, type: ProcessOutputType) {}
    fun onBusy() {}
    fun onRequestReadLn(prompt: String) {}
    fun onPrompt(isDebug: Boolean) {}
    fun onTermination() {}
  }
}

internal fun <T> ListenableFuture<T>.getWithCheckCanceled(): T {
  while (true) {
    try {
      ProgressManager.checkCanceled()
      return get(50, TimeUnit.MILLISECONDS)
    } catch (ignored: TimeoutException) {
    } catch (e: InterruptedException) {
      cancel(true)
      throw ProcessCanceledException()
    } catch (e: ProcessCanceledException) {
      cancel(true)
      throw e
    }
  }
}

internal fun <T> ListenableFuture<T>.toPromise(executor: Executor): Promise<T> {
  val promise = AsyncPromise<T>()
  this.addListener(Runnable {
    promise.setResult(this.get())
  }, executor)
  return promise
}
