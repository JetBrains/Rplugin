package org.jetbrains.r.interpreter

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbModeTask
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiDocumentManagerImpl
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.common.emptyExpiringList
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropAsyncEventsListener
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

interface RInterpreterState {
  /**
   *  true if skeletons update was performed at least once.
   */
  val isSkeletonInitialized: Boolean

  /** A place where all skeleton-related data will be stored */
  val skeletonsDirectory: String

  val project: Project

  val rInterop: RInterop

  data class LibraryPath(val path: String, val isWritable: Boolean)
  val libraryPaths: List<LibraryPath>

  val skeletonFiles: Set<VirtualFile>

  val installedPackages: ExpiringList<RInstalledPackage>

  val userLibraryPath: String

  val isUpdating: Boolean

  fun getPackageByName(name: String): RInstalledPackage?

  fun getLibraryPathByName(name: String): LibraryPath?

  fun getSkeletonFileByPackageName(name: String): PsiFile?

  fun updateState(): Promise<Unit>

  fun cancelStateUpdating()

  fun markOutdated()

  fun scheduleSkeletonUpdate()

  fun hasPackage(name: String): Boolean {
    return getPackageByName(name) != null
  }
}


  class RInterpreterStateImpl(override val project: Project, override val rInterop: RInterop) : RInterpreterState {
  @Volatile
  override var isSkeletonInitialized: Boolean = false
    private set

  override val skeletonsDirectory: String
    get() = "${PathManager.getSystemPath()}${File.separator}${RSkeletonUtil.SKELETON_DIR_NAME}"

  @Volatile
  private var updatePromise: Promise<Unit>? = null

  @Volatile
  private var skeletonPromise: Promise<Unit>? = null

  private val updateEpoch = AtomicInteger(0)

  private val name2PsiFile = ContainerUtil.createConcurrentSoftKeySoftValueMap<String, PsiFile>()
  private var name2libraryPaths: Map<String, RInterpreterState.LibraryPath> = emptyMap()
  private var name2installedPackages: Map<String, RInstalledPackage> = emptyMap()

  private val interpreter
    get() = rInterop.interpreter

  private val interpreterLocation
    get() = interpreter.interpreterLocation

  override var libraryPaths: List<RInterpreterState.LibraryPath> = emptyList()
    private set
  override var skeletonFiles: Set<VirtualFile> = emptySet()
    private set
    get() {
      val currentSkeletonFiles = field
      if (!currentSkeletonFiles.all { it.isValid }) {
        if (project.isOpen && !project.isDisposed) {
          updateState().onSuccess {
            RInterpreterUtil.updateIndexableSet(project)
          }
        }
        return currentSkeletonFiles.filter { it.isValid }.toSet()
      }
      return currentSkeletonFiles
    }
  override var installedPackages: ExpiringList<RInstalledPackage> = emptyExpiringList()
    private set
  override var userLibraryPath: String = ""
    private set
  override val isUpdating
    get() = updatePromise != null

  override fun getPackageByName(name: String) = name2installedPackages[name]

  override fun getLibraryPathByName(name: String) = name2libraryPaths[name]

  override fun getSkeletonFileByPackageName(name: String): PsiFile? {
    val cached = name2PsiFile[name]
    if (cached != null && cached.isValid) {
      return cached
    }
    val rInstalledPackage = getPackageByName(name) ?: return null
    val virtualFile = RSkeletonUtil.installedPackageToSkeletonFile(skeletonsDirectory, rInstalledPackage) ?: return null
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    if (psiFile != null) {
      name2PsiFile[name] = psiFile
    }
    return psiFile
  }

  @Synchronized
  override fun updateState(): Promise<Unit> {
    return updatePromise ?: createUpdatePromise().also {
      updatePromise = it
    }
  }

  override fun markOutdated() {
    updateEpoch.incrementAndGet() // Expire installedPackages
  }

  @Synchronized
  override fun cancelStateUpdating() {
    markOutdated()
    (updatePromise as? AsyncPromise<Unit>)?.cancel()
  }

  private fun createUpdatePromise(): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    val RInteropAsyncEventsListener = object : RInteropAsyncEventsListener {
      override fun onTermination() {
        promise.setError(IllegalStateException("RInterop had been terminated during state updating"))
      }
    }

    promise.onProcessed {
      rInterop.removeAsyncEventsListener(RInteropAsyncEventsListener)
      resetUpdatePromise()
    }.onError { e ->
      if (!promise.isCancelled) {
        LOG.error("Unable to update state", e)
      }
    }

    rInterop.addAsyncEventsListener(RInteropAsyncEventsListener)
    if (!rInterop.isAlive) {
      rInterop.removeAsyncEventsListener(RInteropAsyncEventsListener)
      return promise.also {
        it.setError(IllegalStateException("RInterop had been terminated before state updating"))
      }
    }

    AppExecutorUtil.getAppExecutorService().execute {
      try {
        doUpdateState()
      }
      catch (e: Throwable) {
        promise.setError(e)
        return@execute
      }
      promise.setResult(Unit)
    }
    return promise
  }

  @Synchronized
  private fun resetUpdatePromise() {
    updatePromise = null
  }

  @Throws(IllegalStateException::class)
  private fun doUpdateState() {
    updateEpoch.incrementAndGet()
    val installedPackages = makeExpiring(rInterop.loadInstalledPackages())
    val name2installedPackages = installedPackages.map { it.name to it }.toMap()
    val (libraryPaths, userLibraryPath) = loadPaths()
    val name2libraryPaths = mapNamesToLibraryPaths(installedPackages, libraryPaths)
    val skeletonFiles = installedPackages.mapNotNull {
      RSkeletonUtil.installedPackageToSkeletonFile(skeletonsDirectory, it)
    }.toSet()
    synchronized(this) {
      name2PsiFile.clear()
      this.installedPackages = installedPackages
      this.name2installedPackages = name2installedPackages
      this.libraryPaths = libraryPaths
      this.userLibraryPath = userLibraryPath
      this.name2libraryPaths = name2libraryPaths
      this.skeletonFiles = skeletonFiles
    }
  }

  @TestOnly
  internal fun copyState(state: RInterpreterStateImpl) {
    synchronized(this) {
      updateEpoch.incrementAndGet()
      name2PsiFile.clear()
      this.installedPackages = state.installedPackages
      this.name2installedPackages = state.name2installedPackages
      this.libraryPaths = state.libraryPaths
      this.userLibraryPath = state.userLibraryPath
      this.name2libraryPaths = state.name2libraryPaths
      this.skeletonFiles = state.skeletonFiles
    }
  }

  private fun mapNamesToLibraryPaths(packages: List<RInstalledPackage>,
                                     libraryPaths: List<RInterpreterState.LibraryPath>): Map<String, RInterpreterState.LibraryPath> {
    return packages.mapNotNull { rPackage ->
      libraryPaths.find { it.path == rPackage.libraryPath }?.let { libraryPath -> Pair(rPackage.name, libraryPath) }
    }.toMap()
  }

  private fun <E> makeExpiring(values: List<E>): ExpiringList<E> {
    val usedUpdateEpoch = updateEpoch.get()
    return ExpiringList(values) {
      usedUpdateEpoch < updateEpoch.get() ||
      interpreterLocation != RInterpreterManager.getInstance(project).interpreterLocation
    }
  }

  private fun getUserPath(): String {
    val lines = rInterop.getSysEnv("R_LIBS_USER", "--normalize-path")
    val firstLine = lines.firstOrNull().orEmpty()
    if (firstLine.isNotBlank()) {
      return firstLine
    }
    else {
      throw RuntimeException("Cannot get user library path")
    }
  }

  private fun loadPaths(): Pair<List<RInterpreterState.LibraryPath>, String> {
    val libraryPaths = loadLibraryPaths().toMutableList()
    val userPath = getUserPath()
    val pathWithInfo = rInterop.interpreter.getGuaranteedWritableLibraryPath(libraryPaths, userPath)
    if (pathWithInfo.isUserDirectoryCreated) {
      rInterop.repoAddLibraryPath(userPath)
      libraryPaths.add(RInterpreterState.LibraryPath(userPath, true))
    }
    return Pair(libraryPaths, userPath)
  }

  private fun loadLibraryPaths(): List<RInterpreterState.LibraryPath> {
    return rInterop.loadLibPaths().toList().also {
      if (it.isEmpty()) {
        throw RuntimeException("Got empty library paths")
      }
    }
  }

  @Synchronized
  override fun scheduleSkeletonUpdate() {
    skeletonPromise ?: createSkeletonPromise().also {
      skeletonPromise = it
    }
  }

  private fun createSkeletonPromise(): Promise<Unit> {
    return doScheduleSkeletonUpdate().onProcessed { resetSkeletonPromise() }
  }

  @Synchronized
  private fun resetSkeletonPromise() {
    skeletonPromise = null
  }

  private fun doScheduleSkeletonUpdate(): Promise<Unit> {
    return updateState().onSuccess {
      val updater = object : Task.Backgroundable(project, RBundle.message("interpreter.state.schedule.skeleton.update"), false) {
        override fun run(indicator: ProgressIndicator) {
          RLibraryWatcher.getInstance(project).updateRootsToWatch(this@RInterpreterStateImpl)
          updateSkeletons()
        }
      }
      ProgressManager.getInstance().run(updater)
    }
  }

    private fun updateSkeletons() {
      DumbService.getInstance(project).queueTask(MyDumbModeTask(rInterop))
    }

    private inner class MyDumbModeTask(val rInterop: RInterop) : DumbModeTask() {
      override fun performInDumbMode(indicator: ProgressIndicator) {
        if (!project.isOpen || project.isDisposed) return
        if (RSkeletonUtil.updateSkeletons(rInterop, indicator)) {
          invokeAndWaitIfNeeded { runWriteAction { refreshSkeletons() } }
          updateState().onSuccess {
            RInterpreterUtil.updateIndexableSet(project)
          }
        }
        isSkeletonInitialized = true
        RInterpreterUtil.updateIndexableSet(project)
      }

      override fun tryMergeWith(taskFromQueue: DumbModeTask): DumbModeTask? =
        if (taskFromQueue is MyDumbModeTask && taskFromQueue.rInterop == rInterop) this else null
    }

    private fun refreshSkeletons() {
      if (!project.isOpen || project.isDisposed) return
      val skeletonsDirectory = LocalFileSystem.getInstance().refreshAndFindFileByPath(skeletonsDirectory) ?: return
      VfsUtil.markDirtyAndRefresh(false, true, true, skeletonsDirectory)
      WriteAction.runAndWait<Exception> { PsiDocumentManagerImpl.getInstance(project).commitAllDocuments() }
    }

    companion object {
      val LOG = Logger.getInstance(RInterpreterState::class.java)
    }
  }
