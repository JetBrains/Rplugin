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
        it.interpreterLocation = interpreterLocation
        it.version = version.toString()
        it.timestamp = timestamp ?: interpreterLocation.lastModified() ?: -1
      }
    }

    private fun RSerializableInterpreter.toInterpreter(): RInterpreterInfo? {
      return RBasicInterpreterInfo(name, interpreterLocation ?: return null, RVersion.forceParse(version))
    }

    private fun List<RSerializableInterpreter>.findByLocation(location: RInterpreterLocation): RSerializableInterpreter? {
      return find { it.interpreterLocation == location }
    }

    fun getInstance() = service<RInterpreterSettings>()

    var disabledLocations: Set<String>
      get() {
        return getInstance().state.disabledLocations.toSet()
      }
      private set(newPaths) {
        getInstance().state.disabledLocations.apply {
          clear()
          addAll(newPaths)
        }
      }

    val existingInterpreters: List<RInterpreterInfo>
      get() {
        var isModified = false
        return knownInterpreters
          .mapNotNull { interpreterInfo ->
            val interpreter = interpreterInfo.toInterpreter() ?: return@mapNotNull null
            val localPath = interpreter.interpreterLocation.toLocalPathOrNull() ?: return@mapNotNull interpreterInfo
            val file = File(localPath)
            if (file.exists()) {
              val timestamp = file.lastModified()
              if (timestamp != interpreterInfo.timestamp) {
                // Inflate new instance of RBasicInterpreter in order to get actual version
                isModified = true
                RBasicInterpreterInfo.from(interpreter.interpreterName, interpreter.interpreterLocation)?.toSerializable()
              } else {
                interpreterInfo
              }
            } else {
              isModified = true
              null
            }
          }.let { existing ->
            if (isModified) {
              knownInterpreters = existing
            }
            existing.mapNotNull { it.toInterpreter() }
          }
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
      val previous = known.findByLocation(interpreter.interpreterLocation)
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
      getInstance().state.disabledLocations.remove(interpreter.interpreterLocation.toString())
    }

    fun setEnabledInterpreters(interpreters: List<RInterpreterInfo>) {
      val oldKnown = knownInterpreters
      val newKnown = mutableListOf<RSerializableInterpreter>().apply {
        addAll(interpreters.map { it.toSerializable() })
        for (interpreter in oldKnown) {
          val location = interpreter.interpreterLocation
          if (location != null && findByLocation(location) == null) {
            add(interpreter)
          }
        }
      }
      val newDisabledLocations = mutableSetOf<String>().apply {
        for (interpreter in oldKnown) {
          interpreter.interpreterLocation?.toString()?.let { add(it) }
        }
        for (interpreter in interpreters) {
          interpreter.interpreterLocation.toString().let { remove(it) }
        }
      }
      knownInterpreters = newKnown
      disabledLocations = newDisabledLocations
    }
  }
}

class RInterpreterSettingsState : BaseState() {
  var interpreters: MutableList<RSerializableInterpreter> by list<RSerializableInterpreter>()
  var disabledLocations: MutableList<String> by list<String>()
}

class RSerializableInterpreter {
  var name: String = ""
  var path: String = ""
  var remoteHost: String = ""
  var remoteBasePath: String = ""
  var version: String = ""
  var timestamp: Long = 0L

}

var RSerializableInterpreter.interpreterLocation: RInterpreterLocation?
  get() {
    return RInterpreterSettingsProvider.getProviders().asSequence().mapNotNull { it.deserializeLocation(this) }.firstOrNull()
  }
  set(value) {
    if (value != null) {
      for (provider in RInterpreterSettingsProvider.getProviders()) {
        if (provider.serializeLocation(value, this)) return
      }
    }
    path = ""
    remoteHost = ""
  }
