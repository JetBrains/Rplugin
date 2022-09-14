/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.common

class ExpiringList<out T>(values: List<T>, private val expiryCondition: () -> Boolean) : List<T> by values {
  private val hasExpired: Boolean
    get() = expiryCondition()

  val hasNotExpired: Boolean
    get() = !hasExpired

  fun <R>map(mapping: (T) -> R): ExpiringList<R> {
    val mapped = (this as List<T>).map(mapping)  // Note: casting prevents recursive invocation of `map`
    return ExpiringList(mapped) { hasExpired }
  }

  fun filter(predicate: (T) -> Boolean): ExpiringList<T> {
    val filtered = (this as List<T>).filter(predicate)  // Note: casting prevents recursive invocation of `filter`
    return ExpiringList(filtered) { hasExpired }
  }
}

fun <E>emptyExpiringList(hasExpired: Boolean = true): ExpiringList<E> {
  return ExpiringList(emptyList()) { hasExpired }
}
