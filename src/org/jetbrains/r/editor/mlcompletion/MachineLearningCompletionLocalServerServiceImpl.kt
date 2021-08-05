package org.jetbrains.r.editor.mlcompletion

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import org.jetbrains.io.mandatory.NullCheckingFactory
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.withTryLock
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import org.jetbrains.r.settings.MachineLearningCompletionSettingsChangeListener
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MachineLearningCompletionLocalServerServiceImpl : MachineLearningCompletionLocalServerService {

  companion object {
    private val GSON = GsonBuilder().registerTypeAdapterFactory(NullCheckingFactory.INSTANCE).create()
    private val settings = MachineLearningCompletionSettings.getInstance()
    private val completionFilesService = MachineLearningCompletionModelFilesService.getInstance()
    private val LOG = Logger.getInstance(MachineLearningCompletionLocalServerServiceImpl::class.java)
    private const val RELAUNCH_TIMEOUT_MS = 30_000L

    private fun Path.asLaunchCommand(): String =
      when {
        SystemInfo.isWindows -> toString()
        else -> parent.resolve("./$fileName").toString()
      }
  }

  private val serverLock = ReentrantLock()
  @Volatile
  private var localServer: Process? = null
  private var lastRelaunchInitializedTime: Long = MachineLearningCompletionUtils.currentTimeMillis()

  private val serverAddress
    get() = "http://${settings.state.host}:${settings.state.port}"

  init {
    MachineLearningCompletionSettingsChangeListener { beforeState, afterState ->
      val becameDisabled = beforeState.isEnabled && !afterState.isEnabled
      val addressChanged = (beforeState.host != afterState.host || beforeState.port != afterState.port)

      if (becameDisabled || (afterState.isEnabled && addressChanged)) {
        shutdownServer()
      }
    }.subscribe(this)
  }

  override fun shouldAttemptCompletion(): Boolean {
    return settings.state.isEnabled
  }

  override fun sendCompletionRequest(requestData: MachineLearningCompletionHttpRequest)
    : CompletableFuture<MachineLearningCompletionHttpResponse?> =
    CompletableFuture.supplyAsync(
      {
        if (localServer == null) {
          tryLaunchServer()
        }

        try {
          HttpRequests.post(serverAddress, "application/json")
            .connect { request ->
              request.write(GSON.toJson(requestData))
              return@connect GSON.fromJson(request.reader.readText(), MachineLearningCompletionHttpResponse::class.java)
            }
        }
        catch (e: IOException) {
          tryLaunchServer()
          null
        }
        catch (e: JsonParseException) {
          null
        }
      },
      AppExecutorUtil.getAppExecutorService())

  override fun prepareForLocalUpdate() {
    shutdownServer()
    localServer?.waitFor()
  }

  override fun dispose() {
    shutdownServer()
  }

  private fun tryLaunchServer(host: String = settings.state.hostOrDefault(),
                              port: Int = settings.state.port): Unit = serverLock.withTryLock(Unit) {
    if (localServer != null
        && MachineLearningCompletionUtils.currentTimeMillis() - lastRelaunchInitializedTime < RELAUNCH_TIMEOUT_MS) {
      return
    }
    lastRelaunchInitializedTime = MachineLearningCompletionUtils.currentTimeMillis()
    shutdownServer()

    completionFilesService.tryRunActionOnFiles { completionFiles ->
      completionFiles.localServerAppExecutableFile?.let { appFile ->
        try {
          FileUtil.setExecutable(File(appFile))
          val launchCommand = Paths.get(appFile).asLaunchCommand()
          val processBuilder = ProcessBuilder(launchCommand,
                                              "--config=${completionFiles.localServerConfigFile}",
                                              "--host=$host",
                                              "--port=$port")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .directory(File(completionFiles.localServerDirectory!!))
          processBuilder.environment()
            .putAll(MachineLearningCompletionLocalServerVariables.SERVER_ENVIRONMENT)
          localServer = processBuilder.start()
        }
        catch (e: Exception) {
          LOG.warn("Exception has occurred in R ML Completion server thread", e)
        }
      }
    }
  }

  private fun shutdownServer(): Unit = serverLock.withLock {
    localServer?.run {
      descendants().forEach(ProcessHandle::destroy)
      destroy()
    }
    localServer = null
  }
}
