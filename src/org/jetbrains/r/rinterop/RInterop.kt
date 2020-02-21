/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.protobuf.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.messages.Topic
import com.jetbrains.rd.util.getOrCreate
import io.grpc.*
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import org.jetbrains.concurrency.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.RSourcePosition
import org.jetbrains.r.debugger.RStackFrame
import org.jetbrains.r.interpreter.RVersion
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.psi.TableInfo
import org.jetbrains.r.psi.TableManipulationColumn
import org.jetbrains.r.psi.TableType
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

interface LoadedLibrariesListener {
  fun onLibrariesUpdated()
}

private const val DEADLINE_TEST = 40L
val LOADED_LIBRARIES_UPDATED = Topic.create("R Interop loaded libraries updated", LoadedLibrariesListener::class.java)
const val RINTEROP_THREAD_NAME = "RInterop"

class RInterop(val processHandler: ProcessHandler, address: String, port: Int, val project: Project) : Disposable {
  private val channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build()
  private val isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode

  internal val stub = RPIServiceGrpc.newBlockingStub(channel).let {
    if (isUnitTestMode) it.withDeadline(Deadline.after(DEADLINE_TEST, TimeUnit.SECONDS)) else it
  }
  internal val asyncStub = RPIServiceGrpc.newFutureStub(channel).let {
    if (isUnitTestMode) it.withDeadline(Deadline.after(DEADLINE_TEST, TimeUnit.SECONDS)) else it
  }
  val executor = ConcurrencyUtil.newSingleThreadExecutor(RINTEROP_THREAD_NAME)
  private val heartbeatTimer: Timer
  private val asyncEventsListeners = HashSet<AsyncEventsListener>()
  private var asyncProcessingStarted = false
  private val asyncEventsBeforeStarted = mutableListOf<Service.AsyncEvent>()
  private val cacheIndex = AtomicInteger(0)
  private val dataFrameViewerCache = ConcurrentHashMap<Int, RDataFrameViewer>()
  internal val sourceFileManager = RSourceFileManager(this)

  val rInteropGrpcLogger = RInteropGrpcLogger(if (ApplicationManager.getApplication().isInternal) null else GRPC_LOGGER_MAX_MESSAGES)

  val globalEnvRef = RRef(Service.RRef.newBuilder().setGlobalEnv(Empty.getDefaultInstance()).build(), this)
  val globalEnvLoader = globalEnvRef.createVariableLoader()
  val currentEnvRef = RRef(Service.RRef.newBuilder().setCurrentEnv(Empty.getDefaultInstance()).build(), this)
  val currentEnvLoader = currentEnvRef.createVariableLoader()
  @Volatile var isDebug = false
    private set
  @Volatile var debugStack: List<RStackFrame> = emptyList()
    private set
  @Volatile var lastErrorStack: List<RStackFrame> = emptyList()
    private set

  private val terminationPromise = AsyncPromise<Unit>()
  val isAlive: Boolean
    get() = !terminationPromise.isDone

  fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> execute(
    f: KFunction1<Request, Response>,
    request: Request
  ) : Response {
    val nextStubNumber = rInteropGrpcLogger.nextStubNumber()
    rInteropGrpcLogger.onStubMessageRequest(nextStubNumber, request, f.name)
    val response = try {
      f.invoke(request)
    } catch (e: StatusRuntimeException) {
      reportIfCrash(e)
      throw e
    }
    rInteropGrpcLogger.onStubMessageResponse(nextStubNumber, response)
    return response
  }

  fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> executeAsync(
    f: KFunction1<Request, ListenableFuture<Response>>,
    request: Request
  ) : ListenableFuture<Response> {
    val nextStubNumber = rInteropGrpcLogger.nextStubNumber()
    rInteropGrpcLogger.onStubMessageRequest(nextStubNumber, request, f.name)
    return f.invoke(request).apply {
      addListener(Runnable {
        try {
          rInteropGrpcLogger.onStubMessageResponse(nextStubNumber, get())
        } catch (e: ExecutionException) {
          reportIfCrash(e.cause)
        }
      }, executor)
    }
  }

  fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> executeWithCheckCancel(
    f: KFunction1<Request, ListenableFuture<Response>>,
    request: Request) : Response {
    val nextStubNumber = rInteropGrpcLogger.nextStubNumber()
    rInteropGrpcLogger.onStubMessageRequest(nextStubNumber, request, f.name)
    var result: Response? = null
    try {
      f.invoke(request).getWithCheckCanceled().also { result = it; return it }
    } catch (e: ExecutionException) {
      reportIfCrash(e.cause)
      throw e
    } finally {
      rInteropGrpcLogger.onStubMessageResponse(nextStubNumber, result)
    }
  }

  val workingDir: String by Cached {
    executeWithCheckCancel(asyncStub::getWorkingDir, Empty.getDefaultInstance()).value
  }

  val loadedPackages: Map<String, Int> by Cached {
    executeWithCheckCancel(asyncStub::loaderGetLoadedNamespaces,
                           Empty.getDefaultInstance()).listList.mapIndexed { index, s -> s to index }.toMap().also {
      project.messageBus.syncPublisher(LOADED_LIBRARIES_UPDATED).onLibrariesUpdated()
    }
  }

  val rMarkdownChunkOptions: List<String> by Cached {
    executeWithCheckCancel(asyncStub::getRMarkdownChunkOptions, Empty.getDefaultInstance()).listList
  }

  private val getInfoResponse = execute(stub::getInfo, Empty.getDefaultInstance())
  val rVersion = RVersion.forceParse(getInfoResponse.rVersion)
  val workspaceFile = getInfoResponse.workspaceFile.takeIf { it.isNotEmpty() }
  var saveOnExit = getInfoResponse.saveOnExit
    set(value) {
      if (field != value && workspaceFile != null) {
        executeAsync(asyncStub::setSaveOnExit, BoolValue.of(value))
        field = value
      }
    }

  init {
    processAsyncEvents()
    heartbeatTimer = Timer().also {
      it.schedule(object : TimerTask() {
        override fun run() {
          executeAsync(asyncStub::isBusy, Empty.getDefaultInstance())
        }
      }, 0L, HEARTBEAT_PERIOD.toLong())
    }
  }

