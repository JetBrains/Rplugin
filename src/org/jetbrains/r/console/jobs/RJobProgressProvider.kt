/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

class RJobProgressProvider {
  @Volatile
  var total: Int = 1
    private set
  @Volatile
  var current: Int = 0
    private set
  @Volatile
  var progressUpdated: () -> Unit = {}

  fun onProgressAvailable(message: String) {
    val arguments = message.split(' ')
    if (arguments.size == 2) {
      if (arguments[0] == "count") {
        arguments[1].toIntOrNull()?.let {
         total = it
         progressUpdated()
        }
      }
      if (arguments[0] == "statement") {
        arguments[1].toIntOrNull()?.let {
          current = it
          progressUpdated()
        }
      }
    }
  }

}
