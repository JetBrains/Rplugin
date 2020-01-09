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
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Version
import com.intellij.psi.PsiManager
import com.intellij.util.ConcurrencyUtil
import com.jetbrains.rd.util.getOrCreate
import icons.org.jetbrains.r.psi.TableManipulationColumn
import io.grpc.*
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import org.jetbrains.concurrency.*
import org.jetbrains.r.interpreter.RVersion
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.visualize.RDataFrameException
import org.jetbrains.r.run.visualize.RDataFrameViewer
import org.jetbrains.r.run.visualize.RDataFrameViewerImpl
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.RowSorter
import javax.swing.SortOrder
import kotlin.collections.HashSet
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty

data class RIExecutionResult(val stdout: String, val stderr: String, val exception: String? = null)

private const val DEADLINE_TEST = 40L

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
  private val heartbeatTimer = Timer().also {
    it.schedule(object : TimerTask() {
      override fun run() {
        executeAsync(asyncStub::isBusy, Empty.getDefaultInstance())
      }
    }, 0L, HEARTBEAT_PERIOD.toLong())
  }
  private val asyncEventsListeners = HashSet<AsyncEventsListener>()
  @Volatile private var finished = false
  private var asyncProcessingStarted = false
  private val asyncEventsBeforeStarted = mutableListOf<Service.AsyncEvent>()
  private val cacheIndex = AtomicInteger(0)
  private val dataFrameViewerCache = ConcurrentHashMap<Int, RDataFrameViewer>()

  val rInteropTestGenerator: RInteropTestGenerator? = if (ApplicationManager.getApplication().isInternal) RInteropTestGenerator() else null

  val rVersion: Version
  val globalEnvRef = RRef(Service.RRef.newBuilder().setGlobalEnv(Empty.getDefaultInstance()).build(), this)
  val globalEnvLoader = globalEnvRef.createVariableLoader()
  val currentEnvRef = RRef(Service.RRef.newBuilder().setCurrentEnv(Empty.getDefaultInstance()).build(), this)
  val currentEnvLoader = currentEnvRef.createVariableLoader()

  val isAlive: Boolean
    get() = !finished

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

  val loadedPackages: Map<String, Int> by Cached {
    executeWithCheckCancel(asyncStub::loaderGetLoadedNamespaces,
                           Empty.getDefaultInstance()).listList.mapIndexed { index, s -> s to index }.toMap()
  }

  val rMarkdownChunkOptions: List<String> by Cached {
    executeWithCheckCancel(asyncStub::getRMarkdownChunkOptions, Empty.getDefaultInstance()).listList
  }

  init {
    val info = execute(stub::getInfo, Empty.getDefaultInstance())
    rVersion = RVersion.forceParse(info.rVersion)
    processAsyncEvents()
  }

  fun executeTask(f: () -> Unit) {
    executor.execute(f)
  }

  fun init(rScriptsPath: String, projectDir: String): RIExecutionResult {
    val request = Service.Init.newBuilder().setRScriptsPath(rScriptsPath).setProjectDir(projectDir).build()
    return executeRequest(RPIServiceGrpc.getInitMethod(), request)
  }

  fun isBusy(): Boolean {
    return executeWithCheckCancel(asyncStub::isBusy, Empty.getDefaultInstance()).value
  }

  fun setWorkingDir(dir: String) {
    executeWithCheckCancel(asyncStub::setWorkingDir, StringValue.of(dir))
    invalidateCaches()
  }

  fun loadLibrary(name: String) {
    executeWithCheckCancel(asyncStub::loadLibrary, StringValue.of(name))
    invalidateCaches()
  }

  fun setOutputWidth(width: Int) {
    executeAsync(asyncStub::setOutputWidth, Int32Value.of(width))
  }

  fun replExecute(code: String) {
    executeCodeAsync(code, isRepl = true).getWithCheckCanceled()
  }

  fun executeCode(code: String, withCheckCancelled: Boolean = false): RIExecutionResult {
    val promise = executeCodeAsync(code)
    return if (withCheckCancelled) {
      promise.getWithCheckCanceled()
    } else {
      promise.blockingGet(Int.MAX_VALUE)!!
    }
  }

  fun executeCodeAsync(code: String, withEcho: Boolean = true,
                       debugFileId: String = "",
                       isRepl: Boolean = false,
                       returnOutput: Boolean = !isRepl,
                       outputConsumer: ((String, ProcessOutputType) -> Unit)? = null): CancellablePromise<RIExecutionResult> {
    val request = Service.ExecuteCodeRequest.newBuilder()
      .setCode(code)
      .setDebugFileId(debugFileId)
      .setWithEcho(withEcho)
      .setStreamOutput(returnOutput || outputConsumer != null)
      .setIsRepl(isRepl)
      .build()
    val number = rInteropTestGenerator?.nextStubNumber()
    rInteropTestGenerator?.onExecuteRequestAsync(number!!, RPIServiceGrpc.getExecuteCodeMethod(), request)
    val call = channel.newCall(RPIServiceGrpc.getExecuteCodeMethod(), CallOptions.DEFAULT)
    val promise = object : AsyncPromise<RIExecutionResult>() {
      override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (super.cancel(mayInterruptIfRunning)) {
          call.cancel("Interrupt of execution", null)
          return true
        }
        return false
      }
    }

    val stdoutBuffer = StringBuilder()
    val stderrBuffer = StringBuilder()
    var exception: String? = null

    ClientCalls.asyncServerStreamingCall(call, request, object : StreamObserver<Service.ExecuteCodeResponse> {
      override fun onNext(value: Service.ExecuteCodeResponse) {
        when (value.msgCase) {
          Service.ExecuteCodeResponse.MsgCase.OUTPUT -> {
            rInteropTestGenerator?.onOutputAvailable(number!!, value.output)
            when (value.output.type) {
              Service.CommandOutput.Type.STDOUT -> {
                outputConsumer?.invoke(value.output.text.toStringUtf8(), ProcessOutputType.STDOUT)
                if (returnOutput) stdoutBuffer.append(value.output.text.toStringUtf8())
              }
              Service.CommandOutput.Type.STDERR -> {
                outputConsumer?.invoke(value.output.text.toStringUtf8(), ProcessOutputType.STDERR)
                if (returnOutput) stderrBuffer.append(value.output.text.toStringUtf8())
              }
              else -> {}
            }
          }
          Service.ExecuteCodeResponse.MsgCase.EXCEPTION -> {
            exception = value.exception
          }
          else -> {}
        }
      }

      override fun onError(t: Throwable?) {
        promise.setError(t ?: RuntimeException())
      }

      override fun onCompleted() {
        rInteropTestGenerator?.onExecuteRequestFinish(number!!)
        promise.setResult(RIExecutionResult(stdoutBuffer.toString(), stderrBuffer.toString(), exception))
      }
    })
    return promise
  }

  fun replInterrupt() {
    executeAsync(asyncStub::replInterrupt, Empty.getDefaultInstance())
  }

  fun replSendReadLn(s: String) {
    executeWithCheckCancel(asyncStub::sendReadLn, StringValue.of(s))
  }

  fun replSendDebuggerCommand(command: Service.DebuggerCommand) {
    executeWithCheckCancel(asyncStub::sendDebuggerCommand, Service.DebuggerCommandRequest.newBuilder().setCommand(command).build())
  }

  fun addAsyncEventsListener(listener: AsyncEventsListener) {
    asyncEventsListeners.add(listener)
  }

  fun removeAsyncEventsListener(listener: AsyncEventsListener) {
    asyncEventsListeners.remove(listener)
  }

  fun graphicsInit(properties: RGraphicsUtils.InitProperties, inMemory: Boolean): RIExecutionResult {
    val screenParametersMessage = buildScreenParametersMessage(properties.screenParameters)
    val request = Service.GraphicsInitRequest.newBuilder()
      .setSnapshotDirectory(properties.snapshotDirectory)
      .setScreenParameters(screenParametersMessage)
      .setInMemory(inMemory)
      .build()
    return executeRequest(RPIServiceGrpc.getGraphicsInitMethod(), request)
  }

  fun graphicsDump(): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getGraphicsDumpMethod(), Empty.getDefaultInstance())
  }

  fun graphicsRescale(snapshotNumber: Int?, newParameters: RGraphicsUtils.ScreenParameters): RIExecutionResult {
    val newParametersMessage = buildScreenParametersMessage(newParameters)
    val request = Service.GraphicsRescaleRequest.newBuilder()
      .setSnapshotNumber(snapshotNumber ?: -1)
      .setNewParameters(newParametersMessage)
      .build()
    return executeRequest(RPIServiceGrpc.getGraphicsRescaleMethod(), request)
  }

  fun graphicsRescaleStored(
    parentDirectory: String,
    snapshotNumber: Int,
    snapshotVersion: Int,
    newParameters: RGraphicsUtils.ScreenParameters
  ): RIExecutionResult {
    val newParametersMessage = buildScreenParametersMessage(newParameters)
    val request = Service.GraphicsRescaleStoredRequest.newBuilder()
      .setParentDirectory(parentDirectory)
      .setSnapshotNumber(snapshotNumber)
      .setSnapshotVersion(snapshotVersion)
      .setNewParameters(newParametersMessage)
      .build()
    return executeRequest(RPIServiceGrpc.getGraphicsRescaleStoredMethod(), request)
  }

  private fun buildScreenParametersMessage(parameters: RGraphicsUtils.ScreenParameters): Service.ScreenParameters {
    return Service.ScreenParameters.newBuilder()
      .setWidth(parameters.width)
      .setHeight(parameters.height)
      .setResolution(parameters.resolution ?: -1)
      .build()
  }

  fun graphicsShutdown(): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getGraphicsShutdownMethod(), Empty.getDefaultInstance())
  }

  fun htmlViewerInit(tracedFilePath: String) {
    executeRequest(RPIServiceGrpc.getHtmlViewerInitMethod(), StringValue.of(tracedFilePath))
  }

  fun htmlViewerReset() {
    executeRequest(RPIServiceGrpc.getHtmlViewerResetMethod(), Empty.getDefaultInstance())
  }

  fun runBeforeChunk(rmarkdownParameters: String, chunkText: String, outputDirectory: String, screenParameters: RGraphicsUtils.ScreenParameters): RIExecutionResult {
    val request = Service.ChunkParameters.newBuilder().setRmarkdownParameters(rmarkdownParameters)
                                                      .setChunkText(chunkText)
                                                      .setOutputDirectory(outputDirectory)
                                                      .setWidth(screenParameters.width)
                                                      .setHeight(screenParameters.height)
                                                      .setResolution(screenParameters.resolution ?: -1)
                                                      .build()
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
    execute(stub::repoInstallPackage, request)
  }

  fun repoAddLibraryPath(path: String): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getRepoAddLibraryPathMethod(), StringValue.of(path))
  }

  fun repoCheckPackageInstalled(packageName: String): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getRepoCheckPackageInstalledMethod(), StringValue.of(packageName))
  }

  fun repoRemovePackage(packageName: String, libraryPath: String) {
    val request = Service.RepoRemovePackageRequest.newBuilder()
      .setPackageName(packageName)
      .setLibraryPath(libraryPath)
      .build()
    execute(stub::repoRemovePackage, request)
  }

  fun dataFrameGetViewer(ref: RRef): Promise<RDataFrameViewer> {
    try {
      RDataFrameViewerImpl.ensureDplyrInstalled(project)
    } catch (e: RequiredPackageException) {
       return rejectedPromise(e)
    }
    return executeAsync(asyncStub::dataFrameRegister, ref.proto).toPromise(executor).then {
      val index = it.value
      if (index == -1) {
        throw RDataFrameException("Invalid data frame")
      }
      dataFrameViewerCache.getOrCreate(index) {
        val persistentRef = RPersistentRef(index, this)
        Disposer.register(persistentRef, Disposable {
          dataFrameViewerCache.remove(index)
          execute(stub::dataFrameDispose, Int32Value.of(index))
        })
        val viewer = RDataFrameViewerImpl(persistentRef)
        viewer
      }
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

  private fun processAsyncEvent(event: Service.AsyncEvent) {
    when (event.eventCase) {
      Service.AsyncEvent.EventCase.TEXT -> {
        val text = event.text.text
        val type = when (event.text.type) {
          Service.CommandOutput.Type.STDOUT -> ProcessOutputType.STDOUT
          Service.CommandOutput.Type.STDERR -> ProcessOutputType.STDERR
          else -> return
        }
        asyncEventsListeners.forEach { it.onText(text.toStringUtf8(), type) }
      }
      Service.AsyncEvent.EventCase.REQUESTREADLN -> {
        invalidateCaches()
        val prompt = event.requestReadLn.prompt
        asyncEventsListeners.forEach { it.onRequestReadLn(prompt) }
      }
      Service.AsyncEvent.EventCase.PROMPT -> {
        invalidateCaches()
        val isDebug = event.prompt.isDebug
        asyncEventsListeners.forEach { it.onPrompt(isDebug) }
      }
      Service.AsyncEvent.EventCase.VIEWREQUEST -> {
        val ref = RPersistentRef(event.viewRequest.persistentRefIndex, this)
        val remaining = AtomicInteger(asyncEventsListeners.size)
        asyncEventsListeners.forEach { listener ->
          listener.onViewRequest(ref, event.viewRequest.title, ProtoUtil.rValueFromProto(event.viewRequest.value))
            .onProcessed {
              if (remaining.decrementAndGet() == 0) {
                Disposer.dispose(ref)
                executeAsync(asyncStub::viewRequestFinished, Empty.getDefaultInstance())
              }
            }
        }
      }
      Service.AsyncEvent.EventCase.TERMINATION -> {
        asyncEventsListeners.forEach { it.onTermination() }
      }
      else -> {
      }
    }
  }

  fun asyncEventsStartProcessing() {
    executor.execute {
      if (!asyncProcessingStarted) {
        asyncProcessingStarted = true
        asyncEventsBeforeStarted.forEach { processAsyncEvent(it) }
        asyncEventsBeforeStarted.clear()
      }
    }
  }

  private fun processAsyncEvents() {
    val future = asyncStub.getNextAsyncEvent(Empty.getDefaultInstance())
    future.addListener(Runnable {
      try {
        val event = future.get()
        if (asyncProcessingStarted) {
          processAsyncEvent(event)
        } else {
          asyncEventsBeforeStarted.add(event)
        }
        if (event.hasTermination()) return@Runnable
      } catch (ignored: CancellationException) {
      } catch (e: ExecutionException) {
        if ((e.cause as? StatusRuntimeException)?.status?.code == Status.Code.UNAVAILABLE) {
          processAsyncEvent(Service.AsyncEvent.newBuilder().setTermination(Empty.getDefaultInstance()).build())
          return@Runnable
        }
      }
      processAsyncEvents()
    }, executor)
  }

  override fun dispose() {
    finished = true
    heartbeatTimer.cancel()
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
    invokeLater { PsiManager.getInstance(project).dropPsiCaches() }
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

  interface AsyncEventsListener {
    fun onText(text: String, type: ProcessOutputType) {}
    fun onBusy() {}
    fun onRequestReadLn(prompt: String) {}
    fun onPrompt(isDebug: Boolean) {}
    fun onTermination() {}
    fun onViewRequest(ref: RRef, title: String, value: RValue): Promise<Unit> {
      return resolvedPromise()
    }
  }

  companion object {
    const val HEARTBEAT_PERIOD = 20000
  }
}

internal fun <T> Future<T>.getWithCheckCanceled(): T {
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