  fun <R> executeTask(f: () -> R): Promise<R> {
    val promise = AsyncPromise<R>()
    executor.execute {
      try {
        promise.setResult(f())
      } catch (e: Throwable) {
        promise.setError(e)
      }
    }
    return promise
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

  fun unloadLibrary(name: String, withDynamicLibrary: Boolean) {
    val request = Service.UnloadLibraryRequest.newBuilder()
      .setWithDynamicLibrary(withDynamicLibrary)
      .setPackageName(name)
      .build()
    executeWithCheckCancel(asyncStub::unloadLibrary, request)
    invalidateCaches()
  }

  fun setOutputWidth(width: Int) {
    executeWithCheckCancel(asyncStub::setOutputWidth, Int32Value.of(width))
  }

  fun replExecute(code: String): CancellablePromise<RIExecutionResult> {
    return executeCodeImpl(code, isRepl = true)
  }

  fun executeCode(code: String, withCheckCancelled: Boolean = false): RIExecutionResult {
    val promise = executeCodeImpl(code)
    return if (withCheckCancelled) {
      promise.getWithCheckCanceled()
    } else {
      val timeout = if (isUnitTestMode) EXECUTE_CODE_TEST_TIMEOUT else Int.MAX_VALUE
      promise.blockingGet(timeout)!!
    }
  }

  fun replSourceFile(file: VirtualFile, debug: Boolean = false, textRange: TextRange? = null, resetDebugCommand: Boolean = true,
                     consumer: ((String, ProcessOutputType) -> Unit)? = null): CancellablePromise<RIExecutionResult> {
    var code = ""
    var lineOffset = -1
    runReadAction {
      val document = FileDocumentManager.getInstance().getDocument(file)
                     ?: return@runReadAction
      code = textRange?.let { document.getText(it) } ?: document.text ?: ""
      lineOffset = textRange?.let { document.getLineNumber(it.startOffset) } ?: 0
    }
    if (lineOffset == -1) {
      return AsyncPromise<RIExecutionResult>().also { it.setError("No document for $file") }
    }
    return executeCodeImpl(
      code,
      sourceFileId = sourceFileManager.getFileId(file),
      sourceFileLineOffset = lineOffset,
      isRepl = true, isDebug = debug,
      resetDebugCommand = resetDebugCommand,
      outputConsumer = consumer
    )
  }

  fun executeCodeAsync(
    code: String, withEcho: Boolean = true, isRepl: Boolean = false, returnOutput: Boolean = !isRepl,
    isDebug: Boolean = false, outputConsumer: ((String, ProcessOutputType) -> Unit)? = null): CancellablePromise<RIExecutionResult> {
    return executeCodeImpl(code, withEcho = withEcho, isRepl = isRepl, returnOutput = returnOutput, isDebug = isDebug,
                           outputConsumer = outputConsumer)
  }

  private fun executeCodeImpl(
    code: String, withEcho: Boolean = true, sourceFileId: String = "", sourceFileLineOffset: Int = 0, isRepl: Boolean = false,
    returnOutput: Boolean = !isRepl, isDebug: Boolean = false, resetDebugCommand: Boolean = true,
    outputConsumer: ((String, ProcessOutputType) -> Unit)? = null):
    CancellablePromise<RIExecutionResult> {
    val request = Service.ExecuteCodeRequest.newBuilder()
      .setCode(code)
      .setSourceFileId(sourceFileId)
      .setSourceFileLineOffset(sourceFileLineOffset)
      .setWithEcho(withEcho)
      .setStreamOutput(returnOutput || outputConsumer != null)
      .setIsRepl(isRepl)
      .setIsDebug(isDebug)
      .setResetDebugCommand(resetDebugCommand && isDebug && isRepl)
      .build()
    val number = rInteropGrpcLogger.nextStubNumber()
    rInteropGrpcLogger.onExecuteRequestAsync(number, RPIServiceGrpc.getExecuteCodeMethod(), request)
    if (isRepl) this.isDebug = isDebug
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

    executeTask {
      ClientCalls.asyncServerStreamingCall(call, request, object : StreamObserver<Service.ExecuteCodeResponse> {
        override fun onNext(value: Service.ExecuteCodeResponse) {
          when (value.msgCase) {
            Service.ExecuteCodeResponse.MsgCase.OUTPUT -> {
              rInteropGrpcLogger.onOutputAvailable(number, value.output)
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
          if (isAlive) {
            reportIfCrash(t)
            promise.setError(t ?: RuntimeException())
          } else {
            promise.setResult(RIExecutionResult(stdoutBuffer.toString(), stderrBuffer.toString(), exception))
          }
        }

        override fun onCompleted() {
          rInteropGrpcLogger.onExecuteRequestFinish(number)
          promise.setResult(RIExecutionResult(stdoutBuffer.toString(), stderrBuffer.toString(), exception))
        }
      })
    }
    return promise
  }

  fun replInterrupt() {
    executeAsync(asyncStub::replInterrupt, Empty.getDefaultInstance())
  }

  fun replSendReadLn(s: String) = executeTask {
    execute(stub::sendReadLn, StringValue.of(s))
  }

  fun replSendEof() = executeTask {
    execute(stub::sendEof, Empty.getDefaultInstance())
  }

  fun addAsyncEventsListener(listener: AsyncEventsListener) {
    asyncEventsListeners.add(listener)
  }

  fun removeAsyncEventsListener(listener: AsyncEventsListener) {
    asyncEventsListeners.remove(listener)
  }

  fun debugAddBreakpoint(file: VirtualFile, line: Int,
                         suspend: Boolean = true,
                         evaluateAndLog: String? = null,
                         condition: String? = null) = executeTask {
    val position = Service.SourcePosition.newBuilder()
      .setFileId(sourceFileManager.getFileId(file)).setLine(line).build()
    execute(stub::debugAddBreakpoint, Service.DebugAddBreakpointRequest.newBuilder()
      .setPosition(position)
      .setSuspend(suspend)
      .setEvaluateAndLog(evaluateAndLog ?: "")
      .setCondition(condition ?: "")
      .build()
    )
  }

  fun debugRemoveBreakpoint(file: VirtualFile, line: Int) = executeTask {
    execute(stub::debugRemoveBreakpoint, Service.SourcePosition.newBuilder()
      .setFileId(sourceFileManager.getFileId(file)).setLine(line).build())
  }

  fun debugCommandContinue() = executeTask {
    execute(stub::debugCommandContinue, Empty.getDefaultInstance())
  }

  fun debugCommandPause() = executeTask {
    execute(stub::debugCommandPause, Empty.getDefaultInstance())
  }

  fun debugCommandStop() = executeTask {
    execute(stub::debugCommandStop, Empty.getDefaultInstance())
  }

  fun debugCommandStepOver() = executeTask {
    execute(stub::debugCommandStepOver, Empty.getDefaultInstance())
  }

  fun debugCommandStepInto() = executeTask {
    execute(stub::debugCommandStepInto, Empty.getDefaultInstance())
  }

  fun debugCommandForceStepInto() = executeTask {
    execute(stub::debugCommandForceStepInto, Empty.getDefaultInstance())
  }

  fun debugCommandStepOut() = executeTask {
    execute(stub::debugCommandStepOut, Empty.getDefaultInstance())
  }

  fun debugCommandRunToPosition(position: RSourcePosition) = executeTask {
    execute(stub::debugCommandRunToPosition, Service.SourcePosition.newBuilder()
      .setFileId(sourceFileManager.getFileId(position.file))
      .setLine(position.line)
      .build())
  }

  fun debugMuteBreakpoints(mute: Boolean) = executeTask {
    execute(stub::debugMuteBreakpoints, BoolValue.of(mute))
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

  class HttpdResponse(val content: ByteArray, val url: String)

  fun httpdRequest(url: String): HttpdResponse? {
    return executeWithCheckCancel(asyncStub::httpdRequest, StringValue.of(url))
      .takeIf { it.success }
      ?.let { HttpdResponse(it.content.toByteArray(), it.url) }
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

  fun getTableColumnsInfo(table: RRef): TableInfo {
    val request = Service.TableColumnsInfoRequest.newBuilder().setRef(table.proto).build()
    return executeWithCheckCancel(asyncStub::getTableColumnsInfo, request).run {
      TableInfo(columnsList.map { TableManipulationColumn(it.name, it.type) }, TableType.toTableType(tableType))
    }
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
    val number = rInteropGrpcLogger.nextStubNumber()
    rInteropGrpcLogger.onExecuteRequestAsync(number, methodDescriptor, request)
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
        rInteropGrpcLogger.onOutputAvailable(number, value)
        if (consumer == null) return
        when (value.type) {
          Service.CommandOutput.Type.STDOUT -> consumer(value.text.toStringUtf8(), ProcessOutputType.STDOUT)
          Service.CommandOutput.Type.STDERR -> consumer(value.text.toStringUtf8(), ProcessOutputType.STDERR)
          else -> {
          }
        }
      }

      override fun onError(t: Throwable?) {
        reportIfCrash(t)
        promise.setResult(Unit)
      }

      override fun onCompleted() {
        rInteropGrpcLogger.onExecuteRequestFinish(number)
        promise.setResult(Unit)
      }
    })
    return promise
  }

  private fun processAsyncEvent(event: Service.AsyncEvent) {
    when (event.eventCase) {
      Service.AsyncEvent.EventCase.BUSY -> {
        fireListeners { it.onBusy() }
      }
      Service.AsyncEvent.EventCase.TEXT -> {
        val text = event.text.text.toStringUtf8()
        val type = when (event.text.type) {
          Service.CommandOutput.Type.STDOUT -> ProcessOutputType.STDOUT
          Service.CommandOutput.Type.STDERR -> ProcessOutputType.STDERR
          else -> return
        }
        fireListeners { it.onText(text, type) }
      }
      Service.AsyncEvent.EventCase.REQUESTREADLN -> {
        invalidateCaches()
        val prompt = event.requestReadLn.prompt
        fireListeners { it.onRequestReadLn(prompt) }
      }
      Service.AsyncEvent.EventCase.SUBPROCESSINPUT -> {
        fireListeners { it.onSubprocessInput() }
      }
      Service.AsyncEvent.EventCase.PROMPT -> {
        invalidateCaches()
        isDebug = false
        debugStack = emptyList()
        fireListeners { it.onPrompt() }
      }
      Service.AsyncEvent.EventCase.DEBUGPROMPT -> {
        invalidateCaches()
        isDebug = true
        if (event.debugPrompt.changed) {
          debugStack = stackFromProto(event.debugPrompt.stack)
        }
        fireListeners { it.onPrompt(true) }
      }
      Service.AsyncEvent.EventCase.EXCEPTION -> {
        lastErrorStack = stackFromProto(event.exception.stack) { RRef.errorStackSysFrameRef(it, this) }
        val (text, details) = exceptionInfoFromProto(event.exception.exception)
        fireListeners { it.onException(text, details) }
      }
      Service.AsyncEvent.EventCase.TERMINATION -> {
        fireListeners { it.onTermination() }
      }
      Service.AsyncEvent.EventCase.VIEWREQUEST -> {
        val ref = RPersistentRef(event.viewRequest.persistentRefIndex, this)
        fireListenersAsync({ it.onViewRequest(ref, event.viewRequest.title, ProtoUtil.rValueFromProto(event.viewRequest.value)) }) {
          Disposer.dispose(ref)
          executeAsync(asyncStub::clientRequestFinished, Empty.getDefaultInstance())
        }
      }
      Service.AsyncEvent.EventCase.SHOWFILEREQUEST -> {
        val request = event.showFileRequest
        fireListenersAsync({it.onShowFileRequest(request.filePath, request.title) }) {
          executeAsync(asyncStub::clientRequestFinished, Empty.getDefaultInstance())
        }
      }
      Service.AsyncEvent.EventCase.SHOWHELPREQUEST -> {
        val request = event.showHelpRequest
        fireListeners { it.onShowHelpRequest(request.content, request.url) }
      }
      else -> {
      }
    }
  }

  private fun fireListenersAsync(f: (AsyncEventsListener) -> Promise<Unit>, end: () -> Unit) {
    val remaining = AtomicInteger(asyncEventsListeners.size)
    asyncEventsListeners.forEach { listener ->
      val promise = try {
        f(listener)
      } catch (t: Throwable) {
        LOG.error(t)
        resolvedPromise<Unit>()
      }
      promise.onProcessed {
        if (remaining.decrementAndGet() == 0) end()
      }
    }
  }

  private fun fireListeners(f: (AsyncEventsListener) -> Unit) {
    asyncEventsListeners.forEach {
      try {
        f(it)
      } catch (t: Throwable) {
        LOG.error(t)
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
    val future = executeAsync(asyncStub::getNextAsyncEvent, Empty.getDefaultInstance())
    future.addListener(Runnable {
      try {
        val event = future.get()
        executeTask {
          if (asyncProcessingStarted) {
            processAsyncEvent(event)
          } else {
            asyncEventsBeforeStarted.add(event)
          }
          if (event.hasTermination()) {
            heartbeatTimer.cancel()
            terminationPromise.setResult(Unit)
            asyncStub.quitProceed(Empty.getDefaultInstance())
          }
        }
        if (event.hasTermination()) return@Runnable
      } catch (ignored: CancellationException) {
      } catch (e: ExecutionException) {
        heartbeatTimer.cancel()
        terminationPromise.setResult(Unit)
        return@Runnable
      }
      processAsyncEvents()
    }, MoreExecutors.directExecutor())
  }

  private fun stackFromProto(proto: Service.StackFrameList,
                             indexToEnvironment: (Int) -> RRef = { RRef.sysFrameRef(it, this) }): List<RStackFrame> {
    return proto.framesList.mapIndexed { index, it ->
      val file = sourceFileManager.getFileById(it.position.fileId)
      val position = file?.let { f -> RSourcePosition(f, it.position.line) }
      RStackFrame(position, indexToEnvironment(index), it.functionName.takeIf { it.isNotEmpty() },
                  it.equalityObject)
    }
  }

  override fun dispose() {
    heartbeatTimer.cancel()
    executeAsync(asyncStub::quit, Empty.getDefaultInstance())
    if (isUnitTestMode) {
      if (!processHandler.isProcessTerminated) {
        terminationPromise.blockingGet(20000)
      }
      channel.shutdownNow()
      channel.awaitTermination(1000, TimeUnit.MILLISECONDS)
      if (!processHandler.waitFor(5000)) {
        processHandler.destroyProcess()
      }
      executor.shutdownNow()
    } else {
      ProgressManager.getInstance().run(object : Task.Backgroundable(project, RBundle.message("rinterop.terminating.title"), true) {
        override fun run(indicator: ProgressIndicator) {
          try {
            indicator.isIndeterminate = true
            terminationPromise.getWithCheckCanceled()
            executeTask {
              channel.shutdownNow()
              channel.awaitTermination(1000, TimeUnit.MILLISECONDS)
              if (!processHandler.waitFor(2000)) {
                processHandler.destroyProcess()
              }
              executor.shutdownNow()
            }
          } catch (e: ProcessCanceledException) {
            terminationPromise.setResult(Unit)
            processHandler.destroyProcess()
            executor.shutdownNow()
          }
        }

        override fun shouldStartInBackground() = false
      }.apply { setCancelText(RBundle.message("rinterop.terminate.now")) })
    }
  }

  fun executeOnTermination(f: () -> Unit) {
    terminationPromise.onProcessed { f() }
  }

  fun invalidateCaches() {
    invokeLater { PsiManager.getInstance(project).dropPsiCaches() }
    cacheIndex.incrementAndGet()
  }

  private fun reportIfCrash(t: Throwable?) {
    if (t is StatusRuntimeException && t.status.code == Status.Code.UNAVAILABLE) {
      RInteropUtil.reportCrash(this, RInteropUtil.updateCrashes())
    }
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
    fun onPrompt(isDebug: Boolean = false) {}
    fun onException(message: String, details: RExceptionDetails?) {}
    fun onTermination() {}
    fun onViewRequest(ref: RRef, title: String, value: RValue): Promise<Unit> = resolvedPromise()
    fun onShowHelpRequest(content: String, url: String) {}
    fun onShowFileRequest(filePath: String, title: String): Promise<Unit> = resolvedPromise()
    fun onSubprocessInput() {}
  }

  companion object {
    private const val HEARTBEAT_PERIOD = 20000
    private const val EXECUTE_CODE_TEST_TIMEOUT = 20000
    private const val GRPC_LOGGER_MAX_MESSAGES = 30
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
      throw e
    } catch (e: ProcessCanceledException) {
      cancel(true)
      throw e
    }
  }
}

internal fun <T> ListenableFuture<T>.toPromise(executor: Executor): CancellablePromise<T> {
  return this.then(executor) { it }
}

internal fun <T, R> ListenableFuture<T>.then(executor: Executor = MoreExecutors.directExecutor(), f: (T) -> R): CancellablePromise<R> {
  val promise = object : AsyncPromise<R>() {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
      if (super.cancel(mayInterruptIfRunning)) {
        this@then.cancel(mayInterruptIfRunning)
        return true
      }
      return false
    }
  }
  this.addListener(Runnable {
    try {
      promise.setResult(f(this.get()))
    } catch (e: Throwable) {
      promise.setError(e)
    }
  }, executor)
  return promise
}

private val LOG = Logger.getInstance(RInteropUtil.javaClass)

