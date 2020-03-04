/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.util

import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

object PromiseUtil {
  fun runChain(tasks: List<() -> Promise<Boolean>>): Promise<Boolean> {
    var promise: Promise<Boolean> = resolvedPromise(true)
    tasks.forEach { task ->
      promise = promise.thenAsync {
        if (it) {
          task()
        } else {
          resolvedPromise(false)
        }
      }
    }
    return promise
  }

}