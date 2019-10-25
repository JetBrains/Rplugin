/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.components.*
import org.jetbrains.r.interpreter.*
import java.io.File

@State(name = "RInterpreterSettings", storages = [Storage("rInterpreterSettings.xml")])
class RInterpreterSettings : SimplePersistentStateComponent<RInterpreterSettingsState>(RInterpreterSettingsState()) {
  companion object {
    private fun RInterpreterInfo.toSerializable(timestamp: Long? = null): RSerializableInterpreter {
      return RSerializableInterpreter().also {
        it.name = interpreterName
        it.path = interpreterPath
        it.version = version.toString()
        it.timestamp = timestamp ?: File(interpreterPath).lastModified()
      }
    }

    private fun RSerializableInterpreter.toInterpreter(): RInterpreterInfo {
      return RBasicInterpreterInfo(name, path, RVersion.forceParse(version))
    }

    private fun List<RSerializableInterpreter>.findByPath(path: String): RSerializableInterpreter? {
      return find { it.path == path }
    }

    fun getInstance() = service<RInterpreterSettings>()

    var disabledPaths: Set<String>
      get() {
        return getInstance().state.disabledPaths.toSet()
      }
      private set(newPaths) {
        getInstance().state.disabledPaths.apply {
          clear()
          addAll(newPaths)
        }
      }

    val existingInterpreters: List<RInterpreterInfo>
      get() {
        val known = knownInterpreters
        val existing = mutableListOf<RSerializableInterpreter>()
        var isModified = false
        for (interpreter in known) {
          val file = File(interpreter.path)
          if (file.exists()) {
            val timestamp = file.lastModified()
            if (timestamp != interpreter.timestamp) {
              // Inflate new instance of RBasicInterpreter in order to get actual version
              isModified = true
              RBasicInterpreterInfo.from(interpreter.name, interpreter.path)?.let { inflated ->
                existing.add(inflated.toSerializable(timestamp))
              }
            } else {
              existing.add(interpreter)
            }
          } else {
            isModified = true
          }
        }
        if (isModified) {
          knownInterpreters = existing
        }
        return existing.map { it.toInterpreter() }
      }

    private var knownInterpreters: List<RSerializableInterpreter>
      get() {
        return getInstance().state.interpreters
      }
      set(interpreters) {
        getInstance().state.interpreters.apply {
          clear()
          addAll(interpreters)
        }
      }

    fun addOrEnableInterpreter(interpreter: RInterpreterInfo) {
      val known = knownInterpreters
      val current = interpreter.toSerializable()
      val path = current.path
      val previous = known.findByPath(path)
      if (previous != null) {
        if (previous.timestamp != current.timestamp) {
          previous.apply {
            name = current.name
            version = current.version
            timestamp = current.timestamp
          }
        }
      } else {
        knownInterpreters = known.plus(current)
      }
      getInstance().state.disabledPaths.remove(path)
    }

    fun setEnabledInterpreters(interpreters: List<RInterpreterInfo>) {
      val oldKnown = knownInterpreters
      val newKnown = mutableListOf<RSerializableInterpreter>().apply {
        addAll(interpreters.map { it.toSerializable() })
        for (interpreter in oldKnown) {
          if (findByPath(interpreter.path) == null) {
            add(interpreter)
          }
        }
      }
      val newDisabledPaths = mutableSetOf<String>().apply {
        for (interpreter in oldKnown) {
          add(interpreter.path)
        }
        for (interpreter in interpreters) {
          remove(interpreter.interpreterPath)
        }
      }
      knownInterpreters = newKnown
      disabledPaths = newDisabledPaths
    }
  }
}

class RInterpreterSettingsState : BaseState() {
  var interpreters: MutableList<RSerializableInterpreter> by list<RSerializableInterpreter>()
  var disabledPaths: MutableList<String> by list<String>()
}

class RSerializableInterpreter {
  var name: String = ""
  var path: String = ""
  var version: String = ""
  var timestamp: Long = 0L
}
