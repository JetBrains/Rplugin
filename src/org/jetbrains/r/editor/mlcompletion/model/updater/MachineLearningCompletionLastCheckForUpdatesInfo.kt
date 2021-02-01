package org.jetbrains.r.editor.mlcompletion.model.updater

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

typealias DateChangeListener = (KProperty<*>, Long, Long) -> Unit

object MachineLearningCompletionLastCheckForUpdatesInfo {

  private val listeners = Collections.newSetFromMap(ConcurrentHashMap<DateChangeListener, Boolean>())

  var lastUpdateCheckTimeMs: Long by Delegates.observable(-1) { property, oldValue, newValue ->
    listeners.forEach { it(property, oldValue, newValue) }
  }
    private set

  fun reportUpdateCheck() {
    lastUpdateCheckTimeMs = System.currentTimeMillis()
  }

  fun subscribe(dateChangeListener: DateChangeListener) {
    listeners.add(dateChangeListener)
  }

  fun unsubscribe(dateChangeListener: DateChangeListener) {
    listeners.add(dateChangeListener)
  }
}
