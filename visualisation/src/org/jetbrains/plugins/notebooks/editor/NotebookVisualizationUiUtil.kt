package org.jetbrains.plugins.notebooks.editor

import java.awt.Graphics

inline fun <T> Graphics.use(handler: (g: Graphics) -> T): T =
  try {
    handler(this)
  }
  finally {
    dispose()
  }
