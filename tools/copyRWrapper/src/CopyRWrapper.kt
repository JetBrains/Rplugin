import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import java.io.File

import java.lang.ProcessBuilder.Redirect
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

private fun String.runCommand(workingDir: File) {
  val process = ProcessBuilder(*split(" ").toTypedArray())
    .directory(workingDir)
    .redirectOutput(Redirect.INHERIT)
    .redirectError(Redirect.INHERIT)
    .start()
  if (!process.waitFor(2000, TimeUnit.SECONDS)) {
    process.destroy()
    throw RuntimeException("execution timed out: $this")
  }
  if (process.exitValue() != 0) {
    throw RuntimeException("execution failed with code ${process.exitValue()}: $this")
  }
}

fun main(args: Array<String>) {
  val rwrapperDirectory = File(args[0])
  val destinationDirectory = File(args[1])
  val customRWrapperPath = if (args.size > 2) File(args[2]) else null

  if (SystemUtils.IS_OS_UNIX) {
    "./build_rwrapper.sh".runCommand(rwrapperDirectory)
  }

  destinationDirectory.mkdirs()

  rwrapperDirectory
    .takeIf { it.exists() && it.isDirectory }
    ?.list { _, name -> name.startsWith("rwrapper") || name.startsWith("R-") ||
                        name == "R" || name.startsWith("fsnotifier-") || name == "rkernelVersion.txt" }
    ?.map { Paths.get(rwrapperDirectory.toString(), it).toFile() }
    ?.forEach {
    if (it.isDirectory) {
      FileUtils.copyDirectoryToDirectory(it, destinationDirectory)
    } else {
      FileUtils.copyFileToDirectory(it, destinationDirectory)
    }
  } ?: check(false) { "cannot find directory: ${rwrapperDirectory}" }

  customRWrapperPath
    ?.takeIf { it.exists() && it.isDirectory }
    ?.list { _, name -> name.startsWith("rwrapper") || name.startsWith("fsnotifier-") || name == "rkernelVersion.txt" }
    ?.map { Paths.get(customRWrapperPath.toString(), it).toFile() }
    ?.forEach {
      FileUtils.copyFileToDirectory(it, destinationDirectory)
    } ?: check(customRWrapperPath == null ) { "cannot find directory: ${customRWrapperPath}" }
}