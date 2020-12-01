package org.jetbrains.r.editor.mlcompletion

import com.intellij.ide.AppLifecycleListener

class MachineLearningCompletionServerInitializerListener : AppLifecycleListener {
  override fun appStarted() {
    MachineLearningCompletionServerService.getInstance()
  }
}
