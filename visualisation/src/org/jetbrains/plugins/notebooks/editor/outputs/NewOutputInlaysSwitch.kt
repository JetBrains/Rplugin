package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.util.registry.Registry

object NewOutputInlaysSwitch {
  private val types: Set<String> = Registry.get("ds.editor.new.output.inlays.types").asString()
    .toLowerCase()
    .split(' ', ',', '\n')
    .filterTo(hashSetOf()) { it.isNotEmpty() }

  val useNewForAnything: Boolean = types.isNotEmpty()
  val useNewForEverything: Boolean = "all" in types
  val useNewForWeb: Boolean = useNewForEverything || "web" in types
  val useNewForText: Boolean = useNewForEverything || "text" in types
  val useNewForTable: Boolean = useNewForEverything || "jupyter_table" in types
  val useNewForImage = useNewForEverything || "image" in types
}
