package org.jetbrains.plugins.notebooks.editor

import java.awt.Graphics

inline fun <T, G : Graphics> G.use(handler: (g: G) -> T): T =
  try {
    handler(this)
  }
  finally {
    dispose()
  }
