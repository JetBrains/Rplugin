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
import com.intellij.openapi.application.ApplicationInfo
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
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.messages.Topic
import io.grpc.*
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.RSourcePosition
import org.jetbrains.r.debugger.RStackFrame
import org.jetbrains.r.hints.parameterInfo.RExtraNamedArgumentsInfo
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterState
import org.jetbrains.r.interpreter.RInterpreterStateImpl
import org.jetbrains.r.interpreter.RVersion
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.packages.RPackagePriority
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.psi.TableInfo
import org.jetbrains.r.psi.TableManipulationColumn
import org.jetbrains.r.psi.TableType
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.visualize.RDataFrameException
import org.jetbrains.r.run.visualize.RDataFrameViewer
import org.jetbrains.r.run.visualize.RDataFrameViewerImpl
import org.jetbrains.r.settings.RSettings
import org.jetbrains.r.util.thenCancellable
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.RowSorter
import javax.swing.SortOrder
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty

data class RIExecutionResult(val stdout: String, val stderr: String, val exception: String? = null)

interface LoadedLibrariesListener {
  fun onLibrariesUpdated()
}

private const val DEADLINE_TEST_DEFAULT = 40L
val LOADED_LIBRARIES_UPDATED = Topic.create("R Interop loaded libraries updated", LoadedLibrariesListener::class.java)
const val RINTEROP_THREAD_NAME = "RInterop"

