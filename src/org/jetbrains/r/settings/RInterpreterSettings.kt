/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.components.*
import org.jetbrains.r.interpreter.RBasicInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RVersion
import org.jetbrains.r.interpreter.findByPath

// NOTE: if you're interested in what kind of tricky games are played on list of strings below
// than you should know that PersistentStateComponent is able to save neither data class
// (which is obviously RBasicInterpreterInfo) nor two-level list of strings:
//    [["name", "path", "version"], ["name", ...], ...]
// Instead a flattened string representation of RInterpreterInfo's is used:
//    ["name", "path", "version", "name", ...]
@State(name = "RInterpreterSettings", storages = [Storage("rInterpreterSettings.xml")])
class RInterpreterSettings : SimplePersistentStateComponent<RInterpreterSettingsState>(RInterpreterSettingsState()) {
  companion object {
    private fun RInterpreterInfo.toTriple(): List<String> {
      return listOf(this.interpreterName, this.interpreterPath, this.version.toString())
    }

    private fun List<String>.toInterpreter(): RInterpreterInfo {
      return RBasicInterpreterInfo(this[0], this[1], RVersion.forceParse(this[2]))
    }

    fun getInstance() = service<RInterpreterSettings>()

    private fun getTriples(): MutableList<String> {
      return getInstance().state.triples
    }

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
        return known.filter { it.exists() }.also {
          // Automatically remove non-existing interpreters
          if (it.size < known.size) {
            knownInterpreters = it
          }
        }
      }

    private var knownInterpreters: List<RInterpreterInfo>
      get() {
        return getTriples().chunked(3) { it.toInterpreter() }
      }
      set(interpreters) {
        val triples = getTriples()
        triples.clear()
        triples.addAll(interpreters.flatMap { it.toTriple() })
      }

    fun addOrEnableInterpreter(interpreter: RInterpreterInfo) {
      val known = knownInterpreters
      val path = interpreter.interpreterPath
      if (known.findByPath(path) == null) {
        knownInterpreters = known.plus(interpreter)
      }
      getInstance().state.disabledPaths.remove(path)
    }

    fun setEnabledInterpreters(interpreters: List<RInterpreterInfo>) {
      val oldKnown = knownInterpreters
      val newKnown = mutableListOf<RInterpreterInfo>().apply {
        addAll(interpreters)
        for (interpreter in oldKnown) {
          if (findByPath(interpreter.interpreterPath) == null) {
            add(interpreter)
          }
        }
      }
      val newDisabledPaths = mutableSetOf<String>().apply {
        for (interpreter in oldKnown) {
          add(interpreter.interpreterPath)
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
  var triples: MutableList<String> by list<String>()
  var disabledPaths: MutableList<String> by list<String>()
}
