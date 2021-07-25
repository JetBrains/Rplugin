package org.jetbrains.r.editor.mlcompletion

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import org.jetbrains.io.mandatory.NullCheckingFactory
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

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

  private var localServer: Process? = null
  private var lastRelaunchInitializedTime: Long = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS)

  private val serverAddress
    get() = "http://${settings.state.host}:${settings.state.port}"

  override val requestTimeoutMs
    get() = settings.state.requestTimeoutMs

  @Synchronized
  private fun tryRelaunchServer(host: String = settings.state.hostOrDefault(),
                        port: Int = settings.state.port) {
    if (System.currentTimeMillis() - lastRelaunchInitializedTime < RELAUNCH_TIMEOUT_MS) {
      return
    }
    lastRelaunchInitializedTime = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS)
    shutdownServer()
    launchServer(host, port)
  }

  override fun shouldAttemptCompletion(): Boolean {
    return settings.state.isEnabled
  }

  override fun sendCompletionRequest(requestData: MachineLearningCompletionHttpRequest)
    : CompletableFuture<MachineLearningCompletionHttpResponse?> =
    CompletableFuture.supplyAsync(
      {
        try {
          HttpRequests.post(serverAddress, "application/json")
            .connect { request ->
              request.write(GSON.toJson(requestData))
              return@connect GSON.fromJson(request.reader.readText(), MachineLearningCompletionHttpResponse::class.java)
            }
        }
        catch (e: IOException) {
          tryRelaunchServer()
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

  private fun launchServer(host: String, port: Int): Boolean = completionFilesService.tryRunActionOnFiles { completionFiles ->
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

  private fun shutdownServer(): Unit? = localServer?.run {
    descendants().forEach(ProcessHandle::destroy)
    destroy()
  }

  override fun dispose() {
    shutdownServer()
  }
}
