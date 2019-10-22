/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.components.*
import org.jetbrains.r.interpreter.RBasicInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RVersion

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

    /**
     * List of all existing known interpreters.
     * **Note:** contrary to [knownInterpreters], this list is additionally filtered
     * in order to contain existing interpreters only.
     */
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

    /**
     * List of all known interpreters.
     * **Note:** unlike [existingInterpreters] this list doesn't check
     * whether interpreters it contains exist thus it is faster.
     */
    var knownInterpreters: List<RInterpreterInfo>
      get() {
        return getTriples().chunked(3) { it.toInterpreter() }
      }
      set(interpreters) {
        val triples = getTriples()
        triples.clear()
        triples.addAll(interpreters.flatMap { it.toTriple() })
      }

    fun addInterpreter(interpreter: RInterpreterInfo) {
      getTriples().addAll(interpreter.toTriple())
    }

    fun removeInterpreter(interpreter: RInterpreterInfo) {
      val triples = getTriples()
      val chunks = triples.chunked(3)
      val shrunken = chunks.filter { it[1] != interpreter.interpreterPath }
      triples.clear()
      triples.addAll(shrunken.flatten())
    }
  }
}

class RInterpreterSettingsState : BaseState() {
  var triples: MutableList<String> by list<String>()
}
