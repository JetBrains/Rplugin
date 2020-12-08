package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.util.registry.Registry

object NewOutputInlaysSwitch {
  private val types: Set<String> = Registry.get("ds.editor.new.output.inlays.types").asString()
    .toLowerCase()
    .split(' ', ',', '\n')
    .filterTo(hashSetOf()) { it.isNotEmpty() }

  val useNewForAnything: Boolean = types.isNotEmpty()
  val useNewForText: Boolean = "text" in types
  val useNewForTable: Boolean = "jupyter_table" in types
  val useNewForImage = "image" in types
}