class RInterop(val interpreter: RInterpreter, val processHandler: ProcessHandler,
               address: String, port: Int, val project: Project) : Disposable {
  private val channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().maxInboundMessageSize(MAX_MESSAGE_SIZE).build()
  private val isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode
  private val deadlineTest
    get() = project.getUserData(DEADLINE_TEST_KEY) ?: DEADLINE_TEST_DEFAULT

  internal val stub = RPIServiceGrpc.newBlockingStub(channel).let {
    if (isUnitTestMode) it.withDeadline(Deadline.after(deadlineTest, TimeUnit.SECONDS)) else it
  }
  internal val asyncStub = RPIServiceGrpc.newFutureStub(channel).let {
    if (isUnitTestMode) it.withDeadline(Deadline.after(deadlineTest, TimeUnit.SECONDS)) else it
  }
  val executor = ConcurrencyUtil.newSingleThreadExecutor(RINTEROP_THREAD_NAME)
  private val heartbeatTimer: Timer
  private val asyncEventsListeners = Collections.newSetFromMap<AsyncEventsListener>(ConcurrentHashMap())
  private var asyncProcessingStarted = false
  private val asyncEventsBeforeStarted = mutableListOf<AsyncEvent>()
  private val cacheIndex = AtomicInteger(0)
  private val dataFrameViewerCache = ConcurrentHashMap<Int, RDataFrameViewer>()
  internal val sourceFileManager = RSourceFileManager(this)

  val rInteropGrpcLogger = RInteropGrpcLogger(if (ApplicationManager.getApplication().isInternal) null else GRPC_LOGGER_MAX_MESSAGES)

  val globalEnvRef = RReference(RRef.newBuilder().setGlobalEnv(Empty.getDefaultInstance()).build(), this)
  val globalEnvLoader = globalEnvRef.createVariableLoader()
  val globalEnvEqualityObject = globalEnvRef.getEqualityObject()
  val currentEnvRef = RReference(RRef.newBuilder().setCurrentEnv(Empty.getDefaultInstance()).build(), this)
  val currentEnvLoader = currentEnvRef.createVariableLoader()
  @Volatile var isDebug = false
    private set
  @Volatile var debugStack: List<RStackFrame> = emptyList()
    private set
  @Volatile var lastErrorStack: List<RStackFrame> = emptyList()
    private set

  private val terminationPromise = AsyncPromise<Unit>()
    .also { it.onSuccess { fireListeners { listener -> listener.onTermination() } } }
  val isAlive: Boolean
    get() = !terminationPromise.isDone
  @Volatile
  internal var killedByUsed = false

  val state: RInterpreterState = RInterpreterStateImpl(project, this)
  fun updateState() = state.updateState()

  internal fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> executeAsync(
    f: KFunction1<Request, ListenableFuture<Response>>,
    request: Request
  ) : CancellablePromise<Response> {
    val nextStubNumber = rInteropGrpcLogger.nextStubNumber()
    rInteropGrpcLogger.onStubMessageRequest(nextStubNumber, request, f.name)
    val promise = AsyncPromise<Response>()
    val future = f.invoke(request)
    promise.onError { future.cancel(true) }
    future.addListener(Runnable {
      val result = try {
        future.get()
      } catch (e: Throwable) {
        promise.setError(processError(e, f.name))
        return@Runnable
      }
      promise.setResult(result)
      rInteropGrpcLogger.onStubMessageResponse(nextStubNumber, result)
    }, MoreExecutors.directExecutor())
    return promise
  }

  internal fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> execute(
    f: KFunction1<Request, ListenableFuture<Response>>, request: Request) : Response {
    try {
      return executeAsync(f, request).blockingGet(Int.MAX_VALUE)!!
    } catch (e: ExecutionException) {
      throw (e.cause as? RInteropException) ?: e
    }
  }

  internal fun <Request : GeneratedMessageV3, Response : GeneratedMessageV3> executeWithCheckCancel(
    f: KFunction1<Request, ListenableFuture<Response>>,
    request: Request) : Response {
    return executeAsync(f, request).getWithCheckCanceled()
  }

  val workingDir: String by Cached("") {
    executeWithCheckCancel(asyncStub::getWorkingDir, Empty.getDefaultInstance()).value
  }

  val loadedPackages = AsyncCached<Map<String, Int>>(emptyMap()) {
    executeAsync(asyncStub::loaderGetLoadedNamespaces, Empty.getDefaultInstance()).thenCancellable {
      it.listList.mapIndexed { index, s -> s to index }.toMap().also {
        project.messageBus.syncPublisher(LOADED_LIBRARIES_UPDATED).onLibrariesUpdated()
      }
    }
  }

  val rMarkdownChunkOptions: List<String> by Cached(emptyList()) {
    executeWithCheckCancel(asyncStub::getRMarkdownChunkOptions, Empty.getDefaultInstance()).listList
  }

  private val getInfoResponse = execute(asyncStub::getInfo, Empty.getDefaultInstance())
  val rVersion = RVersion.forceParse(getInfoResponse.rVersion)
  var workspaceFile: String? = null
    private set
  private var saveOnExitValue = false
  var saveOnExit: Boolean
    get() = saveOnExitValue
    set(value) {
      if (saveOnExitValue != value && workspaceFile != null) {
        executeAsync(asyncStub::setSaveOnExit, BoolValue.of(value))
        saveOnExitValue = value
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

  fun init(rScriptsPath: String, baseDir: String, workspaceFile: String? = null) {
    val initRequest = Init.newBuilder()
    this.workspaceFile = workspaceFile?.also {
      val loadWorkspace: Boolean
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        val settings = RSettings.getInstance(project)
        loadWorkspace = settings.loadWorkspace
        saveOnExitValue = settings.saveWorkspace
      } else {
        loadWorkspace = true
        saveOnExitValue = true
      }
      initRequest.setWorkspaceFile(it).setLoadWorkspace(loadWorkspace).setSaveOnExit(saveOnExit)
    }
    initRequest.setRScriptsPath(rScriptsPath).projectDir = baseDir
    initRequest.httpUserAgent = "Rkernel/" + ApplicationInfo.getInstance().build.asStringWithoutProductCode()

    val initOutput = executeRequest(RPIServiceGrpc.getInitMethod(), initRequest.build())
    if (initOutput.stdout.isNotBlank()) {
      LOG.warn(initOutput.stdout)
    }
    if (initOutput.stderr.isNotBlank()) {
      LOG.warn(initOutput.stderr)
    }
  }

  fun executeTask(f: () -> Unit): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    executor.execute {
      try {
        f()
        promise.setResult(Unit)
      } catch (ignored: RInteropTerminated) {
        promise.setResult(Unit)
      } catch (e: Throwable) {
        promise.setError(e)
      }
    }
    return promise
  }

  fun setWorkingDir(dir: String) {
    try {
      executeWithCheckCancel(asyncStub::setWorkingDir, StringValue.of(dir))
      invalidateCaches()
    } catch (ignored: RInteropTerminated) {
    }
  }

  fun isLibraryLoaded(name: String): Boolean {
    return loadedPackages.value.keys.contains(name)
  }

  fun loadLibrary(name: String): CancellablePromise<Unit> {
    return executeAsync(asyncStub::loadLibrary, StringValue.of(name)).thenCancellable {
      invalidateCaches()
    }
  }

  fun unloadLibrary(name: String, withDynamicLibrary: Boolean): CancellablePromise<Unit> {
    val request = UnloadLibraryRequest.newBuilder()
      .setWithDynamicLibrary(withDynamicLibrary)
      .setPackageName(name)
      .build()
    return executeAsync(asyncStub::unloadLibrary, request).thenCancellable {
      invalidateCaches()
    }
  }

  fun saveGlobalEnvironment(filename: String): CancellablePromise<Empty> =
    executeAsync(asyncStub::saveGlobalEnvironment, StringValue.of(filename))

  fun loadEnvironment(filename: String, variableName: String): CancellablePromise<Unit> {
    val request = LoadEnvironmentRequest.newBuilder()
      .setFile(filename)
      .setVariable(variableName)
      .build()
    return executeAsync(asyncStub::loadEnvironment, request).thenCancellable {
      invalidateCaches()
    }
  }

  fun setOutputWidth(width: Int) = executeTask {
    execute(asyncStub::setOutputWidth, Int32Value.of(width))
  }

  fun replExecute(code: String, setLastValue: Boolean = false, isDebug: Boolean = false): CancellablePromise<RIExecutionResult> {
    return executeCodeImpl(code, isRepl = true, setLastValue = setLastValue, isDebug = isDebug)
  }

  /**
   * Do not use in production code
   */
  @TestOnly
  fun executeCode(code: String, withCheckCancelled: Boolean = false): RIExecutionResult {
    val promise = executeCodeImpl(code)
    return if (withCheckCancelled) {
      promise.getWithCheckCanceled()
    } else {
      promise.blockingGet(Int.MAX_VALUE)!!
    }
  }

  fun replSourceFile(file: VirtualFile, debug: Boolean = false, textRange: TextRange? = null,
                     firstDebugCommand: ExecuteCodeRequest.DebugCommand = ExecuteCodeRequest.DebugCommand.CONTINUE,
                     setLastValue: Boolean = false,
                     consumer: ((String, ProcessOutputType) -> Unit)? = null): CancellablePromise<RIExecutionResult> {
    var code = ""
    var lineOffset = -1
    runReadAction {
      val document = FileDocumentManager.getInstance().getDocument(file)
                     ?: return@runReadAction
      code = textRange?.let { document.getText(it) } ?: document.text
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
      firstDebugCommand = firstDebugCommand,
      setLastValue = setLastValue,
      outputConsumer = consumer
    )
  }

  fun executeCodeAsync(
    code: String, withEcho: Boolean = true, isRepl: Boolean = false, returnOutput: Boolean = !isRepl,
    isDebug: Boolean = false, setLastValue: Boolean = false,
    outputConsumer: ((String, ProcessOutputType) -> Unit)? = null): CancellablePromise<RIExecutionResult> {
    return executeCodeImpl(
      code,
      withEcho = withEcho,
      isRepl = isRepl,
      returnOutput = returnOutput,
      isDebug = isDebug,
      setLastValue = setLastValue,
      outputConsumer = outputConsumer)
  }

  private fun executeCodeImpl(
    code: String, withEcho: Boolean = true, sourceFileId: String = "", sourceFileLineOffset: Int = 0, isRepl: Boolean = false,
    returnOutput: Boolean = !isRepl, isDebug: Boolean = false,
    firstDebugCommand: ExecuteCodeRequest.DebugCommand = ExecuteCodeRequest.DebugCommand.CONTINUE,
    setLastValue: Boolean = false,
    outputConsumer: ((String, ProcessOutputType) -> Unit)? = null):
    CancellablePromise<RIExecutionResult> {
    val request = ExecuteCodeRequest.newBuilder()
      .setCode(code)
      .setSourceFileId(sourceFileId)
      .setSourceFileLineOffset(sourceFileLineOffset)
      .setWithEcho(withEcho)
      .setStreamOutput(returnOutput || outputConsumer != null)
      .setIsRepl(isRepl)
      .setIsDebug(isDebug)
      .setSetLastValue(setLastValue)
      .setFirstDebugCommand(firstDebugCommand)
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
      ClientCalls.asyncServerStreamingCall(call, request, object : StreamObserver<ExecuteCodeResponse> {
        override fun onNext(value: ExecuteCodeResponse) {
          when (value.msgCase) {
            ExecuteCodeResponse.MsgCase.OUTPUT -> {
              rInteropGrpcLogger.onOutputAvailable(number, value.output)
              when (value.output.type) {
                CommandOutput.Type.STDOUT -> {
                  outputConsumer?.invoke(value.output.text.toStringUtf8(), ProcessOutputType.STDOUT)
                  if (returnOutput) stdoutBuffer.append(value.output.text.toStringUtf8())
                }
                CommandOutput.Type.STDERR -> {
                  outputConsumer?.invoke(value.output.text.toStringUtf8(), ProcessOutputType.STDERR)
                  if (returnOutput) stderrBuffer.append(value.output.text.toStringUtf8())
                }
                else -> {}
              }
            }
            ExecuteCodeResponse.MsgCase.EXCEPTION -> {
              exception = value.exception
            }
            else -> {}
          }
        }

        override fun onError(t: Throwable?) {
          t?.let { processError(t, "executeCode") }
          promise.setResult(RIExecutionResult(stdoutBuffer.toString(), stderrBuffer.toString(), exception))
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
    execute(asyncStub::sendReadLn, StringValue.of(s))
  }

  fun replSendEof() = executeTask {
    execute(asyncStub::sendEof, Empty.getDefaultInstance())
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
    val position = SourcePosition.newBuilder()
      .setFileId(sourceFileManager.getFileId(file)).setLine(line).build()
    execute(asyncStub::debugAddBreakpoint, DebugAddBreakpointRequest.newBuilder()
      .setPosition(position)
      .setSuspend(suspend)
      .setEvaluateAndLog(evaluateAndLog ?: "")
      .setCondition(condition ?: "")
      .build()
    )
  }

  fun debugRemoveBreakpoint(file: VirtualFile, line: Int) = executeTask {
    execute(asyncStub::debugRemoveBreakpoint, SourcePosition.newBuilder()
      .setFileId(sourceFileManager.getFileId(file)).setLine(line).build())
  }

  fun debugCommandContinue() = executeTask {
    execute(asyncStub::debugCommandContinue, Empty.getDefaultInstance())
  }

  fun debugCommandPause() = executeTask {
    execute(asyncStub::debugCommandPause, Empty.getDefaultInstance())
  }

  fun debugCommandStop() = executeTask {
    execute(asyncStub::debugCommandStop, Empty.getDefaultInstance())
  }

  fun debugCommandStepOver() = executeTask {
    execute(asyncStub::debugCommandStepOver, Empty.getDefaultInstance())
  }

  fun debugCommandStepInto() = executeTask {
    execute(asyncStub::debugCommandStepInto, Empty.getDefaultInstance())
  }

  fun debugCommandForceStepInto() = executeTask {
    execute(asyncStub::debugCommandForceStepInto, Empty.getDefaultInstance())
  }

  fun debugCommandStepOut() = executeTask {
    execute(asyncStub::debugCommandStepOut, Empty.getDefaultInstance())
  }

  fun debugCommandRunToPosition(position: RSourcePosition) = executeTask {
    execute(asyncStub::debugCommandRunToPosition, SourcePosition.newBuilder()
      .setFileId(sourceFileManager.getFileId(position.file))
      .setLine(position.line)
      .build())
  }

  fun debugMuteBreakpoints(mute: Boolean) = executeTask {
    execute(asyncStub::debugMuteBreakpoints, BoolValue.of(mute))
  }

  fun graphicsInit(parameters: RGraphicsUtils.ScreenParameters, inMemory: Boolean): RIExecutionResult {
    val screenParametersMessage = buildScreenParametersMessage(parameters)
    val request = GraphicsInitRequest.newBuilder()
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
    val request = GraphicsRescaleRequest.newBuilder()
      .setSnapshotNumber(snapshotNumber ?: -1)
      .setNewParameters(newParametersMessage)
      .build()
    return executeRequest(RPIServiceGrpc.getGraphicsRescaleMethod(), request)
  }

  fun graphicsRescaleStored(
    groupId: String,
    snapshotNumber: Int,
    snapshotVersion: Int,
    newParameters: RGraphicsUtils.ScreenParameters
  ): RIExecutionResult {
    val newParametersMessage = buildScreenParametersMessage(newParameters)
    val request = GraphicsRescaleStoredRequest.newBuilder()
      .setGroupId(FileUtil.toSystemIndependentName(groupId))
      .setSnapshotNumber(snapshotNumber)
      .setSnapshotVersion(snapshotVersion)
      .setNewParameters(newParametersMessage)
      .build()
    return executeRequest(RPIServiceGrpc.getGraphicsRescaleStoredMethod(), request)
  }

  private fun buildScreenParametersMessage(parameters: RGraphicsUtils.ScreenParameters): ScreenParameters {
    val scaled = RGraphicsUtils.scaleForRetina(parameters)
    return ScreenParameters.newBuilder()
      .setResolution(scaled.resolution ?: -1)
      .setHeight(scaled.height)
      .setWidth(scaled.width)
      .build()
  }

  fun graphicsPullChangedNumbers(): List<Int> {
    val response = executeWithCheckCancel(asyncStub::graphicsPullChangedNumbers, Empty.getDefaultInstance())
    if (response.message.isNotBlank()) {
      throw RuntimeException(response.message)
    }
    return response.valueList
  }

  class GraphicsPullResponse(val name: String, val content: ByteArray, val recorded: ByteArray? = null)

  fun graphicsPullInMemorySnapshot(number: Int, withRecorded: Boolean): GraphicsPullResponse {
    return graphicsPullSnapshot(number, withRecorded = withRecorded)
  }

  fun graphicsPullStoredSnapshot(number: Int, groupId: String): GraphicsPullResponse {
    return graphicsPullSnapshot(number, groupId = groupId)
  }

  private fun graphicsPullSnapshot(number: Int, groupId: String = "", withRecorded: Boolean = false): GraphicsPullResponse {
    val request = GraphicsPullSnapshotRequest.newBuilder()
      .setWithRecorded(withRecorded)
      .setSnapshotNumber(number)
      .setGroupId(FileUtil.toSystemIndependentName(groupId))
      .build()
    val response = executeWithCheckCancel(asyncStub::graphicsPullSnapshot, request)
    if (response.message.isNotBlank()) {
      throw RuntimeException(response.message)
    }
    val recordedContent = response.recorded.takeIf { !it.isEmpty }?.toByteArray()
    return GraphicsPullResponse(response.snapshotName, response.content.toByteArray(), recordedContent)
  }

  fun graphicsPushSnapshot(groupId: String, number: Int, recorded: ByteArray) {
    val request = GraphicsPushSnapshotRequest.newBuilder()
      .setRecorded(ByteString.copyFrom(recorded))
      .setSnapshotNumber(number)
      .setGroupId(FileUtil.toSystemIndependentName(groupId))
      .build()
    val response = executeWithCheckCancel(asyncStub::graphicsPushSnapshot, request)
    if (response.value.isNotBlank()) {
      throw RuntimeException(response.value)
    }
  }

  fun graphicsCreateGroup(): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getGraphicsCreateGroupMethod(), Empty.getDefaultInstance())
  }

  fun graphicsRemoveGroup(groupId: String): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getGraphicsRemoveGroupMethod(), StringValue.of(FileUtil.toSystemIndependentName(groupId)))
  }

  fun graphicsShutdown(): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getGraphicsShutdownMethod(), Empty.getDefaultInstance())
  }

  data class HttpdResponse(val content: String, val url: String)

  fun httpdRequest(url: String): HttpdResponse? {
    return try {
      executeWithCheckCancel(asyncStub::httpdRequest, StringValue.of(url))
        .takeIf { it.success }
        ?.let { HttpdResponse(it.content, it.url) }
    } catch (e: RInteropTerminated) {
      null
    }
  }

  fun getDocumentationForPackage(packageName: String): CancellablePromise<HttpdResponse?> {
    return executeAsync(asyncStub::getDocumentationForPackage, StringValue.of(packageName)).thenCancellable {
      if (it.success) HttpdResponse(it.content, it.url) else null
    }
  }

  fun getDocumentationForSymbol(symbol: String, packageName: String? = null): CancellablePromise<HttpdResponse?> {
    val request = DocumentationForSymbolRequest.newBuilder().setSymbol(symbol).setPackage(packageName.orEmpty()).build()
    return executeAsync(asyncStub::getDocumentationForSymbol, request).thenCancellable {
      if (it.success) HttpdResponse(it.content, it.url) else null
    }
  }

  fun runBeforeChunk(rmarkdownParameters: String, chunkText: String): RIExecutionResult {
    val request = ChunkParameters.newBuilder()
      .setRmarkdownParameters(rmarkdownParameters)
      .setChunkText(chunkText)
      .build()
    return executeRequest(RPIServiceGrpc.getBeforeChunkExecutionMethod(), request)
  }

  fun runAfterChunk(): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getAfterChunkExecutionMethod(), Empty.getDefaultInstance())
  }

  fun pullChunkOutputRelativePaths(): List<String> {
    return executeWithCheckCancel(asyncStub::pullChunkOutputRelativePaths, Empty.getDefaultInstance()).listList
  }

  fun pullChunkOutputFile(relativePath: String): ByteArray {
    val response = executeWithCheckCancel(asyncStub::pullChunkOutputFile, StringValue.of(FileUtil.toSystemIndependentName(relativePath)))
    if (response.message.isNotBlank()) {
      throw RuntimeException(response.message)
    }
    return response.content.toByteArray()
  }

  fun repoGetPackageVersion(packageName: String): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getRepoGetPackageVersionMethod(), StringValue.of(packageName))
  }

  fun repoInstallPackage(packageName: String, fallbackMethod: String?, arguments: Map<String, String>) {
    val request = RepoInstallPackageRequest.newBuilder()
      .setFallbackMethod(fallbackMethod ?: "")
      .setPackageName(packageName)
      .putAllArguments(arguments)
      .build()
    execute(asyncStub::repoInstallPackage, request)
  }

  fun getSysEnv(envName: String, vararg flags: String): List<String> {
    return try {
      val request = GetSysEnvRequest.newBuilder().setEnvName(envName).addAllFlags(flags.toList()).build()
      executeWithCheckCancel(asyncStub::getSysEnv, request).listList
    }
    catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun loadLibPaths(): List<RInterpreterState.LibraryPath> {
    return try {
      executeWithCheckCancel(asyncStub::loadLibPaths, Empty.getDefaultInstance()).libPathsList.map {
        RInterpreterState.LibraryPath(it.path, it.isWritable)
      }
    }
    catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun loadInstalledPackages(): List<RInstalledPackage> {
    return try {
      val obtained = executeWithCheckCancel(asyncStub::loadInstalledPackages, Empty.getDefaultInstance()).packagesList.asSequence().map {
        val priority = when (it.priority) {
          RInstalledPackageList.RInstalledPackage.RPackagePriority.BASE -> RPackagePriority.BASE
          RInstalledPackageList.RInstalledPackage.RPackagePriority.RECOMMENDED -> RPackagePriority.RECOMMENDED
          else -> RPackagePriority.NA
        }
        val description = it.descriptionList.map { entry -> entry.key to entry.value }.toMap()
        RInstalledPackage(it.packageName, it.packageVersion, priority, it.libraryPath, description)
      }
      // Obtained sequence contains duplicates of the same packages but for different versions.
      // The ones which will be used by R's functions go first.
      // Also it's not sorted by package names
      val name2Packages = TreeMap<String, RInstalledPackage>(String.CASE_INSENSITIVE_ORDER)
      for (rPackage in obtained) {
        name2Packages.putIfAbsent(rPackage.packageName, rPackage)
      }
      name2Packages.values.toList()
    }
    catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun repoAddLibraryPath(path: String): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getRepoAddLibraryPathMethod(), StringValue.of(path))
  }

  fun repoCheckPackageInstalled(packageName: String): RIExecutionResult {
    return executeRequest(RPIServiceGrpc.getRepoCheckPackageInstalledMethod(), StringValue.of(packageName))
  }

  fun repoRemovePackage(packageName: String, libraryPath: String) {
    val request = RepoRemovePackageRequest.newBuilder()
      .setPackageName(packageName)
      .setLibraryPath(libraryPath)
      .build()
    execute(asyncStub::repoRemovePackage, request)
  }

  fun previewDataImport(path: String, mode: String, rowCount: Int, additional: Map<String, String>): RIExecutionResult {
    val request = PreviewDataImportRequest.newBuilder()
      .setPath(FileUtil.toSystemIndependentName(path))
      .putAllOptions(additional)
      .setRowCount(rowCount)
      .setMode(mode)
      .build()
    return executeRequest(RPIServiceGrpc.getPreviewDataImportMethod(), request)
  }

  fun commitDataImport(name: String, path: String, mode: String, additional: Map<String, String>) {
    val request = CommitDataImportRequest.newBuilder()
      .setPath(FileUtil.toSystemIndependentName(path))
      .putAllOptions(additional)
      .setMode(mode)
      .setName(name)
      .build()
    execute(asyncStub::commitDataImport, request)
  }

  fun dataFrameGetViewer(ref: RReference): Promise<RDataFrameViewer> {
    try {
      RDataFrameViewerImpl.ensureDplyrInstalled(project)
    } catch (e: RequiredPackageException) {
       return rejectedPromise(e)
    }
    return executeAsync(asyncStub::dataFrameRegister, ref.proto).thenCancellable {
      val index = it.value
      if (index == -1) {
        throw RDataFrameException("Invalid data frame")
      }
      dataFrameViewerCache.getOrPut(index) {
        val persistentRef = RPersistentRef(index, this)
        Disposer.register(persistentRef, Disposable {
          dataFrameViewerCache.remove(index)
          if (isAlive) execute(asyncStub::dataFrameDispose, Int32Value.of(index))
        })
        val viewer = RDataFrameViewerImpl(persistentRef)
        viewer
      }
    }
  }

  fun dataFrameGetInfo(ref: RReference): DataFrameInfoResponse {
    return executeWithCheckCancel(asyncStub::dataFrameGetInfo, ref.proto)
  }

  fun dataFrameGetData(ref: RReference, start: Int, end: Int): Promise<DataFrameGetDataResponse> {
    val request = DataFrameGetDataRequest.newBuilder().setRef(ref.proto).setStart(start).setEnd(end).build()
    return executeAsync(asyncStub::dataFrameGetData, request)
  }

  fun dataFrameSort(ref: RReference, sortKeys: List<RowSorter.SortKey>, disposableParent: Disposable? = null): RPersistentRef {
    val keysProto = sortKeys.map {
      DataFrameSortRequest.SortKey.newBuilder()
        .setColumnIndex(it.column)
        .setDescending(it.sortOrder == SortOrder.DESCENDING)
        .build()
    }
    val request = DataFrameSortRequest.newBuilder().setRef(ref.proto).addAllKeys(keysProto).build()
    return RPersistentRef(executeWithCheckCancel(asyncStub::dataFrameSort, request).value, this, disposableParent)
  }

  fun dataFrameFilter(ref: RReference, f: DataFrameFilterRequest.Filter, disposableParent: Disposable? = null): RPersistentRef {
    val request = DataFrameFilterRequest.newBuilder().setRef(ref.proto).setFilter(f).build()
    return RPersistentRef(executeWithCheckCancel(asyncStub::dataFrameFilter, request).value, this, disposableParent)
  }

  fun findInheritorNamedArguments(function: RReference): List<String> {
    return try {
      executeWithCheckCancel(asyncStub::findInheritorNamedArguments, function.proto).listList
    } catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun findExtraNamedArguments(function: RReference): RExtraNamedArgumentsInfo {
    return try {
      val res = executeWithCheckCancel(asyncStub::findExtraNamedArguments, function.proto)
      RExtraNamedArgumentsInfo(res.argNamesList, res.funArgNamesList)
    } catch (e: RInteropTerminated) {
      RExtraNamedArgumentsInfo(emptyList(), emptyList())
    }
  }

  fun getFormalArguments(function: RReference): List<String> {
    return try {
      executeWithCheckCancel(asyncStub::getFormalArguments, function.proto).listList
    } catch (e: RInteropTerminated) {
      emptyList()
    }
  }

  fun getTableColumnsInfo(table: RReference): TableInfo {
    return try {
      val request = TableColumnsInfoRequest.newBuilder().setRef(table.proto).build()
      executeWithCheckCancel(asyncStub::getTableColumnsInfo, request).run {
        TableInfo(columnsList.map { TableManipulationColumn(it.name, it.type) }, TableType.toTableType(tableType))
      }
    } catch (e: RInteropTerminated) {
      TableInfo(emptyList(), TableType.UNKNOWN)
    }
  }

  fun convertRoxygenToHTML(functionName: String, functionText: String): RIExecutionResult {
    val result = executeWithCheckCancel(asyncStub::convertRoxygenToHTML,
                                        ConvertRoxygenToHTMLRequest.newBuilder()
                                          .setFunctionName(functionName)
                                          .setFunctionText(functionText)
                                          .build())
    return if (result.resultCase == ConvertRoxygenToHTMLResponse.ResultCase.TEXT) {
      RIExecutionResult(result.text, "", null)
    } else {
      RIExecutionResult("", "", result.error)
    }
  }

  fun clearEnvironment(env: RReference) {
    try {
      executeWithCheckCancel(asyncStub::clearEnvironment, env.proto)
      invalidateCaches()
    } catch (ignored: RInteropTerminated) {
    }
  }

  fun getObjectSizes(refs: List<RReference>): List<Long> {
    return execute(asyncStub::getObjectSizes, RRefList.newBuilder().addAllRefs(refs.map { it.proto }).build()).listList
  }

  private fun <TRequest : GeneratedMessageV3> executeRequest(
    methodDescriptor: MethodDescriptor<TRequest, CommandOutput>,
    request: TRequest
  ) : RIExecutionResult {
    val isEdt = !ApplicationManager.getApplication().isUnitTestMode && ApplicationManager.getApplication().isDispatchThread
    check(!isEdt) { "Waiting on dispatch thread is not allowed" }
    val withCheckCancelled = ApplicationManager.getApplication().isReadAccessAllowed
    val stdoutBuffer = StringBuilder()
    val stderrBuffer = StringBuilder()
    val promise = executeRequestAsync(methodDescriptor, request) { text, type ->
      when (type) {
        ProcessOutputType.STDOUT -> stdoutBuffer.append(text)
        ProcessOutputType.STDERR -> stderrBuffer.append(text)
      }
    }
    if (withCheckCancelled) {
      promise.getWithCheckCanceled()
    } else {
      promise.blockingGet(Int.MAX_VALUE)
    }
    return RIExecutionResult(stdoutBuffer.toString(), stderrBuffer.toString())
  }

  private fun <TRequest : GeneratedMessageV3> executeRequestAsync(
    methodDescriptor: MethodDescriptor<TRequest, CommandOutput>,
    request: TRequest,
    consumer: ((String, ProcessOutputType) -> Unit)? = null
  ): CancellablePromise<Unit> {
    val number = rInteropGrpcLogger.nextStubNumber()
    rInteropGrpcLogger.onExecuteRequestAsync(number, methodDescriptor, request)
    val callOptions = if (isUnitTestMode) CallOptions.DEFAULT.withDeadlineAfter(deadlineTest, TimeUnit.SECONDS) else CallOptions.DEFAULT
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
    ClientCalls.asyncServerStreamingCall(call, request, object : StreamObserver<CommandOutput> {
      override fun onNext(value: CommandOutput) {
        rInteropGrpcLogger.onOutputAvailable(number, value)
        if (consumer == null) return
        when (value.type) {
          CommandOutput.Type.STDOUT -> consumer(value.text.toStringUtf8(), ProcessOutputType.STDOUT)
          CommandOutput.Type.STDERR -> consumer(value.text.toStringUtf8(), ProcessOutputType.STDERR)
          else -> {
          }
        }
      }

      override fun onError(t: Throwable?) {
        val e = t?.let { processError(it, methodDescriptor.fullMethodName) }
        if (e is RInteropTerminated) {
          consumer?.invoke(RBundle.message("rinterop.terminated"), ProcessOutputType.STDERR)
        }
        promise.setResult(Unit)
      }

      override fun onCompleted() {
        rInteropGrpcLogger.onExecuteRequestFinish(number)
        promise.setResult(Unit)
      }
    })
    return promise
  }

  private fun processAsyncEvent(event: AsyncEvent) {
    when (event.eventCase) {
      AsyncEvent.EventCase.BUSY -> {
        fireListeners { it.onBusy() }
      }
      AsyncEvent.EventCase.TEXT -> {
        val text = event.text.text.toStringUtf8()
        val type = when (event.text.type) {
          CommandOutput.Type.STDOUT -> ProcessOutputType.STDOUT
          CommandOutput.Type.STDERR -> ProcessOutputType.STDERR
          else -> return
        }
        fireListeners { it.onText(text, type) }
      }
      AsyncEvent.EventCase.REQUESTREADLN -> {
        invalidateCaches()
        val prompt = event.requestReadLn.prompt
        fireListeners { it.onRequestReadLn(prompt) }
      }
      AsyncEvent.EventCase.SUBPROCESSINPUT -> {
        fireListeners { it.onSubprocessInput() }
      }
      AsyncEvent.EventCase.BROWSEURLREQUEST -> {
        fireListeners { it.onBrowseURLRequest(event.browseURLRequest) }
      }
      AsyncEvent.EventCase.PROMPT -> {
        invalidateCaches()
        isDebug = false
        debugStack = emptyList()
        fireListeners { it.onPrompt() }
      }
      AsyncEvent.EventCase.DEBUGPROMPT -> {
        invalidateCaches()
        isDebug = true
        if (event.debugPrompt.changed) {
          debugStack = stackFromProto(event.debugPrompt.stack)
        }
        fireListeners { it.onPrompt(true) }
      }
      AsyncEvent.EventCase.EXCEPTION -> {
        if (!event.exception.exception.hasInterrupted()) {
          lastErrorStack = stackFromProto(event.exception.stack) { RReference.errorStackSysFrameRef(it, this) }
        }
        val info = exceptionInfoFromProto(event.exception.exception)
        fireListeners { it.onException(info) }
      }
      AsyncEvent.EventCase.VIEWREQUEST -> {
        val ref = RPersistentRef(event.viewRequest.persistentRefIndex, this)
        fireListenersAsync({ it.onViewRequest(ref, event.viewRequest.title, ProtoUtil.rValueFromProto(event.viewRequest.value)) }) {
          Disposer.dispose(ref)
          executeAsync(asyncStub::clientRequestFinished, Empty.getDefaultInstance())
        }
      }
      AsyncEvent.EventCase.SHOWFILEREQUEST -> {
        val request = event.showFileRequest
        fireListenersAsync({it.onShowFileRequest(request.filePath, request.title, request.content.toByteArray()) }) {
          executeAsync(asyncStub::clientRequestFinished, Empty.getDefaultInstance())
        }
      }
      AsyncEvent.EventCase.SHOWHELPREQUEST -> {
        val httpdResponse = event.showHelpRequest.takeIf { it.success }?.let { HttpdResponse(it.content, it.url) } ?: return
        fireListeners { it.onShowHelpRequest(httpdResponse) }
      }
      else -> {
      }
    }
  }

  private fun fireListenersAsync(f: (AsyncEventsListener) -> Promise<Unit>, end: () -> Unit) {
    if (asyncEventsListeners.isEmpty()) {
      end()
      return
    }
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
    executeAsync(asyncStub::getNextAsyncEvent, Empty.getDefaultInstance()).onSuccess { event ->
      executeTask {
        if (asyncProcessingStarted) {
          processAsyncEvent(event)
        } else {
          asyncEventsBeforeStarted.add(event)
        }
        if (event.hasTermination()) {
          heartbeatTimer.cancel()
          terminationPromise.setResult(Unit)
          executeAsync(asyncStub::quitProceed, Empty.getDefaultInstance())
        }
      }
      if (!event.hasTermination()) {
        processAsyncEvents()
      }
    }.onError {
      when (it) {
        is CancellationException -> {
          processAsyncEvents()
        }
        is RInteropRequestFailed -> {
          LOG.error(it)
          processAsyncEvents()
        }
        else -> {
          heartbeatTimer.cancel()
          terminationPromise.setResult(Unit)
        }
      }
    }
  }

  private fun stackFromProto(proto: StackFrameList,
                             indexToEnvironment: (Int) -> RReference = { RReference.sysFrameRef(it, this) }): List<RStackFrame> {
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
      try {
        if (!processHandler.isProcessTerminated) {
          terminationPromise.blockingGet(20000)
        }
        channel.shutdown()
        channel.awaitTermination(1000, TimeUnit.MILLISECONDS)
        processHandler.waitFor(5000)
        executor.shutdown()
      } catch (ignored: TimeoutException) {
      } finally {
        processHandler.destroyProcess()
      }
    } else {
      ProgressManager.getInstance().run(object : Task.Backgroundable(null, RBundle.message("rinterop.terminating.title"), true) {
        override fun run(indicator: ProgressIndicator) {
          try {
            indicator.isIndeterminate = true
            terminationPromise.getWithCheckCanceled()
            executeTask {
              channel.shutdown()
              channel.awaitTermination(1000, TimeUnit.MILLISECONDS)
              if (!processHandler.waitFor(2000)) {
                processHandler.destroyProcess()
              }
              executor.shutdown()
            }
          } catch (e: ProcessCanceledException) {
            killedByUsed = true
            terminationPromise.setResult(Unit)
            processHandler.destroyProcess()
            executor.shutdown()
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

  private fun processError(e: Throwable, methodName: String): Throwable {
    (e as? ExecutionException)?.cause?.let { return processError(it, methodName) }
    if (!isAlive) return RInteropTerminated(this)
    if (e is StatusRuntimeException) {
      val code = e.status.code
      return if (code == Status.Code.UNAVAILABLE || code == Status.Code.INTERNAL) {
        terminationPromise.setResult(Unit)
        RInteropTerminated(this)
      } else {
        RInteropRequestFailed(this, methodName, e)
      }
    }
    return e
  }

  inner class Cached<T>(defaultValue: T? = null, val f: () -> T) {
    private var previousValue = defaultValue ?: f()
    private val cached = object : AtomicClearableLazyValue<T>() {
      override fun compute(): T {
        if (!isAlive) return previousValue
        return try {
          f().also { previousValue = it }
        } catch (e: RInteropTerminated) {
          previousValue
        }
      }
    }
    private var cacheIndex = -1

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
      val currentCacheIndex = this@RInterop.cacheIndex.get()
      if (cacheIndex < currentCacheIndex) {
        cached.drop()
        cacheIndex = currentCacheIndex
      }
      return cached.value
    }
  }

  inner class AsyncCached<T>(defaultValue: T, private val f: () -> CancellablePromise<T>) {
    private var cached: T = defaultValue
    private var cacheIndex = -1
    private var currentPromise: CancellablePromise<T>? = null

    val value: T
      @Synchronized
      get() {
        val currentCacheIndex = this@RInterop.cacheIndex.get()
        if (cacheIndex < currentCacheIndex) {
          cacheIndex = currentCacheIndex
          currentPromise?.cancel()
          val promise = AsyncPromise<T>().also { currentPromise = it }
          f().onSuccess {
            cached = it
            promise.setResult(it)
          }.onError {
            if (it is RInteropTerminated) {
              promise.setResult(cached)
            } else {
              promise.setError(it)
            }
          }
        }
        return cached
      }

    fun getAsync(): Promise<T> {
      value
      return currentPromise ?: resolvedPromise(cached)
    }

    fun getWithCheckCancel(): T {
      val result = value
      currentPromise?.let { return it.getWithCheckCanceled(false) }
      return result
    }
  }

  interface AsyncEventsListener {
    fun onText(text: String, type: ProcessOutputType) {}
    fun onBusy() {}
    fun onRequestReadLn(prompt: String) {}
    fun onPrompt(isDebug: Boolean = false) {}
    fun onException(exception: RExceptionInfo) {}
    fun onTermination() {}
    fun onViewRequest(ref: RReference, title: String, value: RValue): Promise<Unit> = resolvedPromise()
    fun onShowHelpRequest(httpdResponse: HttpdResponse) {}
    fun onShowFileRequest(filePath: String, title: String, content: ByteArray): Promise<Unit> = resolvedPromise()
    fun onSubprocessInput() {}
    fun onBrowseURLRequest(url: String) {}
  }

  companion object {
    private const val HEARTBEAT_PERIOD = 20000
    private const val EXECUTE_CODE_TEST_TIMEOUT = 20000
    private const val GRPC_LOGGER_MAX_MESSAGES = 30
    private const val MAX_MESSAGE_SIZE = 16 * 1024 * 1024  // 16 MiB (default is 4)

    internal val DEADLINE_TEST_KEY = Key<Long>("org.jetbrains.r.rinterop.RInterop.DeadlineTest")
  }
}

internal fun <T> Future<T>.getWithCheckCanceled(cancelOnInterrupt: Boolean = true): T {
  while (true) {
    try {
      ProgressManager.checkCanceled()
      return get(50, TimeUnit.MILLISECONDS)
    } catch (ignored: TimeoutException) {
    } catch (e: InterruptedException) {
      if (cancelOnInterrupt) cancel(true)
      throw e
    } catch (e: ProcessCanceledException) {
      if (cancelOnInterrupt) cancel(true)
      throw e
    } catch (e: ExecutionException) {
      throw (e.cause as? RInteropException) ?: e
    }
  }
}

private val LOG = Logger.getInstance(RInteropUtil.javaClass)

