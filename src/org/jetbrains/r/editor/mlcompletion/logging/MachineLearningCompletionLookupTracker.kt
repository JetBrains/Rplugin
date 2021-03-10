package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.codeInsight.lookup.impl.LookupImpl

class MachineLearningCompletionLookupTracker : LookupManagerListener {
  override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
    (newLookup as? LookupImpl)?.let(MachineLearningCompletionLookupStatistics::initStatistics)
  }
}
