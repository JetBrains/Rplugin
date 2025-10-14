package org.jetbrains.r.run

import com.intellij.openapi.ui.DialogWrapper
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import com.intellij.r.psi.interpreter.LocalOrRemotePath
import org.jetbrains.r.run.visualize.RImportBaseDataDialog
import java.nio.file.Paths
import kotlin.io.path.exists

class RImportDataDialogTest : RProcessHandlerBaseTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testDatasetName() {
    checkDatasetName(CSV_FILE_NAME, "sins")
  }

  fun testDatasetSpaceName() {
    checkDatasetName(SPACE_NAME, "example_space")
  }

  fun testDatasetRestrictedCharsName() {
    checkDatasetName(RESTRICTED_CHARS_NAME, "dataset")
  }

  fun testDatasetIncludeRestrictedCharsName() {
    checkDatasetName(INCLUDE_RESTRICTED_CHARS_NAME, "example")
  }

  fun testDatasetUnderscoreFirstName() {
    checkDatasetName(UNDERSCORE_FIRST_NAME, "dataset_space")
  }

  fun testDatasetUnderscoreName() {
    checkDatasetName(UNDERSCORE_NAME, "dataset")
  }

  fun testDatasetNumFirstName() {
    checkDatasetName(NUM_FIRST_NAME, "dataset_1exm")
  }

  fun testDatasetDotNumFirstName() {
    checkDatasetName(DOT_NUM_FIRST_NAME, "dataset_1exm")
  }

  fun testDatasetEmptyName() {
    checkDatasetName(EMPTY_NAME, "dataset")
  }

  private fun checkDatasetName(fileName: String, expected: String) {
    val path = Paths.get(testDataPath, "datasets", fileName)
    TestCase.assertTrue("Test file missing: ${path}", path.exists())
    val lorPath = LocalOrRemotePath(path.toString(), false)
    val promise = AsyncPromise<Boolean>()
    RImportBaseDataDialog(myFixture.project, rInterop, lorPath).runAndClose(promise) {
      dialog ->
      dialog.onUpdateAdditional = {
        promise.setResult(true)
      }
      TestCase.assertEquals(dialog.variableName(), expected)
    }
  }

  companion object {
    private const val TIMEOUT = 5000
    private const val CSV_FILE_NAME = "sins.csv"
    private const val SPACE_NAME = "example space.csv"
    private const val RESTRICTED_CHARS_NAME = "--.csv"
    private const val INCLUDE_RESTRICTED_CHARS_NAME = "example--.csv"
    private const val UNDERSCORE_FIRST_NAME = "_space.csv"
    private const val UNDERSCORE_NAME = "_.csv"
    private const val NUM_FIRST_NAME = "1exm.csv"
    private const val DOT_NUM_FIRST_NAME = ".1exm.csv"
    private const val EMPTY_NAME = ".csv"

    fun <T: DialogWrapper> T.runAndClose(promise: AsyncPromise<Boolean>, block: (T) -> Unit) {
      try {
        block(this)
      }
      finally {
        promise.blockingGet(TIMEOUT)
        close(0)
      }
    }
  }
}