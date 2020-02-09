/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.google.protobuf.GeneratedMessageV3
import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.r.packages.RHelpersUtil
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RInteropJsonTest : RProcessHandlerBaseTestCase() {

  private val pathReplacers: ArrayList<PathReplacer> = ArrayList()
  private lateinit var grpcTester: GRPCTester


  override fun setUp() {
    super.setUp()
    val initPathReplacer = InitPathReplacer(myFixture.project, RHelpersUtil.findFileInRHelpers("R").absolutePath)
    val graphicsPathReplacer = GraphicsPathReplacer(myFixture.project, myFixture.tempDirPath)
    val htmlPathReplacer = HtmlPathReplacer(myFixture.project, FileUtil.createTempFile("", "urls.txt", true).absolutePath)
    pathReplacers.add(initPathReplacer)
    pathReplacers.add(graphicsPathReplacer)
    pathReplacers.add(htmlPathReplacer)
    grpcTester = GRPCTester(testDataPath + "/grpc/" + getTestName(true) + ".json.xz", rInterop, pathReplacers)
  }

  fun testStartSession() {
    runMessages(listOf("loaderGetVariables", "executeCode"))
  }

  fun testDebugSession() {
    runMessages(listOf("loaderGetVariables", "getSourceFileText", "getSourceFileName", "loaderGetValueInfo"))
  }

  fun testRmarkdown() {
    runMessages(listOf("loaderGetVariables", "executeCode"))
  }

  private fun runMessages(checkMethods: List<String>) {
    grpcTester.messages.forEach {
      println("Proceeding: " + it.methodName)
      val (output, expected) = grpcTester.proceedMessage(it) ?: return
      println("output is " + output)
      if (it is RInteropGrpcLogger.StubMessage && checkMethods.contains(it.methodName) && expected != null) {
        TestCase.assertTrue(output is GeneratedMessageV3)
      }
    }
  }
}